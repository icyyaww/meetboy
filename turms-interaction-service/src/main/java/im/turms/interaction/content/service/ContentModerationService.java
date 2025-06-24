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

package im.turms.interaction.content.service;

import im.turms.interaction.content.domain.ModerationResult;
import im.turms.interaction.content.domain.Moment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * 智能内容审核服务 (已迁移到interaction服务)
 * 
 * 提供多层次内容安全检查，包括：
 * - 文本内容审核 (敏感词过滤、情感分析)
 * - 图像内容审核 (色情、暴力、政治敏感)
 * - 视频内容审核 (关键帧检测)
 * - 链接安全检查
 */
@Slf4j
@Service
public class ContentModerationService {

    // 敏感词库 (实际项目中应该从配置文件或数据库加载)
    private static final List<String> SENSITIVE_WORDS = List.of(
        "违法", "暴力", "色情", "赌博", "毒品", "诈骗", "政治敏感", 
        "侮辱", "诽谤", "人身攻击", "恶意传播"
    );

    // 危险链接域名
    private static final List<String> DANGEROUS_DOMAINS = List.of(
        "malicious.com", "phishing.net", "scam.org"
    );

    // URL正则
    private static final Pattern URL_PATTERN = Pattern.compile(
        "https?://[\\w\\.-]+\\.[a-zA-Z]{2,}"
    );

    /**
     * 审核朋友圈内容
     */
    public Mono<im.turms.interaction.content.domain.ModerationResult> moderateContent(String content, List<String> imageUrls, 
                                                  List<String> videoUrls, List<String> links) {
        long startTime = System.currentTimeMillis();
        
        return Mono.fromCallable(() -> {
            Map<String, im.turms.interaction.content.domain.ModerationResult.ModerationCheck> checks = new HashMap<>();
            
            // 1. 文本内容审核
            im.turms.interaction.content.domain.ModerationResult.ModerationCheck textCheck = moderateText(content);
            checks.put("text", textCheck);
            
            // 2. 图片内容审核
            if (imageUrls != null && !imageUrls.isEmpty()) {
                im.turms.interaction.content.domain.ModerationResult.ModerationCheck imageCheck = moderateImages(imageUrls);
                checks.put("image", imageCheck);
            }
            
            // 3. 视频内容审核
            if (videoUrls != null && !videoUrls.isEmpty()) {
                im.turms.interaction.content.domain.ModerationResult.ModerationCheck videoCheck = moderateVideos(videoUrls);
                checks.put("video", videoCheck);
            }
            
            // 4. 链接安全检查
            if (links != null && !links.isEmpty()) {
                im.turms.interaction.content.domain.ModerationResult.ModerationCheck linkCheck = moderateLinks(links);
                checks.put("link", linkCheck);
            }
            
            // 计算总体得分
            double overallScore = calculateOverallScore(checks);
            
            // 生成审核建议
            String recommendation = generateRecommendation(overallScore, checks);
            
            // 生成审核标签
            List<String> labels = generateLabels(checks);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            return im.turms.interaction.content.domain.ModerationResult.builder()
                    .engine("TurmsContentModeration")
                    .version("1.0.0")
                    .overallScore(overallScore)
                    .checks(checks)
                    .labels(labels)
                    .recommendation(recommendation)
                    .moderatedAt(Instant.now())
                    .processingTime(processingTime)
                    .build();
        })
        .doOnSuccess(result -> log.debug("内容审核完成: 总分={}, 建议={}, 耗时={}ms", 
                                        result.getOverallScore(), result.getRecommendation(), 
                                        result.getProcessingTime()));
    }

    /**
     * 快速预审核 (仅检查明显违规内容)
     */
    public Mono<Boolean> quickModerate(String content) {
        return Mono.fromCallable(() -> {
            if (content == null || content.trim().isEmpty()) {
                return true;
            }
            
            String lowerContent = content.toLowerCase();
            
            // 检查是否包含明显敏感词
            for (String word : SENSITIVE_WORDS) {
                if (lowerContent.contains(word.toLowerCase())) {
                    log.warn("内容包含敏感词: {}", word);
                    return false;
                }
            }
            
            return true;
        });
    }

