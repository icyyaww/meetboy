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

package im.turms.social.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 社交关系图分析服务
 * 
 * 提供社交网络分析、用户影响力计算、社区发现等功能
 */
@Service
public class SocialGraphAnalysisService {

    /**
     * 构建用户社交网络图
     */
    public Mono<Map<String, Object>> buildUserSocialNetwork(Long userId, int depth, int maxNodes) {
        // 模拟社交网络图构建
        List<Map<String, Object>> nodes = generateNetworkNodes(userId, depth, maxNodes);
        List<Map<String, Object>> edges = generateNetworkEdges(nodes);
        List<Map<String, Object>> clusters = identifyClusters(nodes);
        
        return Mono.just(Map.of(
            "userId", userId,
            "depth", depth,
            "totalNodes", nodes.size(),
            "totalEdges", edges.size(),
            "nodes", nodes,
            "edges", edges,
            "clusters", clusters,
            "metrics", calculateNetworkMetrics(nodes, edges),
            "generatedAt", System.currentTimeMillis()
        ));
    }

    /**
     * 计算用户影响力指标
     */
    public Mono<Map<String, Object>> calculateUserInfluence(Long userId) {
        // 模拟影响力计算算法
        double degreeCentrality = 0.7 + ThreadLocalRandom.current().nextDouble(0.25);
        double betweennessCentrality = 0.5 + ThreadLocalRandom.current().nextDouble(0.4);
        double closenessCentrality = 0.6 + ThreadLocalRandom.current().nextDouble(0.3);
        double eigenvectorCentrality = 0.6 + ThreadLocalRandom.current().nextDouble(0.25);
        double pageRank = 0.55 + ThreadLocalRandom.current().nextDouble(0.3);
        
        double overallInfluence = (degreeCentrality * 0.25 + betweennessCentrality * 0.2 + 
                                 closenessCentrality * 0.2 + eigenvectorCentrality * 0.2 + 
                                 pageRank * 0.15);
        
        return Mono.just(Map.of(
            "userId", userId,
            "overallInfluence", Math.round(overallInfluence * 100.0) / 100.0,
            "metrics", Map.of(
                "degreeCentrality", Math.round(degreeCentrality * 100.0) / 100.0,
                "betweennessCentrality", Math.round(betweennessCentrality * 100.0) / 100.0,
                "closenessCentrality", Math.round(closenessCentrality * 100.0) / 100.0,
                "eigenvectorCentrality", Math.round(eigenvectorCentrality * 100.0) / 100.0,
                "pageRank", Math.round(pageRank * 100.0) / 100.0
            ),
            "rank", calculateInfluenceRank(overallInfluence),
            "analysis", generateInfluenceAnalysis(userId, overallInfluence),
            "recommendations", generateInfluenceRecommendations(overallInfluence)
        ));
    }

    /**
     * 发现社交路径
     */
    public Flux<Map<String, Object>> findSocialPaths(Long userId1, Long userId2, int maxDegrees) {
        // 模拟路径发现算法（如 BFS、Dijkstra 等）
        return Flux.range(1, 3)
            .map(pathId -> {
                int degrees = 1 + ThreadLocalRandom.current().nextInt(maxDegrees - 1);
                double strength = 0.5 + ThreadLocalRandom.current().nextDouble(0.4);
                
                List<Map<String, Object>> intermediates = generateIntermediateNodes(userId1, userId2, degrees - 1);
                List<Long> path = generatePath(userId1, userId2, intermediates);
                
                return Map.of(
                    "pathId", pathId,
                    "path", path,
                    "degrees", degrees,
                    "strength", Math.round(strength * 100.0) / 100.0,
                    "intermediates", intermediates,
                    "pathType", determinePathType(degrees),
                    "reliability", calculatePathReliability(strength, degrees)
                );
            })
            .sort((p1, p2) -> Double.compare((Double) p2.get("strength"), (Double) p1.get("strength")));
    }

