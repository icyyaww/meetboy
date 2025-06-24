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
import im.turms.interaction.service.LikeServiceV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * 点赞服务V3 - 增强用户关联功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LikeServiceV3 {

    private final LikeServiceV2 likeServiceV2;
    private final UserServiceClient userServiceClient;

    /**
     * 切换点赞状态 (增强用户权限验证)
     */
    public Mono<EnhancedLikeResult> toggleLikeWithUserValidation(Long userId, String targetType, 
                                                               String targetId, String deviceType, 
                                                               String deviceId, String ipAddress, 
                                                               String locationInfo) {
        
        // 1. 验证用户信息
        return userServiceClient.getUserInfo(userId)
                .switchIfEmpty(Mono.error(new RuntimeException("用户不存在")))
                .flatMap(userInfo -> {
                    // 2. 检查权限（如果是朋友圈内容，需要验证好友关系）
                    if ("MOMENT".equals(targetType)) {
                        return validateMomentLikePermission(userId, targetId, userInfo);
                    }
                    return Mono.just(userInfo);
                })
                .flatMap(userInfo -> {
                    // 3. 执行点赞操作
                    return likeServiceV2.toggleLike(userId, targetType, targetId, 
                                                   deviceType, deviceId, ipAddress, locationInfo)
                            .map(result -> EnhancedLikeResult.builder()
                                    .likeResult(result)
                                    .userInfo(userInfo)
                                    .timestamp(Instant.now())
                                    .hasPermission(true)
                                    .build());
                })
                .doOnSuccess(result -> log.debug("增强点赞操作完成: userId={}, target={}:{}", 
                        userId, targetType, targetId));
    }

    /**
     * 获取点赞用户详细信息列表
     */
    public Flux<LikeUserDetail> getLikeUsersWithDetails(String targetType, String targetId, 
                                                       int page, int size, Long viewerUserId) {
        
        return likeServiceV2.getLikeUsers(targetType, targetId, page, size)
                .flatMap(userId -> {
                    // 获取点赞用户的详细信息
                    return userServiceClient.getUserInfo(userId)
                            .map(userInfo -> LikeUserDetail.builder()
                                    .userId(userId)
                                    .username(userInfo.getUsername())
                                    .avatar(userInfo.getAvatar())
                                    .nickname(userInfo.getNickname())
                                    .isFriend(false) // 后面异步填充
                                    .build())
                            .doOnNext(detail -> {
                                // 异步检查是否为好友
                                if (viewerUserId != null && !viewerUserId.equals(userId)) {
                                    userServiceClient.areUsersFriends(viewerUserId, userId)
                                            .subscribe(isFriend -> detail.setFriend(isFriend));
                                }
                            });
                })
                .doOnComplete(() -> log.debug("点赞用户详情查询完成"));
    }

    /**
     * 获取用户的点赞历史 (带内容信息)
     */
    public Flux<UserLikeHistory> getUserLikeHistory(Long userId, int page, int size) {
        
        return userServiceClient.getUserInfo(userId)
                .switchIfEmpty(Mono.error(new RuntimeException("用户不存在")))
                .flatMapMany(userInfo -> {
                    // 这里需要从数据库查询用户的点赞历史
                    // 目前的LikeServiceV2没有这个功能，需要补充
                    return Flux.empty(); // TODO: 实现用户点赞历史查询
                });
    }

    /**
     * 验证朋友圈点赞权限
     */
    private Mono<UserInfo> validateMomentLikePermission(Long userId, String targetId, UserInfo userInfo) {
        // TODO: 调用朋友圈服务检查权限
        // 1. 获取朋友圈作者ID
        // 2. 检查朋友圈可见性设置
        // 3. 验证是否有点赞权限
        
        return Mono.just(userInfo); // 暂时通过，后续集成朋友圈服务后实现
    }

    // 数据传输对象
    @lombok.Data
    @lombok.Builder
    public static class EnhancedLikeResult {
        private LikeServiceV2.LikeResult likeResult;
        private UserInfo userInfo;
        private Instant timestamp;
        private boolean hasPermission;
    }

    @lombok.Data
    @lombok.Builder
    public static class LikeUserDetail {
        private Long userId;
        private String username;
        private String avatar;
        private String nickname;
        private boolean isFriend;
        private Instant likedAt;
    }

    @lombok.Data
    @lombok.Builder
    public static class UserLikeHistory {
        private String targetType;
        private String targetId;
        private String targetTitle;
        private String targetAuthor;
        private Instant likedAt;
        private boolean isValid; // 目标内容是否还存在
    }
}