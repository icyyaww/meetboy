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

import im.turms.interaction.domain.mysql.CommentEntity;
import im.turms.interaction.domain.mysql.CommentCountEntity;
import im.turms.interaction.repository.CommentRepository;
import im.turms.interaction.repository.CommentCountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 评论服务V2 - MySQL主导 + Redis缓存架构
 * 
 * 设计理念：
 * - MySQL：负责数据持久化，简单的一级评论结构
 * - Redis：负责热门文章评论列表缓存，提升查询性能
 * - 简单索引：(article_id, created_at) 满足绝大部分查询需求
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceV2 {

    private final CommentRepository commentRepository;
    private final CommentCountRepository commentCountRepository;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    // Redis键前缀
    private static final String COMMENT_LIST_KEY_PREFIX = "comment:list:";          // article:123:comments
    private static final String COMMENT_COUNT_KEY_PREFIX = "comment:count:";       // comment:count:article:123
    private static final String HOT_COMMENTS_KEY_PREFIX = "comment:hot:";          // comment:hot:article:123
    
    // 缓存过期时间
    private static final Duration CACHE_TTL = Duration.ofHours(2);                 // 2小时
    private static final Duration HOT_CACHE_TTL = Duration.ofMinutes(30);          // 热门评论30分钟

    /**
     * 添加评论（MySQL主操作 + Redis缓存更新）
     */
    @Transactional
    public Mono<CommentResult> addComment(String articleId, Long userId, String username, 
                                        String avatar, String content, String deviceType, 
                                        String ipAddress, String locationInfo) {
        
        return Mono.fromCallable(() -> {
            // MySQL主操作
            CommentEntity comment = CommentEntity.builder()
                    .articleId(articleId)
                    .userId(userId)
                    .username(username)
                    .avatar(avatar)
                    .content(content)
                    .status(CommentEntity.CommentStatus.APPROVED)
                    .likeCount(0)
                    .deviceType(deviceType)
                    .ipAddress(ipAddress)
                    .locationInfo(locationInfo)
                    .build();
            
            CommentEntity savedComment = commentRepository.save(comment);
            
            // 更新计数表（通过触发器自动处理，这里可选手动同步）
            updateCommentCountAsync(articleId, 1);
            
            return savedComment;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(savedComment -> {
            // 异步更新Redis缓存
            refreshCommentCacheAsync(articleId);
            
            return Mono.just(CommentResult.builder()
                    .commentId(savedComment.getId())
                    .articleId(savedComment.getArticleId())
                    .userId(savedComment.getUserId())
                    .username(savedComment.getUsername())
                    .content(savedComment.getContent())
                    .likeCount(savedComment.getLikeCount())
                    .createdAt(savedComment.getCreatedAt())
                    .build());
        })
        .doOnSuccess(result -> log.debug("评论添加成功: articleId={}, commentId={}", 
                articleId, result.getCommentId()))
        .doOnError(error -> log.error("评论添加失败: articleId={}, userId={}", 
                articleId, userId, error));
    }

    /**
     * 获取文章评论列表（优先从Redis缓存读取）
     */
    public Flux<CommentResult> getCommentsByArticle(String articleId, int page, int size, 
                                                   String sortBy) {
        
        String cacheKey = COMMENT_LIST_KEY_PREFIX + articleId + ":" + sortBy + ":" + page + ":" + size;
        
        return redisTemplate.opsForList().range(cacheKey, 0, -1)
                .cast(CommentResult.class)
                .switchIfEmpty(
                    // Redis缓存未命中，从MySQL加载
                    loadCommentsFromDatabase(articleId, page, size, sortBy)
                            .collectList()
                            .flatMapMany(comments -> {
                                // 缓存到Redis
                                if (!comments.isEmpty()) {
                                    return redisTemplate.opsForList().rightPushAll(cacheKey, comments.toArray())
                                            .then(redisTemplate.expire(cacheKey, CACHE_TTL))
                                            .thenMany(Flux.fromIterable(comments));
                                }
                                return Flux.fromIterable(comments);
                            })
                )
                .doOnComplete(() -> log.debug("评论列表查询完成: articleId={}, page={}, size={}", 
                        articleId, page, size));
    }

    /**
     * 获取评论计数（优先从Redis读取）
     */
    public Mono<Integer> getCommentCount(String articleId) {
        String countKey = COMMENT_COUNT_KEY_PREFIX + articleId;
        
        return redisTemplate.opsForValue().get(countKey)
                .cast(Integer.class)
                .switchIfEmpty(
                    // Redis中没有数据，从MySQL加载
                    Mono.fromCallable(() -> {
                        return commentCountRepository.findByArticleId(articleId)
                                .map(CommentCountEntity::getApprovedCount)
                                .orElse(0);
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(count -> {
                        // 缓存到Redis
                        return redisTemplate.opsForValue()
                                .set(countKey, count, CACHE_TTL)
                                .thenReturn(count);
                    })
                )
                .defaultIfEmpty(0);
    }

    /**
     * 获取热门评论（Redis缓存优化）
     */
    public Flux<CommentResult> getHotComments(String articleId, int limit) {
        String hotKey = HOT_COMMENTS_KEY_PREFIX + articleId;
        
        return redisTemplate.opsForList().range(hotKey, 0, limit - 1)
                .cast(CommentResult.class)
                .switchIfEmpty(
                    // 从MySQL加载热门评论
                    Mono.fromCallable(() -> {
                        Pageable pageable = PageRequest.of(0, limit);
                        return commentRepository.findHotCommentsByArticle(
                                articleId, CommentEntity.CommentStatus.APPROVED, 5, pageable);
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMapMany(comments -> {
                        List<CommentResult> results = comments.stream()
                                .map(this::convertToResult)
                                .toList();
                        
                        // 缓存热门评论
                        if (!results.isEmpty()) {
                            return redisTemplate.opsForList().rightPushAll(hotKey, results.toArray())
                                    .then(redisTemplate.expire(hotKey, HOT_CACHE_TTL))
                                    .thenMany(Flux.fromIterable(results));
                        }
                        return Flux.fromIterable(results);
                    })
                );
    }

    /**
     * 更新评论（MySQL主操作 + 缓存清理）
     */
    @Transactional
    public Mono<Boolean> updateComment(Long commentId, Long userId, String newContent) {
        return Mono.fromCallable(() -> {
            return commentRepository.findById(commentId)
                    .filter(comment -> comment.getUserId().equals(userId))
                    .map(comment -> {
                        comment.setContent(newContent);
                        comment.setUpdatedAt(Instant.now());
                        CommentEntity updated = commentRepository.save(comment);
                        
                        // 清理相关缓存
                        clearCommentCacheAsync(updated.getArticleId());
                        
                        return true;
                    })
                    .orElse(false);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(success -> log.debug("评论更新完成: commentId={}, success={}", 
                commentId, success));
    }

    /**
     * 删除评论（软删除）
     */
    @Transactional
    public Mono<Boolean> deleteComment(Long commentId, Long userId) {
        return Mono.fromCallable(() -> {
            return commentRepository.findById(commentId)
                    .filter(comment -> comment.getUserId().equals(userId))
                    .map(comment -> {
                        comment.setStatus(CommentEntity.CommentStatus.DELETED);
                        comment.setUpdatedAt(Instant.now());
                        commentRepository.save(comment);
                        
                        // 更新计数并清理缓存
                        updateCommentCountAsync(comment.getArticleId(), -1);
                        clearCommentCacheAsync(comment.getArticleId());
                        
                        return true;
                    })
                    .orElse(false);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(success -> log.debug("评论删除完成: commentId={}, success={}", 
                commentId, success));
    }

    /**
     * 从数据库加载评论
     */
    private Flux<CommentResult> loadCommentsFromDatabase(String articleId, int page, int size, String sortBy) {
        return Mono.fromCallable(() -> {
            Pageable pageable = PageRequest.of(page, size);
            Page<CommentEntity> commentPage;
            
            switch (sortBy) {
                case "hot" -> commentPage = commentRepository
                        .findByArticleIdAndStatusOrderByLikeCountDescCreatedAtDesc(
                                articleId, CommentEntity.CommentStatus.APPROVED, pageable);
                default -> commentPage = commentRepository
                        .findByArticleIdAndStatusOrderByCreatedAtDesc(
                                articleId, CommentEntity.CommentStatus.APPROVED, pageable);
            }
            
            return commentPage.getContent();
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(comments -> Flux.fromIterable(
                comments.stream().map(this::convertToResult).toList()));
    }

    /**
     * 异步更新评论计数
     */
    private void updateCommentCountAsync(String articleId, int delta) {
        Mono.fromRunnable(() -> {
            try {
                commentCountRepository.findByArticleId(articleId)
                        .ifPresentOrElse(
                            count -> {
                                count.setCommentCount(Math.max(0, count.getCommentCount() + delta));
                                count.setApprovedCount(Math.max(0, count.getApprovedCount() + delta));
                                commentCountRepository.save(count);
                            },
                            () -> {
                                if (delta > 0) {
                                    CommentCountEntity newCount = CommentCountEntity.builder()
                                            .articleId(articleId)
                                            .commentCount(delta)
                                            .approvedCount(delta)
                                            .lastCommentAt(Instant.now())
                                            .build();
                                    commentCountRepository.save(newCount);
                                }
                            }
                        );
                
                // 清理计数缓存
                String countKey = COMMENT_COUNT_KEY_PREFIX + articleId;
                redisTemplate.delete(countKey).subscribe();
                
            } catch (Exception e) {
                log.error("更新评论计数失败: articleId={}", articleId, e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    /**
     * 异步刷新评论缓存
     */
    private void refreshCommentCacheAsync(String articleId) {
        Mono.fromRunnable(() -> {
            try {
                // 清理相关缓存键
                String pattern = COMMENT_LIST_KEY_PREFIX + articleId + ":*";
                redisTemplate.keys(pattern)
                        .flatMap(redisTemplate::delete)
                        .subscribe();
                
                // 清理热门评论缓存
                String hotKey = HOT_COMMENTS_KEY_PREFIX + articleId;
                redisTemplate.delete(hotKey).subscribe();
                
            } catch (Exception e) {
                log.error("刷新评论缓存失败: articleId={}", articleId, e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    /**
     * 异步清理评论缓存
     */
    private void clearCommentCacheAsync(String articleId) {
        refreshCommentCacheAsync(articleId);
    }

    /**
     * 转换实体为结果对象
     */
    private CommentResult convertToResult(CommentEntity entity) {
        return CommentResult.builder()
                .commentId(entity.getId())
                .articleId(entity.getArticleId())
                .userId(entity.getUserId())
                .username(entity.getUsername())
                .avatar(entity.getAvatar())
                .content(entity.getContent())
                .likeCount(entity.getLikeCount())
                .status(entity.getStatus().toString())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // 结果对象
    @lombok.Data
    @lombok.Builder
    public static class CommentResult {
        private Long commentId;
        private String articleId;
        private Long userId;
        private String username;
        private String avatar;
        private String content;
        private Integer likeCount;
        private String status;
        private Instant createdAt;
        private Instant updatedAt;
    }
}