    /**
     * 社区发现算法
     */
    public Flux<Map<String, Object>> discoverCommunities(Long userId) {
        String[][] communityData = {
            {"技术社区", "45", "0.78", "核心成员", "编程,技术,AI"},
            {"大学同学", "28", "0.85", "活跃成员", "校友,聚会,回忆"},
            {"兴趣小组", "16", "0.92", "边缘成员", "摄影,旅行,美食"},
            {"工作同事", "35", "0.73", "普通成员", "工作,项目,团队"}
        };
        
        return Flux.range(0, communityData.length)
            .map(i -> {
                String[] data = communityData[i];
                return Map.of(
                    "communityId", i + 1,
                    "name", data[0],
                    "size", Integer.parseInt(data[1]),
                    "density", Double.parseDouble(data[2]),
                    "role", data[3],
                    "members", generateCommunityMembers(userId, Integer.parseInt(data[1])),
                    "tags", List.of(data[4].split(",")),
                    "activityLevel", calculateCommunityActivity(Double.parseDouble(data[2])),
                    "joinDate", System.currentTimeMillis() - ThreadLocalRandom.current().nextLong(86400000L * 365)
                );
            });
    }

    /**
     * 分析社交趋势
     */
    public Mono<Map<String, Object>> analyzeSocialTrends(Long userId, int days) {
        // 模拟趋势分析
        int newConnections = ThreadLocalRandom.current().nextInt(1, 8);
        int lostConnections = ThreadLocalRandom.current().nextInt(0, 3);
        double currentInfluence = 0.7 + ThreadLocalRandom.current().nextDouble(0.2);
        double previousInfluence = currentInfluence - 0.05 + ThreadLocalRandom.current().nextDouble(0.1);
        
        return Mono.just(Map.of(
            "userId", userId,
            "period", days + "天",
            "growth", Map.of(
                "newConnections", newConnections,
                "lostConnections", lostConnections,
                "netGrowth", newConnections - lostConnections,
                "growthRate", Math.round((double)(newConnections - lostConnections) / 50 * 100.0) / 100.0
            ),
            "activity", generateActivityTrends(),
            "influence", Map.of(
                "currentScore", Math.round(currentInfluence * 100.0) / 100.0,
                "previousScore", Math.round(previousInfluence * 100.0) / 100.0,
                "change", Math.round((currentInfluence - previousInfluence) * 100.0) / 100.0,
                "trend", currentInfluence > previousInfluence ? "增长" : "下降"
            ),
            "predictions", generateTrendPredictions(currentInfluence, newConnections - lostConnections),
            "insights", generateTrendInsights(days, newConnections, currentInfluence)
        ));
    }

    /**
     * 获取社交网络统计
     */
    public Mono<Map<String, Object>> getSocialStatistics(Long userId) {
        return Mono.just(Map.of(
            "userId", userId,
            "basic", Map.of(
                "totalFriends", 100 + ThreadLocalRandom.current().nextInt(200),
                "mutualFriends", 20 + ThreadLocalRandom.current().nextInt(50),
                "groupMemberships", 5 + ThreadLocalRandom.current().nextInt(15),
                "avgConnectionStrength", 0.5 + ThreadLocalRandom.current().nextDouble(0.3)
            ),
            "network", generateNetworkStatistics(),
            "engagement", generateEngagementStatistics(),
            "diversity", generateDiversityStatistics(),
            "lastCalculated", System.currentTimeMillis()
        ));
    }

    // 辅助方法

    private List<Map<String, Object>> generateNetworkNodes(Long userId, int depth, int maxNodes) {
        return List.of(
            Map.of("id", userId, "name", "用户本人", "level", 0, "influence", 0.85, "type", "self"),
            Map.of("id", userId + 1, "name", "好友A", "level", 1, "influence", 0.72, "type", "friend"),
            Map.of("id", userId + 2, "name", "好友B", "level", 1, "influence", 0.68, "type", "friend"),
            Map.of("id", userId + 10, "name", "二度好友C", "level", 2, "influence", 0.45, "type", "friend_of_friend")
        );
    }

