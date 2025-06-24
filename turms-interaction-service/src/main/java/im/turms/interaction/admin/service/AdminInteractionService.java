package im.turms.interaction.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员交互服务
 * 为Admin UI提供数据管理功能
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminInteractionService {
    
    // ========== 点赞管理 ==========
    
    public Mono<List<Map<String, Object>>> getLikes(Map<String, Object> params) {
        log.debug("Getting likes with params: {}", params);
        
        // 模拟数据，实际应该从数据库查询
        List<Map<String, Object>> likes = Arrays.asList(
            createLikeMockData("1", "user1", "moment", "moment1", LocalDateTime.now()),
            createLikeMockData("2", "user2", "comment", "comment1", LocalDateTime.now().minusHours(1)),
            createLikeMockData("3", "user3", "moment", "moment2", LocalDateTime.now().minusHours(2))
        );
        
        return Mono.just(likes);
    }
    
    public Mono<Page<Map<String, Object>>> getLikesPage(Map<String, Object> params, Pageable pageable) {
        log.debug("Getting likes page with params: {}, pageable: {}", params, pageable);
        
        return getLikes(params)
                .map(likes -> {
                    int start = (int) pageable.getOffset();
                    int end = Math.min(start + pageable.getPageSize(), likes.size());
                    List<Map<String, Object>> content = likes.subList(start, end);
                    return new PageImpl<>(content, pageable, likes.size());
                });
    }
    
    public Mono<Map<String, Object>> createLike(Map<String, Object> data) {
        log.debug("Creating like with data: {}", data);
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", UUID.randomUUID().toString());
        result.put("userId", data.get("userId"));
        result.put("targetType", data.get("targetType"));
        result.put("targetId", data.get("targetId"));
        result.put("createdAt", LocalDateTime.now());
        result.put("success", true);
        
        return Mono.just(result);
    }
    
    public Mono<Map<String, Object>> updateLike(String id, Map<String, Object> data) {
        log.debug("Updating like {} with data: {}", id, data);
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("updatedAt", LocalDateTime.now());
        result.put("success", true);
        
        return Mono.just(result);
    }
    
    public Mono<Map<String, Object>> deleteLikes(List<String> ids) {
        log.debug("Deleting likes with ids: {}", ids);
        
        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", ids.size());
        result.put("success", true);
        
        return Mono.just(result);
    }
    
    public Mono<Map<String, Object>> getLikeStats(Map<String, Object> params) {
        log.debug("Getting like stats with params: {}", params);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLikes", 12450);
        stats.put("todayLikes", 156);
        stats.put("likesGrowthRate", 12.5);
        stats.put("avgLikesPerContent", 3.8);
        stats.put("topLikedContent", Arrays.asList(
            Map.of("contentId", "moment1", "likesCount", 245, "title", "精彩瞬间"),
            Map.of("contentId", "moment2", "likesCount", 189, "title", "美好时光")
        ));
        
        return Mono.just(stats);
    }
    
    // ========== 评论管理 ==========
    
    public Mono<List<Map<String, Object>>> getComments(Map<String, Object> params) {
        log.debug("Getting comments with params: {}", params);
        
        List<Map<String, Object>> comments = Arrays.asList(
            createCommentMockData("1", "user1", "这是一个很棒的分享！", "moment1", LocalDateTime.now()),
            createCommentMockData("2", "user2", "同意你的观点", "moment1", LocalDateTime.now().minusMinutes(30)),
            createCommentMockData("3", "user3", "很有意思的内容", "moment2", LocalDateTime.now().minusHours(1))
        );
        
        return Mono.just(comments);
    }
    
    public Mono<Page<Map<String, Object>>> getCommentsPage(Map<String, Object> params, Pageable pageable) {
        log.debug("Getting comments page with params: {}, pageable: {}", params, pageable);
        
        return getComments(params)
                .map(comments -> {
                    int start = (int) pageable.getOffset();
                    int end = Math.min(start + pageable.getPageSize(), comments.size());
                    List<Map<String, Object>> content = comments.subList(start, end);
                    return new PageImpl<>(content, pageable, comments.size());
                });
    }
    
    public Mono<Map<String, Object>> createComment(Map<String, Object> data) {
        log.debug("Creating comment with data: {}", data);
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", UUID.randomUUID().toString());
        result.put("userId", data.get("userId"));
        result.put("content", data.get("content"));
        result.put("targetId", data.get("targetId"));
        result.put("createdAt", LocalDateTime.now());
        result.put("status", "approved");
        result.put("success", true);
        
        return Mono.just(result);
    }
    
    public Mono<Map<String, Object>> updateComment(String id, Map<String, Object> data) {
        log.debug("Updating comment {} with data: {}", id, data);
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("updatedAt", LocalDateTime.now());
        result.put("success", true);
        
        return Mono.just(result);
    }
    
    public Mono<Map<String, Object>> deleteComments(List<String> ids) {
        log.debug("Deleting comments with ids: {}", ids);
        
        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", ids.size());
        result.put("success", true);
        
        return Mono.just(result);
    }
    
    // ========== 朋友圈管理 ==========
    
    public Mono<List<Map<String, Object>>> getMoments(Map<String, Object> params) {
        log.debug("Getting moments with params: {}", params);
        
        List<Map<String, Object>> moments = Arrays.asList(
            createMomentMockData("1", "user1", "今天天气真好！", "public", LocalDateTime.now()),
            createMomentMockData("2", "user2", "和朋友们一起吃饭", "friends", LocalDateTime.now().minusHours(2)),
            createMomentMockData("3", "user3", "工作中的小确幸", "public", LocalDateTime.now().minusHours(5))
        );
        
        return Mono.just(moments);
    }
    
    public Mono<Page<Map<String, Object>>> getMomentsPage(Map<String, Object> params, Pageable pageable) {
        log.debug("Getting moments page with params: {}, pageable: {}", params, pageable);
        
        return getMoments(params)
                .map(moments -> {
                    int start = (int) pageable.getOffset();
                    int end = Math.min(start + pageable.getPageSize(), moments.size());
                    List<Map<String, Object>> content = moments.subList(start, end);
                    return new PageImpl<>(content, pageable, moments.size());
                });
    }
    
    public Mono<Map<String, Object>> createMoment(Map<String, Object> data) {
        log.debug("Creating moment with data: {}", data);
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", UUID.randomUUID().toString());
        result.put("userId", data.get("userId"));
        result.put("content", data.get("content"));
        result.put("visibility", data.getOrDefault("visibility", "public"));
        result.put("createdAt", LocalDateTime.now());
        result.put("likesCount", 0);
        result.put("commentsCount", 0);
        result.put("success", true);
        
        return Mono.just(result);
    }
    
    public Mono<Map<String, Object>> updateMoment(String id, Map<String, Object> data) {
        log.debug("Updating moment {} with data: {}", id, data);
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("updatedAt", LocalDateTime.now());
        result.put("success", true);
        
        return Mono.just(result);
    }
    
    public Mono<Map<String, Object>> deleteMoments(List<String> ids) {
        log.debug("Deleting moments with ids: {}", ids);
        
        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", ids.size());
        result.put("success", true);
        
        return Mono.just(result);
    }
    
    // ========== 审核管理 ==========
    
    public Mono<List<Map<String, Object>>> getModerationPending(Map<String, Object> params) {
        log.debug("Getting moderation pending with params: {}", params);
        
        List<Map<String, Object>> pending = Arrays.asList(
            createModerationMockData("1", "comment", "待审核的评论内容", "pending", LocalDateTime.now()),
            createModerationMockData("2", "moment", "待审核的朋友圈内容", "pending", LocalDateTime.now().minusMinutes(15))
        );
        
        return Mono.just(pending);
    }
    
    public Mono<Page<Map<String, Object>>> getModerationPendingPage(Map<String, Object> params, Pageable pageable) {
        log.debug("Getting moderation pending page with params: {}, pageable: {}", params, pageable);
        
        return getModerationPending(params)
                .map(pending -> {
                    int start = (int) pageable.getOffset();
                    int end = Math.min(start + pageable.getPageSize(), pending.size());
                    List<Map<String, Object>> content = pending.subList(start, end);
                    return new PageImpl<>(content, pageable, pending.size());
                });
    }
    
    public Mono<Map<String, Object>> approveModerationContent(Map<String, Object> data) {
        log.debug("Approving moderation content with data: {}", data);
        
        Map<String, Object> result = new HashMap<>();
        result.put("approvedCount", ((List<?>) data.get("ids")).size());
        result.put("reason", data.get("reason"));
        result.put("success", true);
        
        return Mono.just(result);
    }
    
    public Mono<Map<String, Object>> rejectModerationContent(Map<String, Object> data) {
        log.debug("Rejecting moderation content with data: {}", data);
        
        Map<String, Object> result = new HashMap<>();
        result.put("rejectedCount", ((List<?>) data.get("ids")).size());
        result.put("reason", data.get("reason"));
        result.put("success", true);
        
        return Mono.just(result);
    }
    
    // ========== 审核规则管理 ==========
    
    public Mono<List<Map<String, Object>>> getModerationRules(Map<String, Object> params) {
        log.debug("Getting moderation rules with params: {}", params);
        
        List<Map<String, Object>> rules = Arrays.asList(
            createModerationRuleMockData("1", "垃圾内容过滤", "spam_filter", true, 0.8, LocalDateTime.now().minusDays(7)),
            createModerationRuleMockData("2", "不当言论检测", "inappropriate_content", true, 0.75, LocalDateTime.now().minusDays(3)),
            createModerationRuleMockData("3", "敏感信息过滤", "sensitive_info", false, 0.9, LocalDateTime.now().minusDays(1))
        );
        
        return Mono.just(rules);
    }
    
    public Mono<Page<Map<String, Object>>> getModerationRulesPage(Map<String, Object> params, Pageable pageable) {
        log.debug("Getting moderation rules page with params: {}, pageable: {}", params, pageable);
        
        return getModerationRules(params)
                .map(rules -> {
                    int start = (int) pageable.getOffset();
                    int end = Math.min(start + pageable.getPageSize(), rules.size());
                    List<Map<String, Object>> content = rules.subList(start, end);
                    return new PageImpl<>(content, pageable, rules.size());
                });
    }
    
    public Mono<Map<String, Object>> createModerationRule(Map<String, Object> data) {
        log.debug("Creating moderation rule with data: {}", data);
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", UUID.randomUUID().toString());
        result.put("name", data.get("name"));
        result.put("type", data.get("type"));
        result.put("enabled", data.getOrDefault("enabled", true));
        result.put("threshold", data.getOrDefault("threshold", 0.8));
        result.put("createdAt", LocalDateTime.now());
        result.put("success", true);
        
        return Mono.just(result);
    }
    
    public Mono<Map<String, Object>> updateModerationRule(String id, Map<String, Object> data) {
        log.debug("Updating moderation rule {} with data: {}", id, data);
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("updatedAt", LocalDateTime.now());
        result.put("success", true);
        
        return Mono.just(result);
    }
    
    public Mono<Map<String, Object>> deleteModerationRules(List<String> ids) {
        log.debug("Deleting moderation rules with ids: {}", ids);
        
        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", ids.size());
        result.put("success", true);
        
        return Mono.just(result);
    }
    
    public Mono<Map<String, Object>> enableModerationRule(String id) {
        log.debug("Enabling moderation rule: {}", id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("enabled", true);
        result.put("updatedAt", LocalDateTime.now());
        result.put("success", true);
        
        return Mono.just(result);
    }
    
    public Mono<Map<String, Object>> disableModerationRule(String id) {
        log.debug("Disabling moderation rule: {}", id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("enabled", false);
        result.put("updatedAt", LocalDateTime.now());
        result.put("success", true);
        
        return Mono.just(result);
    }
    
    // ========== 审核日志管理 ==========
    
    public Mono<List<Map<String, Object>>> getModerationLogs(Map<String, Object> params) {
        log.debug("Getting moderation logs with params: {}", params);
        
        List<Map<String, Object>> logs = Arrays.asList(
            createModerationLogMockData("1", "comment_123", "approve", "admin1", "内容符合规范", LocalDateTime.now()),
            createModerationLogMockData("2", "moment_456", "reject", "admin2", "包含不当内容", LocalDateTime.now().minusMinutes(30)),
            createModerationLogMockData("3", "comment_789", "approve", "admin1", "自动审核通过", LocalDateTime.now().minusHours(1))
        );
        
        return Mono.just(logs);
    }
    
    public Mono<Page<Map<String, Object>>> getModerationLogsPage(Map<String, Object> params, Pageable pageable) {
        log.debug("Getting moderation logs page with params: {}, pageable: {}", params, pageable);
        
        return getModerationLogs(params)
                .map(logs -> {
                    int start = (int) pageable.getOffset();
                    int end = Math.min(start + pageable.getPageSize(), logs.size());
                    List<Map<String, Object>> content = logs.subList(start, end);
                    return new PageImpl<>(content, pageable, logs.size());
                });
    }
    
    // ========== 审核统计 ==========
    
    public Mono<Map<String, Object>> getModerationStats(Map<String, Object> params) {
        log.debug("Getting moderation stats with params: {}", params);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalReviewed", 1256);
        stats.put("approved", 1089);
        stats.put("rejected", 167);
        stats.put("pending", 45);
        stats.put("approvalRate", 86.7);
        stats.put("avgReviewTime", 4.2); // minutes
        stats.put("todayReviewed", 78);
        stats.put("todayApproved", 68);
        stats.put("todayRejected", 10);
        
        return Mono.just(stats);
    }
    
    public Mono<Map<String, Object>> getModerationTrend(Map<String, Object> params) {
        log.debug("Getting moderation trend with params: {}", params);
        
        Map<String, Object> trend = new HashMap<>();
        
        // 最近7天的审核趋势数据
        List<Map<String, Object>> dailyTrend = Arrays.asList(
            Map.of("date", "2025-06-17", "reviewed", 145, "approved", 126, "rejected", 19),
            Map.of("date", "2025-06-18", "reviewed", 167, "approved", 142, "rejected", 25),
            Map.of("date", "2025-06-19", "reviewed", 134, "approved", 118, "rejected", 16),
            Map.of("date", "2025-06-20", "reviewed", 189, "approved", 161, "rejected", 28),
            Map.of("date", "2025-06-21", "reviewed", 156, "approved", 135, "rejected", 21),
            Map.of("date", "2025-06-22", "reviewed", 178, "approved", 154, "rejected", 24),
            Map.of("date", "2025-06-23", "reviewed", 78, "approved", 68, "rejected", 10)
        );
        
        trend.put("dailyTrend", dailyTrend);
        trend.put("weeklyGrowth", 12.5);
        trend.put("avgDailyReviewed", 155);
        
        return Mono.just(trend);
    }
    
    // ========== 系统监控 ==========
    
    public Mono<Map<String, Object>> getHealth() {
        log.debug("Getting interaction health");
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("details", Map.of(
            "database", Map.of("status", "UP", "responseTime", "12ms"),
            "redis", Map.of("status", "UP", "responseTime", "3ms"),
            "mongodb", Map.of("status", "UP", "responseTime", "8ms"),
            "kafka", Map.of("status", "UP", "responseTime", "15ms")
        ));
        health.put("uptime", "2d 14h 23m");
        health.put("timestamp", LocalDateTime.now());
        
        return Mono.just(health);
    }
    
    public Mono<Map<String, Object>> getMetrics() {
        log.debug("Getting interaction metrics");
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("requests", Map.of(
            "total", 1234567,
            "successful", 1220145,
            "failed", 14422,
            "rate", 156.8
        ));
        metrics.put("interactions", Map.of(
            "likes", 45623,
            "comments", 12890,
            "moments", 8765
        ));
        metrics.put("performance", Map.of(
            "avgResponseTime", 89.5,
            "p95ResponseTime", 245.2,
            "p99ResponseTime", 456.8
        ));
        
        return Mono.just(metrics);
    }
    
    public Mono<Map<String, Object>> getPerformanceMetrics(Map<String, Object> params) {
        log.debug("Getting performance metrics with params: {}", params);
        
        Map<String, Object> performance = new HashMap<>();
        performance.put("cpu", Map.of("usage", 45.2, "cores", 8));
        performance.put("memory", Map.of("used", 512, "total", 1024, "unit", "MB"));
        performance.put("jvm", Map.of(
            "heapUsed", 256,
            "heapMax", 512,
            "nonHeapUsed", 128,
            "gcCount", 145,
            "gcTime", 1250
        ));
        
        return Mono.just(performance);
    }
    
    public Mono<Map<String, Object>> getDatabaseMetrics() {
        log.debug("Getting database metrics");
        
        Map<String, Object> dbMetrics = new HashMap<>();
        dbMetrics.put("connections", Map.of("active", 8, "idle", 12, "max", 20));
        dbMetrics.put("queries", Map.of("total", 45123, "slow", 23, "avgTime", 12.5));
        dbMetrics.put("size", Map.of("total", "2.5GB", "indexes", "512MB"));
        
        return Mono.just(dbMetrics);
    }
    
    public Mono<Map<String, Object>> getCacheMetrics() {
        log.debug("Getting cache metrics");
        
        Map<String, Object> cacheMetrics = new HashMap<>();
        cacheMetrics.put("hits", 123456);
        cacheMetrics.put("misses", 8920);
        cacheMetrics.put("hitRate", 93.3);
        cacheMetrics.put("memory", Map.of("used", "128MB", "max", "256MB"));
        
        return Mono.just(cacheMetrics);
    }
    
    // ========== 辅助方法 ==========
    
    private Map<String, Object> createLikeMockData(String id, String userId, String targetType, String targetId, LocalDateTime createdAt) {
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
    
    private Map<String, Object> createCommentMockData(String id, String userId, String content, String targetId, LocalDateTime createdAt) {
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
    
    private Map<String, Object> createMomentMockData(String id, String userId, String content, String visibility, LocalDateTime createdAt) {
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
    
    private Map<String, Object> createModerationMockData(String id, String contentType, String content, String status, LocalDateTime createdAt) {
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
    
    private Map<String, Object> createModerationRuleMockData(String id, String name, String type, boolean enabled, double threshold, LocalDateTime createdAt) {
        Map<String, Object> rule = new HashMap<>();
        rule.put("id", id);
        rule.put("name", name);
        rule.put("type", type);
        rule.put("enabled", enabled);
        rule.put("threshold", threshold);
        rule.put("createdAt", createdAt);
        rule.put("description", "自动" + name + "规则");
        rule.put("triggeredCount", new Random().nextInt(100));
        rule.put("accuracy", 0.85 + new Random().nextDouble() * 0.1);
        return rule;
    }
    
    private Map<String, Object> createModerationLogMockData(String id, String contentId, String action, String operator, String reason, LocalDateTime createdAt) {
        Map<String, Object> log = new HashMap<>();
        log.put("id", id);
        log.put("contentId", contentId);
        log.put("action", action);
        log.put("operator", operator);
        log.put("reason", reason);
        log.put("createdAt", createdAt);
        log.put("contentType", contentId.startsWith("comment") ? "comment" : "moment");
        log.put("automated", operator.equals("system"));
        return log;
    }
}