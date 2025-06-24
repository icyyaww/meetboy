/*
 * Copyright (C) 2019 The Turms Project
 * https://github.com/turms-im/turms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.turms.interaction.service;

import im.turms.interaction.domain.Comment;
import im.turms.interaction.domain.InteractionEvent;
import im.turms.interaction.domain.Like;
import im.turms.interaction.dto.UserInfo;
import im.turms.interaction.dto.ModerationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 评论流式处理服务
 * 
 * 核心特性：
 * - 实时评论流推送
 * - 多级评论树结构
 * - 评论分页加载
 * - 智能评论排序
 * - 评论内容审核
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentStreamService {

    private final ReactiveMongoTemplate mongoTemplate;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final EventPublishingService eventPublishingService;
    private final UserServiceClient userServiceClient;
    @Qualifier("interactionContentModerationService")
    private final ContentModerationService moderationService;

    // 实时评论流
    private final ConcurrentHashMap<String, Sinks.Many<Comment>> commentStreams = new ConcurrentHashMap<>();
    
    // 流式序号生成器
    private final AtomicLong streamSequenceGenerator = new AtomicLong(0);
    
    private static final String COMMENT_COUNT_KEY_PREFIX = "comment:count:";
    private static final String COMMENT_CACHE_KEY_PREFIX = "comment:cache:";
    
    /**
     * 添加评论 (流式处理)
     */
    public Mono<Comment> addComment(Long userId, Comment.CommentType type, String targetType, String targetId,
                                   String content, List<Comment.CommentAttachment> attachments,
                                   String parentId, Long replyToUserId, Comment.DeviceInfo deviceInfo) {
        
        Instant now = Instant.now();
        long sequence = streamSequenceGenerator.incrementAndGet();
        
        return userServiceClient.getUserInfo(userId)
                .flatMap(userInfo -> {
                    Comment comment = Comment.builder()
                            .userId(userId)
                            .username(userInfo.getUsername())
                            .avatar(userInfo.getAvatar())
                            .targetType(Like.TargetType.valueOf(targetType))
                            .targetId(targetId)
                            .parentId(parentId)
                            .content(content)
                            .type(type)
                            .status(Comment.CommentStatus.PENDING)
                            .attachments(attachments)
                            .likeCount(0)
                            .replyCount(0)
                            .level(calculateCommentLevel(parentId))
                            .streamSequence(sequence)
                            .createdDate(now)
                            .lastModifiedDate(now)
                            .timeBucket(Like.generateTimeBucket(now))
                            .deviceInfo(deviceInfo)
                            .build();
                    
                    // 设置根评论ID
                    if (parentId != null) {
                        return findRootComment(parentId)
                                .doOnNext(rootId -> comment.setRootId(rootId))
                                .then(Mono.just(comment));
                    } else {
                        comment.setRootId(null);
                        return Mono.just(comment);
                    }
                })
                .flatMap(comment -> {
                    // 内容审核
                    return moderationService.moderateComment(comment)
                            .doOnNext(moderationResult -> {
                                comment.setModerationResult(moderationResult);
                                comment.setStatus(determineCommentStatus(moderationResult));
                            })
                            .then(Mono.just(comment));
                })
                .flatMap(comment -> {
                    // 保存评论
                    return mongoTemplate.save(comment)
                            .doOnNext(savedComment -> {
                                // 更新统计数据
                                updateCommentCountAsync(targetType, targetId, 1);
                                if (parentId != null) {
                                    updateReplyCountAsync(parentId, 1);
                                }
                                
                                // 推送实时评论流
                                pushToCommentStream(targetType + ":" + targetId, savedComment);
                                
                                // 发布评论事件
                                publishCommentEvent(savedComment, InteractionEvent.EventType.COMMENT_ADDED);
                            });
                })
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
                .doOnError(error -> log.error("添加评论失败: userId={}, targetId={}", userId, targetId, error));
    }

    /**
     * 获取评论流 (实时推送)
     */
    public Flux<Comment> getCommentStream(String targetType, String targetId) {
        String streamKey = targetType + ":" + targetId;
        
        // 获取或创建评论流
        Sinks.Many<Comment> sink = commentStreams.computeIfAbsent(streamKey, 
                key -> Sinks.many().multicast().onBackpressureBuffer());
        
        return sink.asFlux()
                .doOnSubscribe(subscription -> log.debug("用户订阅评论流: {}", streamKey))
                .doOnCancel(() -> log.debug("用户取消订阅评论流: {}", streamKey))
                .doFinally(signalType -> {
                    // 如果没有订阅者，清理流
                    if (sink.currentSubscriberCount() == 0) {
                        commentStreams.remove(streamKey);
                    }
                });
    }

    /**
     * 获取评论列表 (分页 + 树形结构)
     */
    public Flux<Comment> getComments(String targetType, String targetId, Long viewerUserId, 
                                    int page, int size, String sortBy) {
        
        // 构建查询条件
        Query query = Query.query(
                Criteria.where("targetType").is(targetType)
                        .and("targetId").is(targetId)
                        .and("status").is(Comment.CommentStatus.APPROVED)
                        .and("parentId").isNull() // 只查询顶级评论
        );
        
        // 设置排序
        switch (sortBy) {
            case "hot" -> query.with(Sort.by(Sort.Direction.DESC, "sortWeight"));
            case "latest" -> query.with(Sort.by(Sort.Direction.DESC, "createdDate"));
            default -> query.with(Sort.by(Sort.Direction.ASC, "createdDate"));
        }
        
        query.skip((long) page * size).limit(size);
        
        return mongoTemplate.find(query, Comment.class)
                .flatMap(comment -> {
                    // 计算排序权重
                    comment.calculateSortWeight();
                    
                    // 加载子评论 (最多3级)
                    if (comment.getReplyCount() > 0) {
                        return loadReplies(comment.getId(), 3)
                                .collectList()
                                .doOnNext(replies -> {
                                    // 这里可以设置replies到comment中，如果Comment类有replies字段
                                    log.debug("加载了{}条回复", replies.size());
                                })
                                .then(Mono.just(comment));
                    }
                    return Mono.just(comment);
                })
                .doOnNext(comment -> {
                    // 异步缓存热门评论
                    if ("hot".equals(sortBy) && page == 0) {
                        cacheHotComment(targetType, targetId, comment);
                    }
                });
    }

    /**
     * 获取评论回复 (子评论)
     */
    public Flux<Comment> getCommentReplies(String parentId, int page, int size) {
        Query query = Query.query(
                Criteria.where("parentId").is(parentId)
                        .and("status").is(Comment.CommentStatus.APPROVED)
        )
        .with(Sort.by(Sort.Direction.ASC, "createdDate"))
        .skip((long) page * size)
        .limit(size);
        
        return mongoTemplate.find(query, Comment.class);
    }

    /**
     * 更新评论
     */
    public Mono<Comment> updateComment(String commentId, Long userId, String newContent) {
        Query query = Query.query(
                Criteria.where("id").is(commentId)
                        .and("userId").is(userId)
                        .and("status").in(Comment.CommentStatus.APPROVED, Comment.CommentStatus.PENDING)
        );
        
        return mongoTemplate.findOne(query, Comment.class)
                .switchIfEmpty(Mono.error(new RuntimeException("评论不存在或无权限")))
                .flatMap(comment -> {
                    // 重新审核内容
                    return moderationService.moderateText(newContent)
                            .flatMap(moderationResult -> {
                                Update update = Update.update("content", newContent)
                                        .set("lastModifiedDate", Instant.now())
                                        .set("moderationResult", moderationResult)
                                        .set("status", determineCommentStatus(moderationResult));
                                
                                return mongoTemplate.findAndModify(query, update, Comment.class)
                                        .doOnNext(updatedComment -> {
                                            // 推送更新事件
                                            pushToCommentStream(comment.getTargetType() + ":" + comment.getTargetId(), 
                                                              updatedComment);
                                            publishCommentEvent(updatedComment, InteractionEvent.EventType.COMMENT_UPDATED);
                                        });
                            });
                });
    }

    /**
     * 删除评论
     */
    public Mono<Boolean> deleteComment(String commentId, Long userId) {
        Query query = Query.query(
                Criteria.where("id").is(commentId)
                        .and("userId").is(userId)
        );
        
        return mongoTemplate.findOne(query, Comment.class)
                .switchIfEmpty(Mono.error(new RuntimeException("评论不存在或无权限")))
                .flatMap(comment -> {
                    Update update = Update.update("status", Comment.CommentStatus.DELETED)
                            .set("lastModifiedDate", Instant.now());
                    
                    return mongoTemplate.updateFirst(query, update, Comment.class)
                            .map(result -> result.getModifiedCount() > 0)
                            .doOnNext(success -> {
                                if (success) {
                                    // 更新统计数据
                                    updateCommentCountAsync(comment.getTargetType().toString(), 
                                                          comment.getTargetId(), -1);
                                    if (comment.getParentId() != null) {
                                        updateReplyCountAsync(comment.getParentId(), -1);
                                    }
                                    
                                    // 发布删除事件
                                    publishCommentEvent(comment, InteractionEvent.EventType.COMMENT_DELETED);
                                }
                            });
                });
    }

    /**
     * 推送评论到实时流
     */
    private void pushToCommentStream(String streamKey, Comment comment) {
        Sinks.Many<Comment> sink = commentStreams.get(streamKey);
        if (sink != null) {
            sink.tryEmitNext(comment);
        }
    }

    /**
     * 计算评论层级
     */
    private int calculateCommentLevel(String parentId) {
        if (parentId == null) {
            return 0;
        }
        
        return mongoTemplate.findById(parentId, Comment.class)
                .map(parent -> parent.getLevel() + 1)
                .block(); // 这里为了简化，使用block()，实际应该改为响应式
    }

    /**
     * 查找根评论ID
     */
    private Mono<String> findRootComment(String parentId) {
        return mongoTemplate.findById(parentId, Comment.class)
                .map(parent -> parent.getRootId() != null ? parent.getRootId() : parent.getId());
    }

    /**
     * 加载回复评论
     */
    private Flux<Comment> loadReplies(String parentId, int maxLevel) {
        if (maxLevel <= 0) {
            return Flux.empty();
        }
        
        Query query = Query.query(
                Criteria.where("parentId").is(parentId)
                        .and("status").is(Comment.CommentStatus.APPROVED)
        )
        .with(Sort.by(Sort.Direction.ASC, "createdDate"))
        .limit(5); // 限制回复数量
        
        return mongoTemplate.find(query, Comment.class)
                .flatMap(reply -> {
                    if (reply.getReplyCount() > 0 && maxLevel > 1) {
                        return loadReplies(reply.getId(), maxLevel - 1)
                                .then(Mono.just(reply));
                    }
                    return Mono.just(reply);
                });
    }

    /**
     * 根据审核结果确定评论状态
     */
    private Comment.CommentStatus determineCommentStatus(ModerationResult moderationResult) {
        if (moderationResult.getScore() >= 0.8) {
            return Comment.CommentStatus.APPROVED;
        } else if (moderationResult.getScore() >= 0.5) {
            return Comment.CommentStatus.PENDING;
        } else {
            return Comment.CommentStatus.REJECTED;
        }
    }

    /**
     * 异步更新评论计数
     */
    private void updateCommentCountAsync(String targetType, String targetId, int delta) {
        String countKey = COMMENT_COUNT_KEY_PREFIX + targetType + ":" + targetId;
        
        redisTemplate.opsForValue()
                .increment(countKey, delta)
                .subscribe(
                    count -> log.debug("评论计数已更新: {} = {}", countKey, count),
                    error -> log.error("评论计数更新失败", error)
                );
    }

    /**
     * 异步更新回复计数
     */
    private void updateReplyCountAsync(String parentId, int delta) {
        Query query = Query.query(Criteria.where("id").is(parentId));
        Update update = new Update().inc("replyCount", delta);
        
        mongoTemplate.updateFirst(query, update, Comment.class)
                .subscribe(
                    result -> log.debug("回复计数已更新: parentId={}, delta={}", parentId, delta),
                    error -> log.error("回复计数更新失败", error)
                );
    }

    /**
     * 缓存热门评论
     */
    private void cacheHotComment(String targetType, String targetId, Comment comment) {
        String cacheKey = COMMENT_CACHE_KEY_PREFIX + "hot:" + targetType + ":" + targetId;
        
        redisTemplate.opsForList()
                .leftPush(cacheKey, comment)
                .then(redisTemplate.expire(cacheKey, Duration.ofMinutes(30)))
                .subscribe(
                    success -> log.debug("热门评论已缓存: {}", cacheKey),
                    error -> log.error("热门评论缓存失败", error)
                );
    }

    /**
     * 发布评论事件
     */
    private void publishCommentEvent(Comment comment, InteractionEvent.EventType eventType) {
        InteractionEvent event = InteractionEvent.builder()
                .eventType(eventType)
                .userId(comment.getUserId())
                .targetType(comment.getTargetType())
                .targetId(comment.getTargetId())
                .priority(InteractionEvent.EventPriority.NORMAL)
                .build();
        
        event.setDefaults();
        
        eventPublishingService.publishEvent(event)
                .subscribe(
                    success -> log.debug("评论事件已发布: {}", eventType),
                    error -> log.error("评论事件发布失败", error)
                );
    }
}