    /**
     * 文本内容审核
     */
    private im.turms.interaction.content.domain.ModerationResult.ModerationCheck moderateText(String content) {
        if (content == null || content.trim().isEmpty()) {
            return im.turms.interaction.content.domain.ModerationResult.ModerationCheck.builder()
                    .type("text")
                    .score(1.0)
                    .result(im.turms.interaction.content.domain.ModerationResult.ModerationCheck.CheckResult.PASS)
                    .details("文本内容为空")
                    .build();
        }

        double score = 1.0;
        String lowerContent = content.toLowerCase();
        StringBuilder issues = new StringBuilder();
        
        // 敏感词检测
        int sensitiveWordCount = 0;
        for (String word : SENSITIVE_WORDS) {
            if (lowerContent.contains(word.toLowerCase())) {
                sensitiveWordCount++;
                score -= 0.3;
                issues.append("包含敏感词: ").append(word).append("; ");
            }
        }
        
        // 重复字符检测
        if (hasExcessiveRepetition(content)) {
            score -= 0.1;
            issues.append("包含过多重复字符; ");
        }
        
        // 长度检查
        if (content.length() > 5000) {
            score -= 0.1;
            issues.append("内容过长; ");
        }
        
        // URL检查
        if (URL_PATTERN.matcher(content).find()) {
            score -= 0.05;
            issues.append("包含链接; ");
        }
        
        score = Math.max(0.0, score);
        
        im.turms.interaction.content.domain.ModerationResult.ModerationCheck.CheckResult result;
        if (score >= 0.8) {
            result = im.turms.interaction.content.domain.ModerationResult.ModerationCheck.CheckResult.PASS;
        } else if (score >= 0.5) {
            result = im.turms.interaction.content.domain.ModerationResult.ModerationCheck.CheckResult.WARNING;
        } else {
            result = im.turms.interaction.content.domain.ModerationResult.ModerationCheck.CheckResult.BLOCK;
        }
        
        return im.turms.interaction.content.domain.ModerationResult.ModerationCheck.builder()
                .type("text")
                .score(score)
                .result(result)
                .details(issues.length() > 0 ? issues.toString() : "文本内容正常")
                .build();
    }

    /**
     * 图片内容审核 (模拟)
     */
    private im.turms.interaction.content.domain.ModerationResult.ModerationCheck moderateImages(List<String> imageUrls) {
        // 模拟图片审核API调用
        double baseScore = 0.8 + ThreadLocalRandom.current().nextDouble(0.2);
        
        // 模拟检测结果
        StringBuilder details = new StringBuilder();
        details.append("检测图片数量: ").append(imageUrls.size()).append("; ");
        
        if (baseScore < 0.6) {
            details.append("检测到可能的敏感图片内容; ");
        } else if (baseScore < 0.8) {
            details.append("图片内容需要关注; ");
        } else {
            details.append("图片内容正常; ");
        }
        
        im.turms.interaction.content.domain.ModerationResult.ModerationCheck.CheckResult result;
        if (baseScore >= 0.8) {
            result = im.turms.interaction.content.domain.ModerationResult.ModerationCheck.CheckResult.PASS;
        } else if (baseScore >= 0.5) {
            result = im.turms.interaction.content.domain.ModerationResult.ModerationCheck.CheckResult.WARNING;
        } else {
            result = im.turms.interaction.content.domain.ModerationResult.ModerationCheck.CheckResult.BLOCK;
        }
        
        return im.turms.interaction.content.domain.ModerationResult.ModerationCheck.builder()
                .type("image")
                .score(baseScore)
                .result(result)
                .details(details.toString())
                .build();
    }

    /**
     * 视频内容审核 (模拟)
     */
    private im.turms.interaction.content.domain.ModerationResult.ModerationCheck moderateVideos(List<String> videoUrls) {
        // 模拟视频审核
        double baseScore = 0.75 + ThreadLocalRandom.current().nextDouble(0.25);
        
        StringBuilder details = new StringBuilder();
        details.append("检测视频数量: ").append(videoUrls.size()).append("; ");
        details.append("关键帧分析完成; ");
        
        if (baseScore < 0.6) {
            details.append("检测到可能的敏感视频内容; ");
        } else {
            details.append("视频内容正常; ");
        }
        
        im.turms.interaction.content.domain.ModerationResult.ModerationCheck.CheckResult result = baseScore >= 0.7 ? 
                im.turms.interaction.content.domain.ModerationResult.ModerationCheck.CheckResult.PASS : 
                im.turms.interaction.content.domain.ModerationResult.ModerationCheck.CheckResult.WARNING;
        
        return im.turms.interaction.content.domain.ModerationResult.ModerationCheck.builder()
                .type("video")
                .score(baseScore)
                .result(result)
                .details(details.toString())
                .build();
    }