    private List<Map<String, Object>> generateNetworkEdges(List<Map<String, Object>> nodes) {
        return List.of(
            Map.of("source", nodes.get(0).get("id"), "target", nodes.get(1).get("id"), 
                   "weight", 0.8, "type", "friend", "strength", "strong"),
            Map.of("source", nodes.get(0).get("id"), "target", nodes.get(2).get("id"), 
                   "weight", 0.9, "type", "friend", "strength", "strong"),
            Map.of("source", nodes.get(1).get("id"), "target", nodes.get(3).get("id"), 
                   "weight", 0.6, "type", "friend", "strength", "medium")
        );
    }

    private List<Map<String, Object>> identifyClusters(List<Map<String, Object>> nodes) {
        return List.of(
            Map.of("id", 1, "name", "工作圈", "members", List.of(nodes.get(1).get("id"), nodes.get(2).get("id"))),
            Map.of("id", 2, "name", "同学圈", "members", List.of(nodes.get(3).get("id")))
        );
    }

    private Map<String, Object> calculateNetworkMetrics(List<Map<String, Object>> nodes, List<Map<String, Object>> edges) {
        return Map.of(
            "density", 0.23,
            "clustering", 0.45,
            "diameter", 5,
            "averagePathLength", 2.8
        );
    }

    private Map<String, Object> calculateInfluenceRank(double influence) {
        int globalRank = (int) ((1.0 - influence) * 2000) + 100;
        int localRank = (int) ((1.0 - influence) * 50) + 5;
        int percentile = (int) (influence * 100);
        
        return Map.of(
            "globalRank", globalRank,
            "localRank", localRank,
            "percentile", percentile
        );
    }

    private Map<String, Object> generateInfluenceAnalysis(Long userId, double influence) {
        return Map.of(
            "networkSize", 200 + ThreadLocalRandom.current().nextInt(100),
            "activeConnections", (int) (influence * 100) + ThreadLocalRandom.current().nextInt(20),
            "reachability", influence,
            "clustering", 0.3 + ThreadLocalRandom.current().nextDouble(0.3)
        );
    }

    private List<String> generateInfluenceRecommendations(double influence) {
        if (influence > 0.8) {
            return List.of("保持现有的活跃度", "考虑成为意见领袖", "分享更多有价值的内容");
        } else if (influence > 0.6) {
            return List.of("增加与他人的互动", "参与更多社区活动", "扩展社交圈子");
        } else {
            return List.of("主动建立新的连接", "提高内容质量", "增加社交活跃度");
        }
    }

    private List<Map<String, Object>> generateIntermediateNodes(Long userId1, Long userId2, int count) {
        return List.of(
            Map.of("userId", userId1 + 100, "name", "中间人A", "relation", "同事"),
            Map.of("userId", userId1 + 200, "name", "中间人B", "relation", "朋友")
        );
    }

    private List<Long> generatePath(Long userId1, Long userId2, List<Map<String, Object>> intermediates) {
        return List.of(userId1, userId1 + 100, userId1 + 200, userId2);
    }

    private String determinePathType(int degrees) {
        return switch (degrees) {
            case 1 -> "直接连接";
            case 2 -> "一度分离";
            case 3 -> "二度分离";
            default -> degrees + "度分离";
        };
    }

    private String calculatePathReliability(double strength, int degrees) {
        if (strength > 0.8 && degrees <= 2) return "很高";
        if (strength > 0.6 && degrees <= 3) return "较高";
        if (strength > 0.4) return "中等";
        return "较低";
    }

    private List<Long> generateCommunityMembers(Long userId, int size) {
        return List.of(userId + 1, userId + 2, userId + 3);
    }

