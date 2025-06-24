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

import im.turms.interaction.dto.UserInfo;
import im.turms.interaction.service.CommentServiceV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评论服务V3 - 增强用户关联功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceV3 {

    private final CommentServiceV2 commentServiceV2;
    private final UserServiceClient userServiceClient;

    /**
     * 添加评论 (自动获取用户信息)
     */
    public Mono<EnhancedCommentResult> addCommentWithUserInfo(String articleId, Long userId, 
                                                            String content, String deviceType, 
                                                            String ipAddress, String locationInfo) {
        
        // 1. 获取用户信息
        return userServiceClient.getUserInfo(userId)
                .switchIfEmpty(Mono.error(new RuntimeException("用户不存在")))
                .flatMap(userInfo -> {
                    // 2. 检查评论权限
                    if ("MOMENT".equals(getContentType(articleId))) {
                        return validateMomentCommentPermission(userId, articleId, userInfo);
                    }
                    return Mono.just(userInfo);
                })
                .flatMap(userInfo -> {
                    // 3. 添加评论
                    return commentServiceV2.addComment(
                            articleId, userId, userInfo.getUsername(), 
                            userInfo.getAvatar(), content, deviceType, 
                            ipAddress, locationInfo)
                            .map(result -> EnhancedCommentResult.builder()
                                    .commentResult(result)
                                    .userInfo(userInfo)
                                    .hasPermission(true)
                                    .timestamp(Instant.now())
                                    .build());
                })
                .doOnSuccess(result -> log.debug("增强评论添加完成: articleId={}, userId={}", 
                        articleId, userId));
    }

    /**
     * 获取文章评论列表 (增强用户信息)
     */
    public Flux<EnhancedCommentResult> getCommentsWithUserDetails(String articleId, int page, 
                                                                int size, String sortBy, 
                                                                Long viewerUserId) {
        
        return commentServiceV2.getCommentsByArticle(articleId, page, size, sortBy)
                .flatMap(comment -> {
                    // 获取评论者的最新用户信息
                    return userServiceClient.getUserInfo(comment.getUserId())
                            .map(userInfo -> EnhancedCommentResult.builder()
                                    .commentResult(comment)
                                    .userInfo(userInfo)
                                    .hasPermission(true)
                                    .timestamp(comment.getCreatedAt())
                                    .isFriend(false) // 后面异步填充
                                    .build())
                            .doOnNext(result -> {
                                // 异步检查是否为好友
                                if (viewerUserId != null && !viewerUserId.equals(comment.getUserId())) {
                                    userServiceClient.areUsersFriends(viewerUserId, comment.getUserId())
                                            .subscribe(isFriend -> result.setFriend(isFriend));
                                }
                            });
                })
                .doOnComplete(() -> log.debug("增强评论列表查询完成"));
    }

    /**
     * 获取用户的评论历史
     */
    public Flux<UserCommentHistory> getUserCommentHistory(Long userId, int page, int size) {
        
        return userServiceClient.getUserInfo(userId)
                .switchIfEmpty(Mono.error(new RuntimeException("用户不存在")))
                .flatMapMany(userInfo -> {
                    // TODO: 需要在CommentRepository中添加按用户查询的方法
                    return Flux.empty(); // 暂时返回空，需要实现
                });
    }

    /**
     * 批量获取评论中提到的用户信息
     */
    public Mono<CommentWithMentions> getCommentWithMentions(Long commentId, Long viewerUserId) {
        
        // TODO: 实现@用户功能
        // 1. 解析评论内容中的@mentions
        // 2. 批量获取被提到用户的信息
        // 3. 检查查看者与被提到用户的关系
        
        return Mono.empty(); // 暂时返回空，需要实现
    }

    /**
     * 获取用户间的互动统计
     */
    public Mono<UserInteractionStats> getUserInteractionStats(Long userId1, Long userId2) {
        
        return userServiceClient.areUsersFriends(userId1, userId2)
                .flatMap(areFriends -> {
                    if (!areFriends) {
                        return Mono.just(UserInteractionStats.builder()
                                .userId1(userId1)
                                .userId2(userId2)
                                .areFriends(false)
                                .build());
                    }
                    
                    // TODO: 统计两个用户之间的互动数据
                    // 1. 互相点赞次数
                    // 2. 互相评论次数  
                    // 3. 最近互动时间
                    
                    return Mono.just(UserInteractionStats.builder()
                            .userId1(userId1)
                            .userId2(userId2)
                            .areFriends(true)
                            .mutualLikes(0)
                            .mutualComments(0)
                            .lastInteractionAt(null)
                            .build());
                });
    }

    /**
     * 验证朋友圈评论权限
     */
    private Mono<UserInfo> validateMomentCommentPermission(Long userId, String articleId, UserInfo userInfo) {
        // TODO: 调用朋友圈服务检查权限
        // 1. 获取朋友圈作者ID
        // 2. 检查朋友圈可见性设置
        // 3. 验证是否有评论权限
        
        return Mono.just(userInfo); // 暂时通过，后续集成朋友圈服务后实现
    }

    /**
     * 根据articleId判断内容类型
     */
    private String getContentType(String articleId) {
        if (articleId.startsWith("moment_")) {
            return "MOMENT";
        } else if (articleId.startsWith("post_")) {
            return "POST";
        }
        return "UNKNOWN";
    }

    // 数据传输对象
    @lombok.Data
    @lombok.Builder
    public static class EnhancedCommentResult {
        private CommentServiceV2.CommentResult commentResult;
        private UserInfo userInfo;
        private boolean hasPermission;
        private Instant timestamp;
        private boolean isFriend;
    }

    @lombok.Data
    @lombok.Builder
    public static class UserCommentHistory {
        private String articleId;
        private String articleTitle;
        private String articleAuthor;
        private String commentContent;
        private Instant commentedAt;
        private boolean isValid;
    }

    @lombok.Data
    @lombok.Builder
    public static class CommentWithMentions {
        private CommentServiceV2.CommentResult comment;
        private Set<UserInfo> mentionedUsers;
        private Set<Long> friendMentions;
    }

    @lombok.Data
    @lombok.Builder
    public static class UserInteractionStats {
        private Long userId1;
        private Long userId2;
        private boolean areFriends;
        private int mutualLikes;
        private int mutualComments;
        private Instant lastInteractionAt;
    }
}