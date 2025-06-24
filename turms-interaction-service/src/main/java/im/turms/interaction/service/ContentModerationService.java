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

package im.turms.interaction.service;

import im.turms.interaction.domain.Comment;
import im.turms.interaction.dto.ModerationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 内容审核服务 - 集成turms-content-service
 * 
 * 功能：
 * - 文本内容审核
 * - 评论内容审核
 * - 敏感词检测
 * - 垃圾信息识别
 */
@Slf4j
@Service("interactionContentModerationService")
@RequiredArgsConstructor
public class ContentModerationService {

    private final WebClient.Builder webClientBuilder;

    @Value("${turms.interaction-service.integration.content-service.base-url:http://localhost:8520}")
    private String contentServiceBaseUrl;

    // 本地敏感词列表 (快速预检)
    private static final List<String> LOCAL_SENSITIVE_WORDS = List.of(
            "垃圾", "广告", "spam", "fuck", "shit"
    );
    
    // 重复字符检测模式
    private static final Pattern REPEAT_PATTERN = Pattern.compile("(.)\\1{4,}");
    
    /**
     * 审核评论内容
     */
    public Mono<ModerationResult> moderateComment(Comment comment) {
        return Mono.fromCallable(() -> {
            ModerationResult result = new ModerationResult();
            List<String> labels = new ArrayList<>();
            double totalScore = 1.0;
            
            // 1. 本地快速检测
            double localScore = performLocalModeration(comment.getContent(), labels);
            totalScore = Math.min(totalScore, localScore);
            
            // 2. 检测重复字符
            if (REPEAT_PATTERN.matcher(comment.getContent()).find()) {
                labels.add("重复字符");
                totalScore *= 0.7;
            }
            
            // 3. 检测内容长度
            if (comment.getContent().length() > 1000) {
                labels.add("内容过长");
                totalScore *= 0.8;
            }
            
            // 4. 检测特殊字符比例
            long specialCharCount = comment.getContent().chars()
                    .filter(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))
                    .count();
            double specialCharRatio = (double) specialCharCount / comment.getContent().length();
            if (specialCharRatio > 0.3) {
                labels.add("特殊字符过多");
                totalScore *= 0.6;
            }
            
            result.setScore(totalScore);
            result.setLabels(labels);
            result.setReason(labels.isEmpty() ? "内容正常" : String.join(", ", labels));
            
            return result;
        })
        .flatMap(localResult -> {
            // 如果本地检测分数较低，调用远程服务进行深度审核
            if (localResult.getScore() < 0.8) {
                return performRemoteModeration(comment.getContent())
                        .map(remoteResult -> {
                            // 合并本地和远程结果
                            double finalScore = Math.min(localResult.getScore(), remoteResult.getScore());
                            List<String> allLabels = new ArrayList<>(localResult.getLabels());
                            allLabels.addAll(remoteResult.getLabels());
                            
                            ModerationResult combinedResult = new ModerationResult();
                            combinedResult.setScore(finalScore);
                            combinedResult.setLabels(allLabels);
                            combinedResult.setReason(String.join(", ", allLabels));
                            
                            return combinedResult;
                        })
                        .onErrorReturn(localResult); // 远程服务失败时使用本地结果
            }
            return Mono.just(localResult);
        })
        .doOnNext(result -> {
            log.debug("评论审核完成: content={}, score={}, labels={}", 
                    comment.getContent().substring(0, Math.min(50, comment.getContent().length())), 
                    result.getScore(), result.getLabels());
        });
    }

    /**
     * 审核纯文本内容
     */
    public Mono<ModerationResult> moderateText(String content) {
        return Mono.fromCallable(() -> {
            ModerationResult result = new ModerationResult();
            List<String> labels = new ArrayList<>();
            
            double score = performLocalModeration(content, labels);
            
            result.setScore(score);
            result.setLabels(labels);
            result.setReason(labels.isEmpty() ? "内容正常" : String.join(", ", labels));
            
            return result;
        });
    }

    /**
     * 快速内容检查 (仅本地检测)
     */
    public Mono<Boolean> quickCheck(String content) {
        return Mono.fromCallable(() -> {
            // 检查敏感词
            String lowerContent = content.toLowerCase();
            for (String word : LOCAL_SENSITIVE_WORDS) {
                if (lowerContent.contains(word)) {
                    return false;
                }
            }
            
            // 检查重复字符
            if (REPEAT_PATTERN.matcher(content).find()) {
                return false;
            }
            
            // 检查长度
            if (content.length() > 2000) {
                return false;
            }
            
            return true;
        });
    }

    /**
     * 本地审核逻辑
     */
    private double performLocalModeration(String content, List<String> labels) {
        double score = 1.0;
        String lowerContent = content.toLowerCase();
        
        // 检查敏感词
        for (String word : LOCAL_SENSITIVE_WORDS) {
            if (lowerContent.contains(word)) {
                labels.add("敏感词: " + word);
                score *= 0.3; // 大幅降低分数
            }
        }
        
        // 检查是否全是数字或特殊字符 (可能是垃圾信息)
        if (content.matches("[0-9\\s\\-\\+\\*\\/\\=\\!\\@\\#\\$\\%\\^\\&\\(\\)]+")) {
            labels.add("疑似垃圾信息");
            score *= 0.4;
        }
        
        // 检查URL模式 (可能是广告)
        if (content.matches(".*https?://.*") || content.matches(".*www\\..*")) {
            labels.add("包含链接");
            score *= 0.6;
        }
        
        // 检查联系方式模式
        if (content.matches(".*\\d{11}.*") || content.matches(".*QQ.*\\d+.*")) {
            labels.add("包含联系方式");
            score *= 0.5;
        }
        
        return score;
    }

    /**
     * 远程审核服务调用
     */
    private Mono<ModerationResult> performRemoteModeration(String content) {
        WebClient webClient = webClientBuilder
                .baseUrl(contentServiceBaseUrl)
                .build();
        
        Map<String, String> request = Map.of("content", content);
        
        return webClient.post()
                .uri("/content/moderation/quick-check")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    ModerationResult result = new ModerationResult();
                    
                    // 解析响应
                    Object scoreObj = response.get("score");
                    if (scoreObj instanceof Number) {
                        result.setScore(((Number) scoreObj).doubleValue());
                    } else {
                        result.setScore(0.5); // 默认分数
                    }
                    
                    Object labelsObj = response.get("labels");
                    if (labelsObj instanceof List) {
                        result.setLabels((List<String>) labelsObj);
                    } else {
                        result.setLabels(List.of("远程审核"));
                    }
                    
                    Object reasonObj = response.get("reason");
                    result.setReason(reasonObj != null ? reasonObj.toString() : "远程审核完成");
                    
                    return result;
                })
                .doOnError(error -> log.error("远程审核服务调用失败: content={}", 
                        content.substring(0, Math.min(50, content.length())), error))
                .timeout(java.time.Duration.ofSeconds(3)); // 3秒超时
    }

    /**
     * 批量文本审核
     */
    public Mono<Map<String, ModerationResult>> moderateTextBatch(List<String> contents) {
        return reactor.core.publisher.Flux.fromIterable(contents)
                .index()
                .flatMap(tuple -> {
                    Long index = tuple.getT1();
                    String content = tuple.getT2();
                    return moderateText(content)
                            .map(result -> Map.entry(index.toString(), result));
                })
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }
}