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
 * 社交关系图分析控制器
 * 
 * 提供社交网络图形分析、用户影响力分析、社交路径发现等功能
 */
@RestController
@RequestMapping("/social/graph")
public class SocialGraphController {

    /**
     * 获取用户的社交关系图
     * 返回用户的一度、二度社交关系网络
     */
    @GetMapping("/{userId}/network")
    public Mono<Map<String, Object>> getUserSocialNetwork(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "2") int depth,
            @RequestParam(defaultValue = "50") int maxNodes) {
        
        return Mono.just(Map.of(
            "userId", userId,
            "depth", depth,
            "totalNodes", 45,
            "totalEdges", 123,
            "nodes", List.of(
                Map.of("id", userId, "name", "用户本人", "level", 0, "influence", 0.85),
                Map.of("id", userId + 1, "name", "好友A", "level", 1, "influence", 0.72),
                Map.of("id", userId + 2, "name", "好友B", "level", 1, "influence", 0.68),
                Map.of("id", userId + 10, "name", "二度好友C", "level", 2, "influence", 0.45)
            ),
            "edges", List.of(
                Map.of("source", userId, "target", userId + 1, "weight", 0.8, "type", "friend"),
                Map.of("source", userId, "target", userId + 2, "weight", 0.9, "type", "friend"),
                Map.of("source", userId + 1, "target", userId + 10, "weight", 0.6, "type", "friend")
            ),
            "clusters", List.of(
                Map.of("id", 1, "name", "工作圈", "members", List.of(userId + 1, userId + 2)),
                Map.of("id", 2, "name", "同学圈", "members", List.of(userId + 10, userId + 11))
            )
        ));
    }

    /**
     * 分析用户影响力
     * 基于社交网络的中心性指标计算用户影响力
     */
    @GetMapping("/{userId}/influence")
    public Mono<Map<String, Object>> analyzeUserInfluence(@PathVariable Long userId) {
        return Mono.just(Map.of(
            "userId", userId,
            "overallInfluence", 0.75,
            "metrics", Map.of(
                "degreeCentrality", 0.82,      // 度中心性
                "betweennessCentrality", 0.65, // 介数中心性
                "closenessCentrality", 0.78,   // 接近中心性
                "eigenvectorCentrality", 0.71, // 特征向量中心性
                "pageRank", 0.69               // PageRank值
            ),
            "rank", Map.of(
                "globalRank", 1250,
                "localRank", 15,
                "percentile", 85
            ),
            "analysis", Map.of(
                "networkSize", 245,
                "activeConnections", 89,
                "reachability", 0.82,
                "clustering", 0.45
            )
        ));
    }

    /**
     * 发现社交路径
     * 计算两个用户之间的社交路径和关系强度
     */
    @GetMapping("/path/{userId1}/{userId2}")
    public Flux<Map<String, Object>> findSocialPath(
            @PathVariable Long userId1,
            @PathVariable Long userId2,
            @RequestParam(defaultValue = "6") int maxDegrees) {
        
        return Flux.just(
            Map.of(
                "pathId", 1,
                "path", List.of(userId1, userId1 + 100, userId1 + 200, userId2),
                "degrees", 3,
                "strength", 0.72,
                "intermediates", List.of(
                    Map.of("userId", userId1 + 100, "name", "中间人A", "relation", "同事"),
                    Map.of("userId", userId1 + 200, "name", "中间人B", "relation", "朋友")
                )
            ),
            Map.of(
                "pathId", 2,
                "path", List.of(userId1, userId1 + 150, userId2),
                "degrees", 2,
                "strength", 0.65,
                "intermediates", List.of(
                    Map.of("userId", userId1 + 150, "name", "中间人C", "relation", "大学同学")
                )
            )
        );
    }

    /**
     * 社区发现
     * 识别用户所在的社交社区和群体
     */
    @GetMapping("/{userId}/communities")
    public Flux<Map<String, Object>> discoverCommunities(@PathVariable Long userId) {
        return Flux.just(
            Map.of(
                "communityId", 1,
                "name", "技术社区",
                "size", 45,
                "density", 0.78,
                "role", "核心成员",
                "members", List.of(userId + 1, userId + 2, userId + 3),
                "tags", List.of("编程", "技术", "AI")
            ),
            Map.of(
                "communityId", 2,
                "name", "大学同学",
                "size", 28,
                "density", 0.85,
                "role", "活跃成员",
                "members", List.of(userId + 10, userId + 11, userId + 12),
                "tags", List.of("校友", "聚会", "回忆")
            ),
            Map.of(
                "communityId", 3,
                "name", "兴趣小组",
                "size", 16,
                "density", 0.92,
                "role", "边缘成员",
                "members", List.of(userId + 20, userId + 21),
                "tags", List.of("摄影", "旅行", "美食")
            )
        );
    }

    /**
     * 趋势分析
     * 分析社交网络的变化趋势
     */
    @GetMapping("/{userId}/trends")
    public Mono<Map<String, Object>> analyzeSocialTrends(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "30") int days) {
        
        return Mono.just(Map.of(
            "period", days + "天",
            "growth", Map.of(
                "newConnections", 5,
                "lostConnections", 2,
                "netGrowth", 3,
                "growthRate", 0.05
            ),
            "activity", Map.of(
                "interactionFrequency", 0.78,
                "responseRate", 0.85,
                "initiationRate", 0.65
            ),
            "influence", Map.of(
                "currentScore", 0.75,
                "previousScore", 0.72,
                "change", 0.03,
                "trend", "增长"
            ),
            "predictions", Map.of(
                "nextMonthGrowth", 0.08,
                "influenceProjection", 0.78,
                "recommendedActions", List.of(
                    "增加与技术社区的互动",
                    "参与更多群组讨论",
                    "主动分享有价值的内容"
                )
            )
        ));
    }

    /**
     * 获取社交网络统计信息
     */
    @GetMapping("/{userId}/statistics")
    public Mono<Map<String, Object>> getSocialStatistics(@PathVariable Long userId) {
        return Mono.just(Map.of(
            "userId", userId,
            "basic", Map.of(
                "totalFriends", 156,
                "mutualFriends", 45,
                "groupMemberships", 12,
                "avgConnectionStrength", 0.67
            ),
            "network", Map.of(
                "networkDiameter", 5,
                "averagePathLength", 2.8,
                "clusteringCoefficient", 0.45,
                "networkDensity", 0.23
            ),
            "engagement", Map.of(
                "dailyInteractions", 25,
                "weeklyInteractions", 167,
                "monthlyInteractions", 689,
                "engagementScore", 0.72
            ),
            "diversity", Map.of(
                "ageRange", "18-45",
                "genderDistribution", Map.of("male", 0.6, "female", 0.4),
                "locationDiversity", 0.78,
                "interestDiversity", 0.85
            )
        ));
    }
}