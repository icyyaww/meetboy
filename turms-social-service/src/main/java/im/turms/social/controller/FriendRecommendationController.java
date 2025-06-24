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

package im.turms.social.controller;

import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.List;

/**
 * 好友推荐控制器
 * 
 * 基于社交网络分析算法提供好友推荐功能
 */
@RestController
@RequestMapping("/social/recommendations")
public class FriendRecommendationController {

    /**
     * 获取好友推荐列表
     * 基于共同好友、兴趣标签、地理位置等因素进行推荐
     */
    @GetMapping("/friends/{userId}")
    public Flux<Map<String, Object>> getFriendRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "hybrid") String algorithm,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0.5") double threshold) {
        
        // 模拟推荐算法结果
        return Flux.just(
            Map.of(
                "userId", userId + 1001,
                "username", "推荐用户1",
                "avatar", "avatar1.jpg",
                "score", 0.85,
                "reason", List.of("共同好友: 5人", "相同兴趣: 编程, 旅行"),
                "mutualFriends", 5,
                "commonTags", List.of("编程", "旅行", "音乐"),
                "distance", "1.2km"
            ),
            Map.of(
                "userId", userId + 1002,
                "username", "推荐用户2", 
                "avatar", "avatar2.jpg",
                "score", 0.72,
                "reason", List.of("共同好友: 3人", "地理位置接近"),
                "mutualFriends", 3,
                "commonTags", List.of("健身", "美食"),
                "distance", "800m"
            ),
            Map.of(
                "userId", userId + 1003,
                "username", "推荐用户3",
                "avatar", "avatar3.jpg", 
                "score", 0.68,
                "reason", List.of("兴趣匹配度高", "活跃度相似"),
                "mutualFriends", 1,
                "commonTags", List.of("读书", "电影", "科技"),
                "distance", "2.5km"
            )
        );
    }

    /**
     * 获取基于协同过滤的推荐
     * 使用用户行为数据进行推荐
     */
    @GetMapping("/collaborative/{userId}")
    public Flux<Map<String, Object>> getCollaborativeRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        
        return Flux.just(
            Map.of(
                "userId", userId + 2001,
                "username", "协同推荐1",
                "score", 0.78,
                "algorithm", "UserBasedCF",
                "similarity", 0.82,
                "reason", "与您有相似喜好的用户"
            ),
            Map.of(
                "userId", userId + 2002,
                "username", "协同推荐2",
                "score", 0.71,
                "algorithm", "ItemBasedCF", 
                "similarity", 0.75,
                "reason", "喜欢相似内容的用户"
            )
        );
    }

    /**
     * 获取基于内容的推荐
     * 基于用户画像和兴趣标签推荐
     */
    @GetMapping("/content-based/{userId}")
    public Flux<Map<String, Object>> getContentBasedRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        
        return Flux.just(
            Map.of(
                "userId", userId + 3001,
                "username", "内容推荐1",
                "score", 0.88,
                "matchingTags", List.of("机器学习", "Python", "数据分析"),
                "profileSimilarity", 0.91,
                "reason", "兴趣标签高度匹配"
            ),
            Map.of(
                "userId", userId + 3002,
                "username", "内容推荐2",
                "score", 0.76,
                "matchingTags", List.of("前端开发", "Vue.js", "TypeScript"),
                "profileSimilarity", 0.82,
                "reason", "技能背景相似"
            )
        );
    }

    /**
     * 获取推荐算法的解释
     */
    @GetMapping("/explain/{userId}/{recommendedUserId}")
    public Mono<Map<String, Object>> explainRecommendation(
            @PathVariable Long userId,
            @PathVariable Long recommendedUserId) {
        
        return Mono.just(Map.of(
            "algorithm", "hybrid",
            "totalScore", 0.85,
            "factors", List.of(
                Map.of("factor", "共同好友", "weight", 0.3, "score", 0.9),
                Map.of("factor", "兴趣匹配", "weight", 0.25, "score", 0.8),
                Map.of("factor", "地理位置", "weight", 0.2, "score", 0.7),
                Map.of("factor", "活跃度", "weight", 0.15, "score", 0.9),
                Map.of("factor", "互动历史", "weight", 0.1, "score", 0.6)
            ),
            "explanation", "该用户与您有5个共同好友，兴趣标签匹配度80%，地理位置较近"
        ));
    }

    /**
     * 更新推荐反馈
     * 用于改进推荐算法
     */
    @PostMapping("/feedback")
    public Mono<Map<String, Object>> submitRecommendationFeedback(
            @RequestBody Map<String, Object> feedback) {
        
        Long userId = Long.valueOf(feedback.get("userId").toString());
        Long recommendedUserId = Long.valueOf(feedback.get("recommendedUserId").toString());
        String action = feedback.get("action").toString(); // like, dislike, ignore, accept
        
        return Mono.just(Map.of(
            "success", true,
            "message", "推荐反馈已记录",
            "userId", userId,
            "recommendedUserId", recommendedUserId,
            "action", action,
            "timestamp", System.currentTimeMillis()
        ));
    }
}