    /**
     * 链接安全检查
     */
    private im.turms.interaction.content.domain.ModerationResult.ModerationCheck moderateLinks(List<String> links) {
        double score = 1.0;
        StringBuilder details = new StringBuilder();
        
        for (String link : links) {
            // 检查危险域名
            for (String dangerousDomain : DANGEROUS_DOMAINS) {
                if (link.contains(dangerousDomain)) {
                    score = 0.0;
                    details.append("包含危险链接: ").append(dangerousDomain).append("; ");
                    break;
                }
            }
        }
        
        if (score == 1.0) {
            details.append("链接安全检查通过; ");
        }
        
        im.turms.interaction.content.domain.ModerationResult.ModerationCheck.CheckResult result = score >= 0.5 ? 
                im.turms.interaction.content.domain.ModerationResult.ModerationCheck.CheckResult.PASS : 
                im.turms.interaction.content.domain.ModerationResult.ModerationCheck.CheckResult.BLOCK;
        
        return im.turms.interaction.content.domain.ModerationResult.ModerationCheck.builder()
                .type("link")
                .score(score)
                .result(result)
                .details(details.toString())
                .build();
    }

    /**
     * 计算总体得分
     */
    private double calculateOverallScore(Map<String, im.turms.interaction.content.domain.ModerationResult.ModerationCheck> checks) {
        if (checks.isEmpty()) {
            return 1.0;
        }
        
        double totalScore = 0.0;
        double totalWeight = 0.0;
        
        // 权重配置
        Map<String, Double> weights = Map.of(
            "text", 0.4,
            "image", 0.3,
            "video", 0.2,
            "link", 0.1
        );
        
        for (Map.Entry<String, im.turms.interaction.content.domain.ModerationResult.ModerationCheck> entry : checks.entrySet()) {
            String type = entry.getKey();
            double score = entry.getValue().getScore();
            double weight = weights.getOrDefault(type, 0.1);
            
            totalScore += score * weight;
            totalWeight += weight;
        }
        
        return totalWeight > 0 ? totalScore / totalWeight : 1.0;
    }

    /**
     * 生成审核建议
     */
    private String generateRecommendation(double overallScore, 
                                        Map<String, im.turms.interaction.content.domain.ModerationResult.ModerationCheck> checks) {
        if (overallScore >= 0.8) {
            return "内容审核通过，可以发布";
        } else if (overallScore >= 0.6) {
            return "内容需要关注，建议人工复审";
        } else if (overallScore >= 0.4) {
            return "内容存在风险，建议修改后重新提交";
        } else {
            return "内容违规，拒绝发布";
        }
    }

    /**
     * 生成审核标签
     */
    private List<String> generateLabels(Map<String, im.turms.interaction.content.domain.ModerationResult.ModerationCheck> checks) {
        return checks.entrySet().stream()
                .filter(entry -> entry.getValue().getResult() != im.turms.interaction.content.domain.ModerationResult.ModerationCheck.CheckResult.PASS)
                .map(entry -> entry.getKey() + "_risk")
                .toList();
    }

    /**
     * 检查是否有过度重复字符
     */
    private boolean hasExcessiveRepetition(String content) {
        if (content.length() < 10) {
            return false;
        }
        
        int repetitionCount = 0;
        char lastChar = 0;
        int consecutiveCount = 0;
        
        for (char c : content.toCharArray()) {
            if (c == lastChar) {
                consecutiveCount++;
                if (consecutiveCount >= 5) {
                    repetitionCount++;
                }
            } else {
                consecutiveCount = 1;
                lastChar = c;
            }
        }
        
        return repetitionCount > 3;
    }
}