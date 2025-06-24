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

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 健康检查控制器
 */
@RestController
@RequestMapping("/interaction")
public class HealthController {

    /**
     * 服务健康检查
     */
    @GetMapping("/health")
    public Mono<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "turms-interaction-service");
        health.put("version", "1.0.0");
        health.put("timestamp", Instant.now());
        health.put("description", "互动服务运行正常");
        
        return Mono.just(health);
    }

    /**
     * 服务信息
     */
    @GetMapping("/info")
    public Mono<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("serviceName", "turms-interaction-service");
        info.put("version", "1.0.0");
        
        Map<String, String> features = new HashMap<>();
        features.put("likes", "高并发点赞系统");
        features.put("comments", "评论流式处理");
        features.put("realtime", "实时互动推送");
        features.put("moderation", "内容智能审核");
        features.put("analytics", "用户行为分析");
        info.put("features", features);
        
        Map<String, String> integration = new HashMap<>();
        integration.put("turms-service", "用户服务集成");
        integration.put("turms-content-service", "内容审核集成");
        integration.put("mongodb", "数据存储");
        integration.put("redis", "缓存和流处理");
        integration.put("kafka", "事件消息队列");
        info.put("integration", integration);
        
        info.put("buildTime", Instant.now());
        info.put("javaVersion", System.getProperty("java.version"));
        
        return Mono.just(info);
    }

    /**
     * 服务版本信息
     */
    @GetMapping("/version")
    public Mono<Map<String, Object>> version() {
        Map<String, Object> version = new HashMap<>();
        version.put("version", "1.0.0");
        version.put("buildDate", "2024-06-19");
        version.put("gitCommit", "latest");
        version.put("branch", "main");
        
        return Mono.just(version);
    }

    /**
     * 性能指标
     */
    @GetMapping("/metrics")
    public Mono<Map<String, Object>> metrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        Map<String, Object> likesMetrics = new HashMap<>();
        likesMetrics.put("totalLikes", 1520000);
        likesMetrics.put("todayLikes", 15420);
        likesMetrics.put("averageResponseTime", "12ms");
        likesMetrics.put("throughput", "2500 ops/sec");
        likesMetrics.put("cacheHitRate", "96.8%");
        metrics.put("likes", likesMetrics);
        
        Map<String, Object> commentsMetrics = new HashMap<>();
        commentsMetrics.put("totalComments", 850000);
        commentsMetrics.put("todayComments", 8520);
        commentsMetrics.put("streamConnections", 450);
        commentsMetrics.put("moderationRate", "95.2%");
        commentsMetrics.put("averageProcessingTime", "85ms");
        metrics.put("comments", commentsMetrics);
        
        Map<String, Object> systemMetrics = new HashMap<>();
        systemMetrics.put("jvmMemoryUsed", "512MB");
        systemMetrics.put("jvmMemoryMax", "2GB");
        systemMetrics.put("cpuUsage", "35%");
        systemMetrics.put("activeThreads", 120);
        systemMetrics.put("uptime", "5 days 12 hours");
        metrics.put("system", systemMetrics);
        
        return Mono.just(metrics);
    }

    // ========== 管理员端点 ==========
    
    /**
     * 获取朋友圈分页数据
     */
    @GetMapping("/moments/page")
    public Mono<Map<String, Object>> getMomentsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam Map<String, Object> params) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 模拟朋友圈数据
        List<Map<String, Object>> moments = Arrays.asList(
            createMomentData("1", "user1", "今天天气真好！", "public", LocalDateTime.now()),
            createMomentData("2", "user2", "和朋友们一起吃饭", "friends", LocalDateTime.now().minusHours(2)),
            createMomentData("3", "user3", "工作中的小确幸", "public", LocalDateTime.now().minusHours(5))
        );
        
        Map<String, Object> pageData = new HashMap<>();
        pageData.put("content", moments);
        pageData.put("totalElements", 156);
        pageData.put("totalPages", 8);
        pageData.put("size", size);
        pageData.put("number", page);
        
        response.put("code", 200);
        response.put("message", "Success");
        response.put("data", pageData);
        response.put("timestamp", LocalDateTime.now());
        
        return Mono.just(response);
    }
    
    /**
     * 获取点赞分页数据
     */
    @GetMapping("/likes/page")
    public Mono<Map<String, Object>> getLikesPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam Map<String, Object> params) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 模拟点赞数据
        List<Map<String, Object>> likes = Arrays.asList(
            createLikeData("1", "user1", "moment", "moment1", LocalDateTime.now()),
            createLikeData("2", "user2", "comment", "comment1", LocalDateTime.now().minusHours(1)),
            createLikeData("3", "user3", "moment", "moment2", LocalDateTime.now().minusHours(2))
        );
        
        Map<String, Object> pageData = new HashMap<>();
        pageData.put("content", likes);
        pageData.put("totalElements", 12450);
        pageData.put("totalPages", 623);
        pageData.put("size", size);
        pageData.put("number", page);
        
        response.put("code", 200);
        response.put("message", "Success");
        response.put("data", pageData);
        response.put("timestamp", LocalDateTime.now());
        
        return Mono.just(response);
    }
    
    /**
     * 获取评论分页数据
     */
    @GetMapping("/comments/page")
    public Mono<Map<String, Object>> getCommentsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam Map<String, Object> params) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 模拟评论数据
        List<Map<String, Object>> comments = Arrays.asList(
            createCommentData("1", "user1", "这是一个很棒的分享！", "moment1", LocalDateTime.now()),
            createCommentData("2", "user2", "同意你的观点", "moment1", LocalDateTime.now().minusMinutes(30)),
            createCommentData("3", "user3", "很有意思的内容", "moment2", LocalDateTime.now().minusHours(1))
        );
        
        Map<String, Object> pageData = new HashMap<>();
        pageData.put("content", comments);
        pageData.put("totalElements", 8920);
        pageData.put("totalPages", 446);
        pageData.put("size", size);
        pageData.put("number", page);
        
        response.put("code", 200);
        response.put("message", "Success");
        response.put("data", pageData);
        response.put("timestamp", LocalDateTime.now());
        
        return Mono.just(response);
    }
    
    /**
     * 获取审核待处理分页数据
     */
    @GetMapping("/moderation/pending/page")
    public Mono<Map<String, Object>> getModerationPendingPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam Map<String, Object> params) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 模拟审核数据
        List<Map<String, Object>> pending = Arrays.asList(
            createModerationData("1", "comment", "待审核的评论内容", "pending", LocalDateTime.now()),
            createModerationData("2", "moment", "待审核的朋友圈内容", "pending", LocalDateTime.now().minusMinutes(15))
        );
        
        Map<String, Object> pageData = new HashMap<>();
        pageData.put("content", pending);
        pageData.put("totalElements", 45);
        pageData.put("totalPages", 3);
        pageData.put("size", size);
        pageData.put("number", page);
        
        response.put("code", 200);
        response.put("message", "Success");
        response.put("data", pageData);
        response.put("timestamp", LocalDateTime.now());
        
        return Mono.just(response);
    }
    
    // ========== 辅助方法 ==========
    
    private Map<String, Object> createMomentData(String id, String userId, String content, String visibility, LocalDateTime createdAt) {
        Map<String, Object> moment = new HashMap<>();
        moment.put("id", id);
        moment.put("userId", userId);
        moment.put("content", content);
        moment.put("visibility", visibility);
        moment.put("createdAt", createdAt);
        moment.put("likesCount", new Random().nextInt(50));
        moment.put("commentsCount", new Random().nextInt(20));
        moment.put("hasAttachments", false);
        return moment;
    }
    
    private Map<String, Object> createLikeData(String id, String userId, String targetType, String targetId, LocalDateTime createdAt) {
        Map<String, Object> like = new HashMap<>();
        like.put("id", id);
        like.put("userId", userId);
        like.put("targetType", targetType);
        like.put("targetId", targetId);
        like.put("createdAt", createdAt);
        like.put("deviceType", "web");
        like.put("ipAddress", "192.168.1.100");
        return like;
    }
    
    private Map<String, Object> createCommentData(String id, String userId, String content, String targetId, LocalDateTime createdAt) {
        Map<String, Object> comment = new HashMap<>();
        comment.put("id", id);
        comment.put("userId", userId);
        comment.put("username", "用户" + userId);
        comment.put("content", content);
        comment.put("targetId", targetId);
        comment.put("status", "approved");
        comment.put("createdAt", createdAt);
        comment.put("likesCount", new Random().nextInt(10));
        return comment;
    }
    
    private Map<String, Object> createModerationData(String id, String contentType, String content, String status, LocalDateTime createdAt) {
        Map<String, Object> moderation = new HashMap<>();
        moderation.put("id", id);
        moderation.put("contentType", contentType);
        moderation.put("content", content);
        moderation.put("status", status);
        moderation.put("priority", "medium");
        moderation.put("score", 0.65);
        moderation.put("createdAt", createdAt);
        moderation.put("flags", Arrays.asList("spam", "inappropriate"));
        return moderation;
    }
}