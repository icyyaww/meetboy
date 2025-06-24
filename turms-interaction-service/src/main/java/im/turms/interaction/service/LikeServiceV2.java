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

import im.turms.interaction.domain.mysql.LikeEntity;
import im.turms.interaction.repository.LikeRepository;
import im.turms.interaction.repository.LikeCountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 点赞服务V2 - Redis主导 + MySQL持久化架构
 * 
 * 设计理念：
 * - Redis：负责高频读写，提供毫秒级响应
 * - MySQL：负责持久化存储，用于数据恢复和审计
 * - 异步同步：避免影响用户体验
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LikeServiceV2 {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final LikeRepository likeRepository;
    private final LikeCountRepository likeCountRepository;

    // Redis键前缀
    private static final String LIKE_COUNT_KEY_PREFIX = "like:count:";          // 点赞计数: like:count:POST:123
    private static final String USER_LIKE_SET_PREFIX = "like:user:";            // 用户点赞集合: like:user:1001
    private static final String TARGET_LIKE_SET_PREFIX = "like:target:";        // 目标点赞集合: like:target:POST:123
    
    // Redis脚本：原子操作保证数据一致性
    private static final String TOGGLE_LIKE_SCRIPT = """
            local userId = ARGV[1]
            local targetType = ARGV[2]
            local targetId = ARGV[3]
            
            local countKey = KEYS[1]     -- like:count:POST:123
            local userSetKey = KEYS[2]   -- like:user:1001
            local targetSetKey = KEYS[3] -- like:target:POST:123
            
            -- 检查用户是否已点赞
            local isLiked = redis.call('SISMEMBER', userSetKey, targetType .. ':' .. targetId)
            
            if isLiked == 1 then
                -- 取消点赞
                redis.call('SREM', userSetKey, targetType .. ':' .. targetId)
                redis.call('SREM', targetSetKey, userId)
                local newCount = redis.call('DECR', countKey)
                -- 设置过期时间（7天）
                redis.call('EXPIRE', countKey, 604800)
                redis.call('EXPIRE', userSetKey, 604800)
                redis.call('EXPIRE', targetSetKey, 604800)
                return {0, newCount}  -- 0表示取消点赞，返回新的计数
            else
                -- 添加点赞
                redis.call('SADD', userSetKey, targetType .. ':' .. targetId)
                redis.call('SADD', targetSetKey, userId)
                local newCount = redis.call('INCR', countKey)
                -- 设置过期时间（7天）
                redis.call('EXPIRE', countKey, 604800)
                redis.call('EXPIRE', userSetKey, 604800)
                redis.call('EXPIRE', targetSetKey, 604800)
                return {1, newCount}  -- 1表示添加点赞，返回新的计数
            end
            """;

    /**
     * 切换点赞状态（Redis主操作 + 异步MySQL持久化）
     */
    public Mono<LikeResult> toggleLike(Long userId, String targetType, String targetId, 
                                     String deviceType, String deviceId, String ipAddress, 
                                     String locationInfo) {
        
        String countKey = LIKE_COUNT_KEY_PREFIX + targetType + ":" + targetId;
        String userSetKey = USER_LIKE_SET_PREFIX + userId;
        String targetSetKey = TARGET_LIKE_SET_PREFIX + targetType + ":" + targetId;
        
        List<String> keys = Arrays.asList(countKey, userSetKey, targetSetKey);
        List<String> args = Arrays.asList(userId.toString(), targetType, targetId);
        
        return redisTemplate.execute(RedisScript.of(TOGGLE_LIKE_SCRIPT, List.class), keys, args)
                .cast(List.class)
                .next()  // 从Flux转换为Mono
                .map(result -> {
                    Integer action = (Integer) result.get(0);  // 0=取消点赞, 1=添加点赞
                    Integer newCount = (Integer) result.get(1); // 新的点赞计数
                    
                    boolean isLiked = action == 1;
                    
                    // 异步持久化到MySQL
                    if (isLiked) {
                        persistLikeToMySQL(userId, targetType, targetId, deviceType, deviceId, 
                                         ipAddress, locationInfo).subscribe();
                    } else {
                        removeLikeFromMySQL(userId, targetType, targetId).subscribe();
                    }
                    
                    return LikeResult.builder()
                            .isLiked(isLiked)
                            .likeCount(newCount)
                            .targetType(targetType)
                            .targetId(targetId)
                            .userId(userId)
                            .build();
                })
                .doOnSuccess(result -> log.debug("点赞操作完成: userId={}, target={}:{}, isLiked={}, count={}", 
                        userId, targetType, targetId, result.isLiked(), result.getLikeCount()))
                .doOnError(error -> log.error("点赞操作失败: userId={}, target={}:{}", 
                        userId, targetType, targetId, error));
    }

    /**
     * 查询点赞计数（优先从Redis读取）
     */
    public Mono<Integer> getLikeCount(String targetType, String targetId) {
        String countKey = LIKE_COUNT_KEY_PREFIX + targetType + ":" + targetId;
        
        return redisTemplate.opsForValue().get(countKey)
                .cast(Integer.class)
                .switchIfEmpty(
                    // Redis中没有数据，从MySQL恢复
                    loadLikeCountFromMySQL(targetType, targetId)
                            .flatMap(count -> {
                                // 缓存到Redis
                                return redisTemplate.opsForValue()
                                        .set(countKey, count, Duration.ofDays(7))
                                        .thenReturn(count);
                            })
                )
                .defaultIfEmpty(0);
    }

    /**
     * 检查用户是否已点赞（从Redis查询）
     */
    public Mono<Boolean> isLiked(Long userId, String targetType, String targetId) {
        String userSetKey = USER_LIKE_SET_PREFIX + userId;
        String targetKey = targetType + ":" + targetId;
        
        return redisTemplate.opsForSet().isMember(userSetKey, targetKey)
                .switchIfEmpty(
                    // Redis中没有数据，从MySQL查询
                    Mono.fromCallable(() -> 
                        likeRepository.existsByUserIdAndTargetTypeAndTargetId(
                            userId, LikeEntity.TargetType.valueOf(targetType), targetId))
                            .subscribeOn(Schedulers.boundedElastic())
                );
    }

    /**
     * 批量查询用户点赞状态
     */
    public Mono<List<LikeStatus>> batchCheckLikeStatus(Long userId, String targetType, 
                                                      List<String> targetIds) {
        String userSetKey = USER_LIKE_SET_PREFIX + userId;
        
        // 构建目标键列表
        List<String> targetKeys = targetIds.stream()
                .map(targetId -> targetType + ":" + targetId)
                .toList();
        
        return Flux.fromIterable(targetKeys)
                .flatMap(targetKey -> 
                    redisTemplate.opsForSet().isMember(userSetKey, targetKey)
                            .map(isLiked -> {
                                String[] parts = targetKey.split(":", 2);
                                return LikeStatus.builder()
                                        .targetType(parts[0])
                                        .targetId(parts[1])
                                        .isLiked(isLiked)
                                        .build();
                            })
                )
                .collectList()
                .doOnSuccess(statuses -> log.debug("批量查询点赞状态完成: userId={}, 查询{}个目标", 
                        userId, statuses.size()));
    }

    /**
     * 获取目标的点赞用户列表（分页）
     */
    public Flux<Long> getLikeUsers(String targetType, String targetId, int page, int size) {
        String targetSetKey = TARGET_LIKE_SET_PREFIX + targetType + ":" + targetId;
        
        return redisTemplate.opsForSet().members(targetSetKey)
                .cast(String.class)
                .map(Long::valueOf)
                .skip((long) page * size)
                .take(size)
                .doOnComplete(() -> log.debug("获取点赞用户列表: target={}:{}, page={}, size={}", 
                        targetType, targetId, page, size));
    }

    /**
     * 异步持久化点赞记录到MySQL
     */
    private Mono<Void> persistLikeToMySQL(Long userId, String targetType, String targetId,
                                        String deviceType, String deviceId, String ipAddress,
                                        String locationInfo) {
        return Mono.fromRunnable(() -> {
            try {
                LikeEntity likeEntity = LikeEntity.builder()
                        .userId(userId)
                        .targetType(LikeEntity.TargetType.valueOf(targetType))
                        .targetId(targetId)
                        .deviceType(deviceType)
                        .deviceId(deviceId)
                        .ipAddress(ipAddress)
                        .locationInfo(locationInfo)
                        .build();
                
                likeRepository.save(likeEntity);
                log.debug("点赞记录已持久化到MySQL: userId={}, target={}:{}", userId, targetType, targetId);
            } catch (Exception e) {
                log.error("点赞记录持久化失败: userId={}, target={}:{}", userId, targetType, targetId, e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * 异步从MySQL删除点赞记录
     */
    private Mono<Void> removeLikeFromMySQL(Long userId, String targetType, String targetId) {
        return Mono.fromRunnable(() -> {
            try {
                int deleted = likeRepository.deleteByUserIdAndTargetTypeAndTargetId(
                        userId, LikeEntity.TargetType.valueOf(targetType), targetId);
                log.debug("点赞记录已从MySQL删除: userId={}, target={}:{}, deleted={}", 
                        userId, targetType, targetId, deleted);
            } catch (Exception e) {
                log.error("点赞记录删除失败: userId={}, target={}:{}", userId, targetType, targetId, e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * 从MySQL加载点赞计数
     */
    private Mono<Integer> loadLikeCountFromMySQL(String targetType, String targetId) {
        return Mono.fromCallable(() -> {
            long count = likeRepository.countByTargetTypeAndTargetId(
                    LikeEntity.TargetType.valueOf(targetType), targetId);
            return (int) count;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 数据恢复：从MySQL恢复Redis数据
     */
    @Transactional(readOnly = true)
    public Mono<Void> recoverDataFromMySQL(String targetType, String targetId) {
        return Mono.fromRunnable(() -> {
            try {
                LikeEntity.TargetType type = LikeEntity.TargetType.valueOf(targetType);
                
                // 1. 恢复点赞计数
                long count = likeRepository.countByTargetTypeAndTargetId(type, targetId);
                String countKey = LIKE_COUNT_KEY_PREFIX + targetType + ":" + targetId;
                redisTemplate.opsForValue().set(countKey, (int) count, Duration.ofDays(7)).block();
                
                // 2. 恢复点赞关系
                String targetSetKey = TARGET_LIKE_SET_PREFIX + targetType + ":" + targetId;
                likeRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
                        type, targetId, org.springframework.data.domain.Pageable.ofSize(10000))
                        .getContent()
                        .forEach(like -> {
                            // 恢复目标点赞集合
                            redisTemplate.opsForSet().add(targetSetKey, like.getUserId().toString()).block();
                            
                            // 恢复用户点赞集合
                            String userSetKey = USER_LIKE_SET_PREFIX + like.getUserId();
                            redisTemplate.opsForSet().add(userSetKey, targetType + ":" + targetId).block();
                        });
                
                log.info("数据恢复完成: target={}:{}, count={}", targetType, targetId, count);
            } catch (Exception e) {
                log.error("数据恢复失败: target={}:{}", targetType, targetId, e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    // 内部类：点赞结果
    @lombok.Data
    @lombok.Builder
    public static class LikeResult {
        private boolean isLiked;
        private Integer likeCount;
        private String targetType;
        private String targetId;
        private Long userId;
    }

    // 内部类：点赞状态
    @lombok.Data
    @lombok.Builder
    public static class LikeStatus {
        private String targetType;
        private String targetId;
        private Boolean isLiked;
    }
}