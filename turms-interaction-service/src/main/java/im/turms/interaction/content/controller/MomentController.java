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

package im.turms.interaction.content.controller;

import im.turms.interaction.content.domain.*;
import im.turms.interaction.content.service.MomentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 朋友圈动态控制器
 */
@Slf4j
@RestController
@RequestMapping("/content/moments")
@RequiredArgsConstructor
public class MomentController {

    private final MomentService momentService;

    /**
     * 发布朋友圈动态
     */
    @PostMapping
    public Mono<Map<String, Object>> publishMoment(@RequestBody PublishMomentRequest request) {
        log.info("用户发布朋友圈: userId={}, type={}, privacy={}", 
                request.getUserId(), request.getType(), request.getPrivacy());
        
        return momentService.publishMoment(
                        request.getUserId(),
                        request.getContent(),
                        request.getAttachments(),
                        request.getType(),
                        request.getPrivacy(),
                        request.getVisibleUsers(),
                        request.getLocation()
                )
                .map(moment -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("momentId", moment.getId());
                        result.put("moderationStatus", moment.getModerationStatus());
                        result.put("message", getModerationMessage(moment.getModerationStatus()));
                        return result;
                })
                .onErrorResume(error -> {
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("error", error.getMessage());
                        return Mono.just(errorResult);
                });
    }

    /**
     * 获取朋友圈时间线
     */
    @GetMapping("/timeline")
    public Flux<Moment> getTimeline(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return momentService.getMomentTimeline(userId, page, size);
    }

    /**
     * 获取指定用户的朋友圈
     */
    @GetMapping("/user/{targetUserId}")
    public Flux<Moment> getUserMoments(
            @PathVariable Long targetUserId,
            @RequestParam Long viewerUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return momentService.getUserMoments(targetUserId, viewerUserId, page, size);
    }

    /**
     * 点赞/取消点赞动态
     */
    @PostMapping("/{momentId}/like")
    public Mono<Map<String, Object>> toggleLike(
            @PathVariable String momentId,
            @RequestBody Map<String, Object> request) {
        
        Long userId = ((Number) request.get("userId")).longValue();
        
        return momentService.toggleLike(momentId, userId)
                .map(isLiked -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("isLiked", isLiked);
                        result.put("message", isLiked ? "点赞成功" : "取消点赞成功");
                        return result;
                })
                .onErrorResume(error -> {
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("error", error.getMessage());
                        return Mono.just(errorResult);
                });
    }

    /**
     * 添加评论
     */
    @PostMapping("/{momentId}/comments")
    public Mono<Map<String, Object>> addComment(
            @PathVariable String momentId,
            @RequestBody AddCommentRequest request) {
        
        return momentService.addComment(
                        momentId,
                        request.getUserId(),
                        request.getContent(),
                        request.getParentId(),
                        request.getReplyToUserId()
                )
                .map(comment -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("commentId", comment.getId());
                        result.put("message", "评论成功");
                        return result;
                })
                .onErrorResume(error -> {
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("error", error.getMessage());
                        return Mono.just(errorResult);
                });
    }

    /**
     * 获取动态评论列表
     */
    @GetMapping("/{momentId}/comments")
    public Flux<MomentComment> getComments(
            @PathVariable String momentId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        return momentService.getMomentComments(momentId, userId, page, size);
    }

    /**
     * 删除动态
     */
    @DeleteMapping("/{momentId}")
    public Mono<Map<String, Object>> deleteMoment(
            @PathVariable String momentId,
            @RequestParam Long userId) {
        
        return momentService.deleteMoment(momentId, userId)
                .map(deleted -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", deleted);
                        result.put("message", deleted ? "删除成功" : "删除失败");
                        return result;
                })
                .onErrorResume(error -> {
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("error", error.getMessage());
                        return Mono.just(errorResult);
                });
    }

    private String getModerationMessage(Moment.ModerationStatus status) {
        return switch (status) {
            case APPROVED -> "动态发布成功";
            case PENDING -> "动态正在审核中";
            case REVIEW_NEEDED -> "动态需要人工审核";
            case REJECTED -> "动态审核未通过";
        };
    }

    /**
     * 发布动态请求DTO
     */
    @lombok.Data
    public static class PublishMomentRequest {
        private Long userId;
        private String content;
        private List<MomentAttachment> attachments;
        private Moment.MomentType type;
        private Moment.MomentPrivacy privacy;
        private Set<Long> visibleUsers;
        private MomentLocation location;
    }

    /**
     * 添加评论请求DTO
     */
    @lombok.Data
    public static class AddCommentRequest {
        private Long userId;
        private String content;
        private String parentId;
        private Long replyToUserId;
    }
}