    private String calculateCommunityActivity(double density) {
        if (density > 0.8) return "非常活跃";
        if (density > 0.6) return "比较活跃";
        if (density > 0.4) return "一般活跃";
        return "不太活跃";
    }

    private Map<String, Object> generateActivityTrends() {
        return Map.of(
            "interactionFrequency", 0.7 + ThreadLocalRandom.current().nextDouble(0.2),
            "responseRate", 0.8 + ThreadLocalRandom.current().nextDouble(0.15),
            "initiationRate", 0.6 + ThreadLocalRandom.current().nextDouble(0.25)
        );
    }

    private Map<String, Object> generateTrendPredictions(double currentInfluence, int netGrowth) {
        double nextMonthGrowth = netGrowth > 0 ? 0.05 + ThreadLocalRandom.current().nextDouble(0.05) : 
                                               -0.02 + ThreadLocalRandom.current().nextDouble(0.04);
        double influenceProjection = currentInfluence + nextMonthGrowth;
        
        return Map.of(
            "nextMonthGrowth", Math.round(nextMonthGrowth * 100.0) / 100.0,
            "influenceProjection", Math.round(influenceProjection * 100.0) / 100.0,
            "recommendedActions", generateActionRecommendations(currentInfluence, netGrowth)
        );
    }

    private List<String> generateActionRecommendations(double influence, int netGrowth) {
        if (netGrowth > 0 && influence > 0.7) {
            return List.of("继续保持当前的社交策略", "考虑成为社区意见领袖", "分享更多专业知识");
        } else if (netGrowth <= 0) {
            return List.of("增加与现有好友的互动", "主动参与群组讨论", "扩展新的社交圈子");
        } else {
            return List.of("提高内容质量", "增加社交活跃度", "建立更多有意义的连接");
        }
    }

    private Map<String, Object> generateTrendInsights(int days, int newConnections, double influence) {
        return Map.of(
            "period", days,
            "highlights", List.of(
                "最近" + days + "天新增" + newConnections + "个连接",
                "当前影响力为" + Math.round(influence * 100) + "%",
                "社交活跃度呈" + (newConnections > 3 ? "上升" : "平稳") + "趋势"
            ),
            "opportunities", List.of("技术社区领域有发展潜力", "可以加强与同事的联系"),
            "concerns", newConnections < 2 ? List.of("社交增长较慢") : List.of()
        );
    }

    private Map<String, Object> generateNetworkStatistics() {
        return Map.of(
            "networkDiameter", 4 + ThreadLocalRandom.current().nextInt(3),
            "averagePathLength", 2.5 + ThreadLocalRandom.current().nextDouble(1.0),
            "clusteringCoefficient", 0.3 + ThreadLocalRandom.current().nextDouble(0.3),
            "networkDensity", 0.15 + ThreadLocalRandom.current().nextDouble(0.15)
        );
    }

    private Map<String, Object> generateEngagementStatistics() {
        return Map.of(
            "dailyInteractions", 15 + ThreadLocalRandom.current().nextInt(20),
            "weeklyInteractions", 100 + ThreadLocalRandom.current().nextInt(100),
            "monthlyInteractions", 400 + ThreadLocalRandom.current().nextInt(400),
            "engagementScore", 0.6 + ThreadLocalRandom.current().nextDouble(0.3)
        );
    }

    private Map<String, Object> generateDiversityStatistics() {
        return Map.of(
            "ageRange", "22-45",
            "genderDistribution", Map.of("male", 0.5 + ThreadLocalRandom.current().nextDouble(0.3), 
                                        "female", 0.5 + ThreadLocalRandom.current().nextDouble(0.3)),
            "locationDiversity", 0.6 + ThreadLocalRandom.current().nextDouble(0.3),
            "interestDiversity", 0.7 + ThreadLocalRandom.current().nextDouble(0.25)
        );
    }
}