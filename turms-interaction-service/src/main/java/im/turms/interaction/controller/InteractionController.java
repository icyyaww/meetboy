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

package im.turms.interaction.controller;

import im.turms.interaction.service.LikeServiceV3;
import im.turms.interaction.service.CommentServiceV3;
import im.turms.interaction.dto.UserInfo;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

/**
 * 统一互动控制器
 * 
 * 设计原则：
 * 1. 统一的API路径：/api/v1/interaction
 * 2. 统一的响应格式：ApiResponse<T>
 * 3. 集成点赞和评论功能，支持所有内容类型
 * 4. 使用最新的V3服务（带用户验证和权限检查）
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/interaction")
@RequiredArgsConstructor
public class InteractionController {

    private final LikeServiceV3 likeServiceV3;
    private final CommentServiceV3 commentServiceV3;

    // ================== 点赞功能 ==================

    /**
     * 切换点赞状态 (支持所有内容类型：朋友圈、评论、帖子等)
     */
    @PostMapping("/like")
    public Mono<ApiResponse<LikeResult>> toggleLike(@RequestBody LikeRequest request) {
        log.info("点赞请求: userId={}, target={}:{}", 
                request.getUserId(), request.getTargetType(), request.getTargetId());
        
        return likeServiceV3.toggleLikeWithUserValidation(
                        request.getUserId(),
                        request.getTargetType(),
                        request.getTargetId(),
                        request.getDeviceType(),
                        request.getDeviceId(),
                        request.getIpAddress(),
                        request.getLocationInfo())
                .map(result -> ApiResponse.success(LikeResult.builder()
                        .isLiked(result.getLikeResult().isLiked())
                        .likeCount(result.getLikeResult().getLikeCount())
                        .userInfo(result.getUserInfo())
                        .timestamp(result.getTimestamp())
                        .build(), result.getLikeResult().isLiked() ? "点赞成功" : "取消点赞成功"))
                .onErrorResume(error -> {
                    log.error("点赞失败: userId={}, target={}:{}", 
                            request.getUserId(), request.getTargetType(), request.getTargetId(), error);
                    return Mono.just(ApiResponse.<LikeResult>error("点赞失败: " + error.getMessage()));
                });
    }

    /**
     * 获取点赞用户列表
     */
    @GetMapping("/likes")
    public Mono<ApiResponse<LikeUsersResult>> getLikeUsers(
            @RequestParam String targetType,
            @RequestParam String targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long viewerUserId) {
        
        return likeServiceV3.getLikeUsersWithDetails(targetType, targetId, page, size, viewerUserId)
                .collectList()
                .map(users -> ApiResponse.success(LikeUsersResult.builder()
                        .users(users)
                        .pagination(Pagination.builder()
                                .page(page)
                                .size(size)
                                .hasNext(users.size() == size)
                                .build())
                        .build()))
                .onErrorResume(error -> {
                    log.error("获取点赞用户列表失败: target={}:{}", targetType, targetId, error);
                    return Mono.just(ApiResponse.<LikeUsersResult>error("获取点赞用户列表失败: " + error.getMessage()));
                });
    }

    // ================== 评论功能 ==================

    /**
     * 添加评论 (支持所有内容类型)
     */
    @PostMapping("/comment")
    public Mono<ApiResponse<CommentResult>> addComment(@RequestBody CommentRequest request) {
        log.info("评论请求: userId={}, target={}", request.getUserId(), request.getTargetId());
        
        return commentServiceV3.addCommentWithUserInfo(
                        request.getTargetId(),
                        request.getUserId(),
                        request.getContent(),
                        request.getDeviceType(),
                        request.getIpAddress(),
                        request.getLocationInfo())
                .map(result -> ApiResponse.success(CommentResult.builder()
                        .commentId(result.getCommentResult().getCommentId())
                        .content(result.getCommentResult().getContent())
                        .userInfo(result.getUserInfo())
                        .timestamp(result.getTimestamp())
                        .build(), "评论成功"))
                .onErrorResume(error -> {
                    log.error("评论失败: userId={}, target={}", request.getUserId(), request.getTargetId(), error);
                    return Mono.just(ApiResponse.<CommentResult>error("评论失败: " + error.getMessage()));
                });
    }

    /**
     * 获取评论列表
     */
    @GetMapping("/comments")
    public Mono<ApiResponse<CommentsResult>> getComments(
            @RequestParam String targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sortBy,
            @RequestParam(required = false) Long viewerUserId) {
        
        return commentServiceV3.getCommentsWithUserDetails(targetId, page, size, sortBy, viewerUserId)
                .collectList()
                .map(comments -> ApiResponse.success(CommentsResult.builder()
                        .comments(comments)
                        .pagination(Pagination.builder()
                                .page(page)
                                .size(size)
                                .hasNext(comments.size() == size)
                                .build())
                        .build()))
                .onErrorResume(error -> {
                    log.error("获取评论列表失败: targetId={}", targetId, error);
                    return Mono.just(ApiResponse.<CommentsResult>error("获取评论列表失败: " + error.getMessage()));
                });
    }

    // ================== 朋友圈专用接口 ==================

    /**
     * 朋友圈点赞 (便捷接口)
     */
    @PostMapping("/moments/{momentId}/like")
    public Mono<ApiResponse<LikeResult>> likeMoment(
            @PathVariable String momentId,
            @RequestBody SimpleLikeRequest request) {
        
        LikeRequest likeRequest = LikeRequest.builder()
                .userId(request.getUserId())
                .targetType("MOMENT")
                .targetId(momentId)
                .deviceType(request.getDeviceType())
                .deviceId(request.getDeviceId())
                .ipAddress(request.getIpAddress())
                .locationInfo(request.getLocationInfo())
                .build();
        
        return toggleLike(likeRequest);
    }

    /**
     * 朋友圈评论 (便捷接口)
     */
    @PostMapping("/moments/{momentId}/comments")
    public Mono<ApiResponse<CommentResult>> commentMoment(
            @PathVariable String momentId,
            @RequestBody SimpleCommentRequest request) {
        
        CommentRequest commentRequest = CommentRequest.builder()
                .targetId(momentId)
                .userId(request.getUserId())
                .content(request.getContent())
                .deviceType(request.getDeviceType())
                .ipAddress(request.getIpAddress())
                .locationInfo(request.getLocationInfo())
                .build();
        
        return addComment(commentRequest);
    }

    /**
     * 获取朋友圈点赞列表
     */
    @GetMapping("/moments/{momentId}/likes")
    public Mono<ApiResponse<LikeUsersResult>> getMomentLikes(
            @PathVariable String momentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long viewerUserId) {
        
        return getLikeUsers("MOMENT", momentId, page, size, viewerUserId);
    }

    /**
     * 获取朋友圈评论列表
     */
    @GetMapping("/moments/{momentId}/comments")
    public Mono<ApiResponse<CommentsResult>> getMomentComments(
            @PathVariable String momentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sortBy,
            @RequestParam(required = false) Long viewerUserId) {
        
        return getComments(momentId, page, size, sortBy, viewerUserId);
    }

    // ================== 管理员接口 ==================
    
    /**
     * 管理员获取点赞列表 (分页)
     */
    @GetMapping("/likes/page")
    public Mono<Map<String, Object>> getAdminLikesPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) Long userId) {
        
        log.info("Admin request: getLikesPage with page={}, size={}, targetType={}, targetId={}, userId={}", 
                page, size, targetType, targetId, userId);
        
        // Create mock admin data structure
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> records = new ArrayList<>();
        
        // Generate mock data
        for (int i = 0; i < size; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", page * size + i + 1);
            record.put("userId", 1000L + i);
            record.put("targetType", targetType != null ? targetType : "MOMENT");
            record.put("targetId", targetId != null ? targetId : "moment" + (i + 1));
            record.put("createdAt", "2025-06-23T15:18:00Z");
            record.put("updatedAt", "2025-06-23T15:18:00Z");
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", 1000L + i);
            userInfo.put("name", "User" + (i + 1));
            userInfo.put("avatar", "https://example.com/avatar" + (i + 1) + ".jpg");
            record.put("userInfo", userInfo);
            
            records.add(record);
        }
        
        response.put("records", records);
        response.put("total", 12450L);
        response.put("page", page);
        response.put("size", size);
        response.put("hasNext", page < 622); // 12450 / 20 = 622.5
        
        return Mono.just(response);
    }
    
    /**
     * 管理员获取评论列表 (分页)
     */
    @GetMapping("/comments/page")
    public Mono<Map<String, Object>> getAdminCommentsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) Long userId) {
        
        log.info("Admin request: getCommentsPage with page={}, size={}, targetId={}, userId={}", 
                page, size, targetId, userId);
        
        // Create mock admin data structure
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> records = new ArrayList<>();
        
        // Generate mock data
        for (int i = 0; i < size; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", page * size + i + 1);
            record.put("userId", 2000L + i);
            record.put("targetId", targetId != null ? targetId : "moment" + (i + 1));
            record.put("content", "这是一条评论内容 " + (i + 1));
            record.put("createdAt", "2025-06-23T15:18:00Z");
            record.put("updatedAt", "2025-06-23T15:18:00Z");
            record.put("status", "ACTIVE");
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", 2000L + i);
            userInfo.put("name", "Commenter" + (i + 1));
            userInfo.put("avatar", "https://example.com/avatar" + (2000 + i) + ".jpg");
            record.put("userInfo", userInfo);
            
            records.add(record);
        }
        
        response.put("records", records);
        response.put("total", 8754L);
        response.put("page", page);
        response.put("size", size);
        response.put("hasNext", page < 437); // 8754 / 20 = 437.7
        
        return Mono.just(response);
    }

    // ================== 系统功能 ==================


    // ================== 请求/响应 DTO ==================

    @Data
    @lombok.Builder
    public static class LikeRequest {
        private Long userId;
        private String targetType;
        private String targetId;
        private String deviceType;
        private String deviceId;
        private String ipAddress;
        private String locationInfo;
    }

    @Data
    @lombok.Builder
    public static class CommentRequest {
        private String targetId;
        private Long userId;
        private String content;
        private String deviceType;
        private String ipAddress;
        private String locationInfo;
    }

    @Data
    @lombok.Builder
    public static class SimpleLikeRequest {
        private Long userId;
        private String deviceType;
        private String deviceId;
        private String ipAddress;
        private String locationInfo;
    }

    @Data
    @lombok.Builder
    public static class SimpleCommentRequest {
        private Long userId;
        private String content;
        private String deviceType;
        private String ipAddress;
        private String locationInfo;
    }

    @Data
    @lombok.Builder
    public static class LikeResult {
        private boolean isLiked;
        private Integer likeCount;
        private UserInfo userInfo;
        private Instant timestamp;
    }

    @Data
    @lombok.Builder
    public static class CommentResult {
        private Long commentId;
        private String content;
        private UserInfo userInfo;
        private Instant timestamp;
    }

    @Data
    @lombok.Builder
    public static class LikeUsersResult {
        private List<LikeServiceV3.LikeUserDetail> users;
        private Pagination pagination;
    }

    @Data
    @lombok.Builder
    public static class CommentsResult {
        private List<CommentServiceV3.EnhancedCommentResult> comments;
        private Pagination pagination;
    }

    @Data
    @lombok.Builder
    public static class Pagination {
        private int page;
        private int size;
        private boolean hasNext;
    }

    /**
     * 统一API响应格式
     */
    @Data
    @lombok.Builder
    public static class ApiResponse<T> {
        private boolean success;
        private T data;
        private String message;
        private String error;
        private Instant timestamp;

        public static <T> ApiResponse<T> success(T data, String message) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .data(data)
                    .message(message)
                    .timestamp(Instant.now())
                    .build();
        }

        public static <T> ApiResponse<T> success(T data) {
            return success(data, "操作成功");
        }

        public static <T> ApiResponse<T> error(String error) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .error(error)
                    .timestamp(Instant.now())
                    .build();
        }
    }
}