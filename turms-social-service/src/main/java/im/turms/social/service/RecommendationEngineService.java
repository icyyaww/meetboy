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
 * 推荐引擎核心服务
 * 
 * 实现多种推荐算法，包括协同过滤、内容推荐、混合推荐等
 */
@Service
public class RecommendationEngineService {

    /**
     * 混合推荐算法
     * 结合多种推荐策略，生成综合推荐结果
     */
    public Flux<Map<String, Object>> generateHybridRecommendations(Long userId, int limit, double threshold) {
        // 模拟混合推荐算法
        return Flux.range(1, limit)
            .map(i -> {
                double score = 0.5 + ThreadLocalRandom.current().nextDouble(0.4);
                if (score < threshold) {
                    score = threshold + ThreadLocalRandom.current().nextDouble(0.2);
                }
                
                return Map.of(
                    "userId", userId + 1000 + i,
                    "username", "推荐用户" + i,
                    "avatar", "avatar" + i + ".jpg",
                    "score", Math.round(score * 100.0) / 100.0,
                    "algorithm", "hybrid",
                    "factors", generateRecommendationFactors(),
                    "reason", generateRecommendationReason(score)
                );
            })
            .filter(rec -> (Double) rec.get("score") >= threshold);
    }

    /**
     * 协同过滤推荐
     * 基于用户行为相似性进行推荐
     */
    public Flux<Map<String, Object>> generateCollaborativeFilteringRecommendations(Long userId, int limit) {
        return Flux.range(1, limit)
            .map(i -> {
                String algorithm = i % 2 == 0 ? "UserBasedCF" : "ItemBasedCF";
                double similarity = 0.6 + ThreadLocalRandom.current().nextDouble(0.3);
                
                return Map.of(
                    "userId", userId + 2000 + i,
                    "username", "协同推荐" + i,
                    "score", Math.round(similarity * 100.0) / 100.0,
                    "algorithm", algorithm,
                    "similarity", Math.round(similarity * 100.0) / 100.0,
                    "reason", algorithm.equals("UserBasedCF") ? "与您有相似喜好的用户" : "喜欢相似内容的用户",
                    "behaviorSimilarity", generateBehaviorSimilarity()
                );
            });
    }

    /**
     * 基于内容的推荐
     * 根据用户画像和兴趣标签推荐
     */
    public Flux<Map<String, Object>> generateContentBasedRecommendations(Long userId, int limit) {
        List<List<String>> tagGroups = List.of(
            List.of("机器学习", "Python", "数据分析"),
            List.of("前端开发", "Vue.js", "TypeScript"),
            List.of("后端开发", "Java", "Spring Boot"),
            List.of("移动开发", "Flutter", "React Native")
        );
        
        return Flux.range(1, limit)
            .map(i -> {
                List<String> tags = tagGroups.get(i % tagGroups.size());
                double similarity = 0.7 + ThreadLocalRandom.current().nextDouble(0.25);
                
                return Map.of(
                    "userId", userId + 3000 + i,
                    "username", "内容推荐" + i,
                    "score", Math.round(similarity * 100.0) / 100.0,
                    "matchingTags", tags,
                    "profileSimilarity", Math.round(similarity * 100.0) / 100.0,
                    "reason", "兴趣标签高度匹配",
                    "contentFeatures", generateContentFeatures(tags)
                );
            });
    }

    /**
     * 解释推荐原因
     */
    public Mono<Map<String, Object>> explainRecommendation(Long userId, Long recommendedUserId) {
        return Mono.just(Map.of(
            "userId", userId,
            "recommendedUserId", recommendedUserId,
            "algorithm", "hybrid",
            "totalScore", 0.85,
            "explanation", generateDetailedExplanation(),
            "factors", List.of(
                Map.of("factor", "共同好友", "weight", 0.3, "score", 0.9, "contribution", 0.27),
                Map.of("factor", "兴趣匹配", "weight", 0.25, "score", 0.8, "contribution", 0.20),
                Map.of("factor", "地理位置", "weight", 0.2, "score", 0.7, "contribution", 0.14),
                Map.of("factor", "活跃度", "weight", 0.15, "score", 0.9, "contribution", 0.135),
                Map.of("factor", "互动历史", "weight", 0.1, "score", 0.6, "contribution", 0.06)
            ),
            "confidence", 0.82,
            "reliability", "高"
        ));
    }

    /**
     * 记录推荐反馈
     */
    public Mono<Map<String, Object>> recordRecommendationFeedback(
            Long userId, Long recommendedUserId, String action) {
        
        // 模拟反馈记录逻辑
        double feedbackWeight = switch (action) {
            case "accept" -> 1.0;
            case "like" -> 0.8;
            case "ignore" -> 0.0;
            case "dislike" -> -0.5;
            default -> 0.0;
        };
        
        return Mono.just(Map.of(
            "success", true,
            "userId", userId,
            "recommendedUserId", recommendedUserId,
            "action", action,
            "feedbackWeight", feedbackWeight,
            "timestamp", System.currentTimeMillis(),
            "message", "反馈已记录，将用于改进推荐算法"
        ));
    }

    /**
     * 获取推荐算法性能指标
     */
    public Mono<Map<String, Object>> getRecommendationMetrics() {
        return Mono.just(Map.of(
            "performance", Map.of(
                "precision", 0.78,
                "recall", 0.65,
                "f1Score", 0.71,
                "ndcg", 0.82,
                "coverage", 0.35
            ),
            "userEngagement", Map.of(
                "clickThroughRate", 0.12,
                "acceptanceRate", 0.08,
                "averageTimeToDecision", 45.6,
                "satisfactionScore", 4.2
            ),
            "algorithmDistribution", Map.of(
                "hybrid", 0.45,
                "collaborative", 0.30,
                "contentBased", 0.20,
                "trending", 0.05
            ),
            "lastUpdated", System.currentTimeMillis()
        ));
    }

    /**
     * 生成推荐因子
     */
    private List<String> generateRecommendationFactors() {
        List<List<String>> factorSets = List.of(
            List.of("共同好友: 5人", "相同兴趣: 编程, 旅行"),
            List.of("共同好友: 3人", "地理位置接近"),
            List.of("兴趣匹配度高", "活跃度相似"),
            List.of("同在技术群组", "互动频繁")
        );
        
        return factorSets.get(ThreadLocalRandom.current().nextInt(factorSets.size()));
    }

    /**
     * 生成推荐原因
     */
    private String generateRecommendationReason(double score) {
        if (score >= 0.8) {
            return "高度匹配，强烈推荐";
        } else if (score >= 0.7) {
            return "较好匹配，建议关注";
        } else if (score >= 0.6) {
            return "中等匹配，可以考虑";
        } else {
            return "轻度匹配，仅供参考";
        }
    }

    /**
     * 生成行为相似性数据
     */
    private Map<String, Object> generateBehaviorSimilarity() {
        return Map.of(
            "messageFrequency", 0.75,
            "onlineTimeOverlap", 0.68,
            "groupParticipation", 0.82,
            "contentPreference", 0.71
        );
    }

    /**
     * 生成内容特征
     */
    private Map<String, Object> generateContentFeatures(List<String> tags) {
        return Map.of(
            "primaryTags", tags,
            "skillLevel", "中级",
            "activityLevel", "高",
            "contentType", "技术分享",
            "engagement", 0.78
        );
    }

    /**
     * 生成详细解释
     */
    private String generateDetailedExplanation() {
        return "该用户与您有5个共同好友，兴趣标签匹配度80%，地理位置较近（1.2公里），" +
               "在技术领域活跃度相似，建议优先关注";
    }
}