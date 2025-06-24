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

package im.turms.interaction.content.service;

import im.turms.interaction.service.UserServiceClient;
import im.turms.interaction.service.LikeServiceV3;
import im.turms.interaction.content.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 朋友圈业务服务 (已迁移到interaction服务)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MomentService {

    private final ReactiveMongoTemplate mongoTemplate;
    private final ContentModerationService moderationService;
    private final UserServiceClient userServiceClient;
    private final LikeServiceV3 likeServiceV3;

    /**
     * 发布朋友圈动态
     */
    public Mono<Moment> publishMoment(Long userId, String content, List<MomentAttachment> attachments,
                                      Moment.MomentType type, Moment.MomentPrivacy privacy,
                                      Set<Long> visibleUsers, MomentLocation location) {
        
        return userServiceClient.getUserInfo(userId)
                .flatMap(userInfo -> {
                    // 提取链接和多媒体URL用于审核
                    List<String> imageUrls = extractImageUrls(attachments);
                    List<String> videoUrls = extractVideoUrls(attachments);
                    List<String> links = extractLinks(content);
                    
                    // 内容审核
                    return moderationService.moderateContent(content, imageUrls, videoUrls, links)
                            .flatMap(moderationResult -> {
                                // 构建朋友圈实体
                                Moment moment = Moment.builder()
                                        .userId(userId)
                                        .username(userInfo.getUsername())
                                        .avatar(userInfo.getAvatar())
                                        .content(content)
                                        .attachments(attachments)
                                        .type(type)
                                        .privacy(privacy)
                                        .visibleUsers(visibleUsers)
                                        .location(location)
                                        .likeCount(0)
                                        .commentCount(0)
                                        .shareCount(0)
                                        .moderationStatus(determineModerationStatus(moderationResult))
                                        .moderationResult(moderationResult)
                                        .pinned(false)
                                        .createdDate(Instant.now())
                                        .lastModifiedDate(Instant.now())
                                        .build();
                                
                                // 根据审核结果决定是否保存
                                if (moment.getModerationStatus() == Moment.ModerationStatus.REJECTED) {
                                    return Mono.error(new RuntimeException("内容审核未通过: " + 
                                                     moderationResult.getRecommendation()));
                                }
                                
                                return mongoTemplate.save(moment);
                            });
                })
                .doOnSuccess(moment -> log.info("朋友圈发布成功: userId={}, momentId={}, status={}", 
                                               userId, moment.getId(), moment.getModerationStatus()));
    }

    /**
     * 获取朋友圈动态列表 (首页时间线)
     */
    public Flux<Moment> getMomentTimeline(Long userId, int page, int size) {
        return userServiceClient.getUserFriends(userId)
                .collectList()
                .flatMapMany(friendIds -> {
                    // 包含自己和好友的动态
                    Set<Long> userIds = new HashSet<>(friendIds);
                    userIds.add(userId);
                    
                    Query query = Query.query(
                            Criteria.where("userId").in(userIds)
                                    .and("moderationStatus").in(Moment.ModerationStatus.APPROVED)
                                    .and("expiresAt").gte(Instant.now()).orOperator(
                                            Criteria.where("expiresAt").exists(false)
                                    )
                    ).with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate")));
                    
                    return mongoTemplate.find(query, Moment.class);
                })
                .filter(moment -> isVisible(moment, userId));
    }

    /**
     * 获取用户的朋友圈动态
     */
    public Flux<Moment> getUserMoments(Long targetUserId, Long viewerUserId, int page, int size) {
        return userServiceClient.areUsersFriends(viewerUserId, targetUserId)
                .flatMapMany(areFriends -> {
                    if (!areFriends && !viewerUserId.equals(targetUserId)) {
                        return Flux.empty(); // 非好友不能查看
                    }
                    
                    Query query = Query.query(
                            Criteria.where("userId").is(targetUserId)
                                    .and("moderationStatus").is(Moment.ModerationStatus.APPROVED)
                    ).with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate")));
                    
                    return mongoTemplate.find(query, Moment.class);
                })
                .filter(moment -> isVisible(moment, viewerUserId));
    }

    /**
     * 点赞/取消点赞 (与interaction服务集成)
     */
    public Mono<Boolean> toggleLike(String momentId, Long userId) {
        return mongoTemplate.findById(momentId, Moment.class)
                .switchIfEmpty(Mono.error(new RuntimeException("动态不存在")))
                .flatMap(moment -> {
                    if (!isVisible(moment, userId)) {
                        return Mono.error(new RuntimeException("无权限访问此动态"));
                    }
                    
                    // 使用LikeServiceV3处理点赞逻辑
                    return likeServiceV3.toggleLikeWithUserValidation(
                            userId, 
                            "MOMENT", 
                            momentId, 
                            null, // deviceType 
                            null, // deviceId
                            null, // ipAddress
                            null  // locationInfo
                    ).flatMap(enhancedResult -> {
                        // 更新Moment中的likeCount缓存
                        int countChange = enhancedResult.getLikeResult().isLiked() ? 1 : -1;
                        Update update = new Update().inc("likeCount", countChange);
                        
                        return mongoTemplate.updateFirst(
                                Query.query(Criteria.where("id").is(momentId)),
                                update,
                                Moment.class
                        ).map(result -> enhancedResult.getLikeResult().isLiked());
                    });
                });
    }

    /**
     * 添加评论 (与interaction服务集成)
     */
    public Mono<MomentComment> addComment(String momentId, Long userId, String content, 
                                         String parentId, Long replyToUserId) {
        
        return mongoTemplate.findById(momentId, Moment.class)
                .switchIfEmpty(Mono.error(new RuntimeException("动态不存在")))
                .flatMap(moment -> {
                    if (!isVisible(moment, userId)) {
                        return Mono.error(new RuntimeException("无权限访问此动态"));
                    }
                    
                    return userServiceClient.getUserInfo(userId);
                })
                .flatMap(userInfo -> {
                    // 审核评论内容
                    return moderationService.quickModerate(content)
                            .flatMap(isApproved -> {
                                if (!isApproved) {
                                    return Mono.error(new RuntimeException("评论内容包含敏感信息"));
                                }
                                
                                // 获取回复目标用户信息
                                Mono<String> replyToUsernameMono = replyToUserId != null ?
                                        userServiceClient.getUserInfo(replyToUserId)
                                                .map(userInfoReply -> userInfoReply.getUsername())
                                                .onErrorReturn("未知用户") :
                                        Mono.just(null);
                                
                                return replyToUsernameMono.flatMap(replyToUsername -> {
                                    MomentComment comment = MomentComment.builder()
                                            .momentId(momentId)
                                            .userId(userId)
                                            .username(userInfo.getUsername())
                                            .avatar(userInfo.getAvatar())
                                            .content(content)
                                            .parentId(parentId)
                                            .rootId(parentId != null ? parentId : null)
                                            .replyToUserId(replyToUserId)
                                            .replyToUsername(replyToUsername)
                                            .likes(new HashSet<>())
                                            .likeCount(0)
                                            .replyCount(0)
                                            .moderationStatus(Moment.ModerationStatus.APPROVED)
                                            .createdDate(Instant.now())
                                            .lastModifiedDate(Instant.now())
                                            .build();
                                    
                                    return mongoTemplate.save(comment)
                                            .flatMap(savedComment -> {
                                                // 更新动态的评论数
                                                return mongoTemplate.updateFirst(
                                                        Query.query(Criteria.where("id").is(momentId)),
                                                        new Update().inc("commentCount", 1),
                                                        Moment.class
                                                ).thenReturn(savedComment);
                                            });
                                });
                            });
                });
    }

    /**
     * 获取动态评论
     */
    public Flux<MomentComment> getMomentComments(String momentId, Long userId, int page, int size) {
        return mongoTemplate.findById(momentId, Moment.class)
                .switchIfEmpty(Mono.error(new RuntimeException("动态不存在")))
                .flatMapMany(moment -> {
                    if (!isVisible(moment, userId)) {
                        return Flux.error(new RuntimeException("无权限访问此动态"));
                    }
                    
                    Query query = Query.query(
                            Criteria.where("momentId").is(momentId)
                                    .and("moderationStatus").is(Moment.ModerationStatus.APPROVED)
                    ).with(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdDate")));
                    
                    return mongoTemplate.find(query, MomentComment.class);
                });
    }

    /**
     * 删除动态
     */
    public Mono<Boolean> deleteMoment(String momentId, Long userId) {
        return mongoTemplate.findById(momentId, Moment.class)
                .switchIfEmpty(Mono.error(new RuntimeException("动态不存在")))
                .flatMap(moment -> {
                    if (!moment.getUserId().equals(userId)) {
                        return Mono.error(new RuntimeException("只能删除自己的动态"));
                    }
                    
                    // 删除动态及其所有评论
                    return mongoTemplate.remove(Query.query(Criteria.where("momentId").is(momentId)), 
                                               MomentComment.class)
                            .then(mongoTemplate.remove(moment))
                            .map(result -> result.getDeletedCount() > 0);
                });
    }

    /**
     * 根据审核结果确定动态状态
     */
    private Moment.ModerationStatus determineModerationStatus(im.turms.interaction.content.domain.ModerationResult moderationResult) {
        double score = moderationResult.getOverallScore();
        
        if (score >= 0.8) {
            return Moment.ModerationStatus.APPROVED;
        } else if (score >= 0.5) {
            return Moment.ModerationStatus.REVIEW_NEEDED;
        } else {
            return Moment.ModerationStatus.REJECTED;
        }
    }

    /**
     * 检查动态对用户是否可见
     */
    private boolean isVisible(Moment moment, Long userId) {
        // 自己的动态总是可见
        if (moment.getUserId().equals(userId)) {
            return true;
        }
        
        switch (moment.getPrivacy()) {
            case PUBLIC:
                return true;
            case FRIENDS_ONLY:
                // 需要检查是否为好友关系（这里简化处理）
                return true;
            case CUSTOM:
                return moment.getVisibleUsers() != null && 
                       moment.getVisibleUsers().contains(userId);
            case PRIVATE:
                return false;
            default:
                return false;
        }
    }

    private List<String> extractImageUrls(List<MomentAttachment> attachments) {
        if (attachments == null) return List.of();
        return attachments.stream()
                .filter(att -> att.getType() == MomentAttachment.AttachmentType.IMAGE)
                .map(MomentAttachment::getUrl)
                .collect(Collectors.toList());
    }

    private List<String> extractVideoUrls(List<MomentAttachment> attachments) {
        if (attachments == null) return List.of();
        return attachments.stream()
                .filter(att -> att.getType() == MomentAttachment.AttachmentType.VIDEO)
                .map(MomentAttachment::getUrl)
                .collect(Collectors.toList());
    }

    private List<String> extractLinks(String content) {
        if (content == null) return List.of();
        // 简化的链接提取（实际应使用正则表达式）
        return List.of();
    }
}