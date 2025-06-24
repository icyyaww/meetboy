package im.turms.interaction.admin.controller;

import im.turms.interaction.admin.service.AdminInteractionService;
import im.turms.interaction.common.response.ResponseTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * 管理员互动服务控制器
 * 提供给Admin UI使用的管理接口
 */
@RestController
@RequestMapping("/interaction/admin")
@CrossOrigin(
    originPatterns = {"http://localhost:*", "http://127.0.0.1:*"}, 
    allowedHeaders = "*", 
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
    allowCredentials = "true"
)
@RequiredArgsConstructor
@Slf4j
public class AdminInteractionController {
    
    private final AdminInteractionService adminInteractionService;
    
    // ========== 点赞管理 API ==========
    
    @GetMapping("/likes")
    public Mono<ResponseEntity<Object>> fetchLikes(@RequestParam Map<String, Object> params) {
        log.debug("Fetching likes with params: {}", params);
        return adminInteractionService.getLikes(params)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to fetch likes"));
    }
    
    @GetMapping("/likes/page")
    public Mono<Map<String, Object>> fetchLikesPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam Map<String, Object> params) {
        log.debug("Fetching likes page with params: {}, page: {}, size: {}", params, page, size);
        
        // Return mock data for testing
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> records = new ArrayList<>();
        
        // Create mock like records
        for (int i = 0; i < size; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", page * size + i + 1);
            record.put("userId", 1000L + i);
            record.put("targetType", "MOMENT");
            record.put("targetId", "moment" + (i + 1));
            record.put("createdAt", "2025-06-23T15:16:00Z");
            record.put("userInfo", Map.of("id", 1000L + i, "name", "User" + (i + 1)));
            records.add(record);
        }
        
        response.put("records", records);
        response.put("total", 12450L);
        response.put("page", page);
        response.put("size", size);
        
        return Mono.just(response);
    }
    
    @PostMapping("/likes")
    public Mono<ResponseEntity<Object>> createLike(@RequestBody Map<String, Object> data) {
        log.debug("Creating like with data: {}", data);
        return adminInteractionService.createLike(data)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to create like"));
    }
    
    @PutMapping("/likes/{id}")
    public Mono<ResponseEntity<Object>> updateLike(@PathVariable String id, @RequestBody Map<String, Object> data) {
        log.debug("Updating like {} with data: {}", id, data);
        return adminInteractionService.updateLike(id, data)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to update like"));
    }
    
    @DeleteMapping("/likes")
    public Mono<ResponseEntity<Object>> deleteLikes(@RequestParam List<String> ids) {
        log.debug("Deleting likes with ids: {}", ids);
        return adminInteractionService.deleteLikes(ids)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to delete likes"));
    }
    
    @GetMapping("/likes/stats")
    public Mono<ResponseEntity<Object>> fetchLikeStats(@RequestParam Map<String, Object> params) {
        log.debug("Fetching like stats with params: {}", params);
        return adminInteractionService.getLikeStats(params)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to fetch like stats"));
    }
    
    // ========== 评论管理 API ==========
    
    @GetMapping("/comments")
    public Mono<ResponseEntity<Object>> fetchComments(@RequestParam Map<String, Object> params) {
        log.debug("Fetching comments with params: {}", params);
        return adminInteractionService.getComments(params)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to fetch comments"));
    }
    
    @GetMapping("/comments/page")
    public Mono<ResponseEntity<Object>> fetchCommentsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam Map<String, Object> params) {
        log.debug("Fetching comments page with params: {}, page: {}, size: {}", params, page, size);
        Pageable pageable = PageRequest.of(page, size);
        return adminInteractionService.getCommentsPage(params, pageable)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to fetch comments page"));
    }
    
    @PostMapping("/comments")
    public Mono<ResponseEntity<Object>> createComment(@RequestBody Map<String, Object> data) {
        log.debug("Creating comment with data: {}", data);
        return adminInteractionService.createComment(data)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to create comment"));
    }
    
    @PutMapping("/comments/{id}")
    public Mono<ResponseEntity<Object>> updateComment(@PathVariable String id, @RequestBody Map<String, Object> data) {
        log.debug("Updating comment {} with data: {}", id, data);
        return adminInteractionService.updateComment(id, data)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to update comment"));
    }
    
    @DeleteMapping("/comments")
    public Mono<ResponseEntity<Object>> deleteComments(@RequestParam List<String> ids) {
        log.debug("Deleting comments with ids: {}", ids);
        return adminInteractionService.deleteComments(ids)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to delete comments"));
    }
    
    // ========== 朋友圈管理 API ==========
    
    @GetMapping("/moments")
    public Mono<ResponseEntity<Object>> fetchMoments(@RequestParam Map<String, Object> params) {
        log.debug("Fetching moments with params: {}", params);
        return adminInteractionService.getMoments(params)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to fetch moments"));
    }
    
    @GetMapping("/moments/page")
    public Mono<ResponseEntity<Object>> fetchMomentsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam Map<String, Object> params) {
        log.debug("Fetching moments page with params: {}, page: {}, size: {}", params, page, size);
        Pageable pageable = PageRequest.of(page, size);
        return adminInteractionService.getMomentsPage(params, pageable)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to fetch moments page"));
    }
    
    @PostMapping("/moments")
    public Mono<ResponseEntity<Object>> createMoment(@RequestBody Map<String, Object> data) {
        log.debug("Creating moment with data: {}", data);
        return adminInteractionService.createMoment(data)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to create moment"));
    }
    
    @PutMapping("/moments/{id}")
    public Mono<ResponseEntity<Object>> updateMoment(@PathVariable String id, @RequestBody Map<String, Object> data) {
        log.debug("Updating moment {} with data: {}", id, data);
        return adminInteractionService.updateMoment(id, data)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to update moment"));
    }
    
    @DeleteMapping("/moments")
    public Mono<ResponseEntity<Object>> deleteMoments(@RequestParam List<String> ids) {
        log.debug("Deleting moments with ids: {}", ids);
        return adminInteractionService.deleteMoments(ids)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to delete moments"));
    }
    
    // ========== 审核管理 API ==========
    
    @GetMapping("/moderation/pending")
    public Mono<ResponseEntity<Object>> fetchModerationPending(@RequestParam Map<String, Object> params) {
        log.debug("Fetching moderation pending with params: {}", params);
        return adminInteractionService.getModerationPending(params)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to fetch moderation pending"));
    }
    
    @GetMapping("/moderation/pending/page")
    public Mono<ResponseEntity<Object>> fetchModerationPendingPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam Map<String, Object> params) {
        log.debug("Fetching moderation pending page with params: {}, page: {}, size: {}", params, page, size);
        Pageable pageable = PageRequest.of(page, size);
        return adminInteractionService.getModerationPendingPage(params, pageable)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to fetch moderation pending page"));
    }
    
    @PostMapping("/moderation/approve")
    public Mono<ResponseEntity<Object>> approveModerationContent(@RequestBody Map<String, Object> data) {
        log.debug("Approving moderation content with data: {}", data);
        return adminInteractionService.approveModerationContent(data)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to approve moderation content"));
    }
    
    @PostMapping("/moderation/reject")
    public Mono<ResponseEntity<Object>> rejectModerationContent(@RequestBody Map<String, Object> data) {
        log.debug("Rejecting moderation content with data: {}", data);
        return adminInteractionService.rejectModerationContent(data)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to reject moderation content"));
    }
    
    // ========== 审核规则管理 API ==========
    
    @GetMapping("/moderation/rules")
    public Mono<ResponseEntity<Object>> fetchModerationRules(@RequestParam Map<String, Object> params) {
        log.debug("Fetching moderation rules with params: {}", params);
        return adminInteractionService.getModerationRules(params)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to fetch moderation rules"));
    }
    
    @GetMapping("/moderation/rules/page")
    public Mono<ResponseEntity<Object>> fetchModerationRulesPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam Map<String, Object> params) {
        log.debug("Fetching moderation rules page with params: {}, page: {}, size: {}", params, page, size);
        Pageable pageable = PageRequest.of(page, size);
        return adminInteractionService.getModerationRulesPage(params, pageable)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to fetch moderation rules page"));
    }
    
    @PostMapping("/moderation/rules")
    public Mono<ResponseEntity<Object>> createModerationRule(@RequestBody Map<String, Object> data) {
        log.debug("Creating moderation rule with data: {}", data);
        return adminInteractionService.createModerationRule(data)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to create moderation rule"));
    }
    
    @PutMapping("/moderation/rules/{id}")
    public Mono<ResponseEntity<Object>> updateModerationRule(@PathVariable String id, @RequestBody Map<String, Object> data) {
        log.debug("Updating moderation rule {} with data: {}", id, data);
        return adminInteractionService.updateModerationRule(id, data)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to update moderation rule"));
    }
    
    @DeleteMapping("/moderation/rules")
    public Mono<ResponseEntity<Object>> deleteModerationRules(@RequestParam List<String> ids) {
        log.debug("Deleting moderation rules with ids: {}", ids);
        return adminInteractionService.deleteModerationRules(ids)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to delete moderation rules"));
    }
    
    @PostMapping("/moderation/rules/{id}/enable")
    public Mono<ResponseEntity<Object>> enableModerationRule(@PathVariable String id) {
        log.debug("Enabling moderation rule: {}", id);
        return adminInteractionService.enableModerationRule(id)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to enable moderation rule"));
    }
    
    @PostMapping("/moderation/rules/{id}/disable")
    public Mono<ResponseEntity<Object>> disableModerationRule(@PathVariable String id) {
        log.debug("Disabling moderation rule: {}", id);
        return adminInteractionService.disableModerationRule(id)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to disable moderation rule"));
    }
    
    // ========== 审核日志 API ==========
    
    @GetMapping("/moderation/logs")
    public Mono<ResponseEntity<Object>> fetchModerationLogs(@RequestParam Map<String, Object> params) {
        log.debug("Fetching moderation logs with params: {}", params);
        return adminInteractionService.getModerationLogs(params)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to fetch moderation logs"));
    }
    
    @GetMapping("/moderation/logs/page")
    public Mono<ResponseEntity<Object>> fetchModerationLogsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam Map<String, Object> params) {
        log.debug("Fetching moderation logs page with params: {}, page: {}, size: {}", params, page, size);
        Pageable pageable = PageRequest.of(page, size);
        return adminInteractionService.getModerationLogsPage(params, pageable)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to fetch moderation logs page"));
    }
    
    // ========== 审核统计 API ==========
    
    @GetMapping("/moderation/stats")
    public Mono<ResponseEntity<Object>> fetchModerationStats(@RequestParam Map<String, Object> params) {
        log.debug("Fetching moderation stats with params: {}", params);
        return adminInteractionService.getModerationStats(params)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to fetch moderation stats"));
    }
    
    @GetMapping("/moderation/trend")
    public Mono<ResponseEntity<Object>> fetchModerationTrend(@RequestParam Map<String, Object> params) {
        log.debug("Fetching moderation trend with params: {}", params);
        return adminInteractionService.getModerationTrend(params)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to fetch moderation trend"));
    }
    
    // ========== 系统监控 API ==========
    
    @GetMapping("/health")
    public Mono<ResponseEntity<Object>> fetchInteractionHealth() {
        log.debug("Fetching interaction health");
        return adminInteractionService.getHealth()
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to fetch health"));
    }
    
    @GetMapping("/metrics")
    public Mono<ResponseEntity<Object>> fetchInteractionMetrics() {
        log.debug("Fetching interaction metrics");
        return adminInteractionService.getMetrics()
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to fetch metrics"));
    }
    
    // ========== 性能监控 API ==========
    
    @GetMapping("/performance/metrics")
    public Mono<ResponseEntity<Object>> fetchPerformanceMetrics(@RequestParam Map<String, Object> params) {
        log.debug("Fetching performance metrics with params: {}", params);
        return adminInteractionService.getPerformanceMetrics(params)
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to fetch performance metrics"));
    }
    
    @GetMapping("/performance/database")
    public Mono<ResponseEntity<Object>> fetchDatabaseMetrics() {
        log.debug("Fetching database metrics");
        return adminInteractionService.getDatabaseMetrics()
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to fetch database metrics"));
    }
    
    @GetMapping("/performance/cache")
    public Mono<ResponseEntity<Object>> fetchCacheMetrics() {
        log.debug("Fetching cache metrics");
        return adminInteractionService.getCacheMetrics()
                .map(ResponseTemplate::ok)
                .onErrorReturn(ResponseTemplate.error("Failed to fetch cache metrics"));
    }
}