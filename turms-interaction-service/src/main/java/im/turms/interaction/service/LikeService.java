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

import im.turms.interaction.domain.Like;
import im.turms.interaction.domain.InteractionEvent;
import im.turms.interaction.dto.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * 高并发点赞服务
 * 
 * 核心特性：
 * - Redis缓存 + MongoDB持久化
 * - 异步批量写入优化
 * - 防重复点赞机制
 * - 实时计数更新
 * - 事件驱动架构
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LikeService {

    private final ReactiveMongoTemplate mongoTemplate;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final EventPublishingService eventPublishingService;
    private final UserServiceClient userServiceClient;

    private static final String LIKE_COUNT_KEY_PREFIX = "like:count:";
    private static final String LIKE_SET_KEY_PREFIX = "like:set:";
    private static final String USER_LIKE_KEY_PREFIX = "user:likes:";
    
    /**
     * 点赞或取消点赞 (高并发优化版本)
     */
    public Mono<Boolean> toggleLike(Long userId, Like.TargetType targetType, String targetId, 
                                   Like.DeviceInfo deviceInfo, Like.Location location) {
        
        String likeSetKey = LIKE_SET_KEY_PREFIX + targetType + ":" + targetId;
        String userLikeKey = USER_LIKE_KEY_PREFIX + userId;
        String countKey = LIKE_COUNT_KEY_PREFIX + targetType + ":" + targetId;
        
        return redisTemplate.opsForSet()
                .isMember(likeSetKey, userId.toString())
                .flatMap(isLiked -> {
                    if (Boolean.TRUE.equals(isLiked)) {
                        // 取消点赞
                        return removeLike(userId, targetType, targetId, likeSetKey, userLikeKey, countKey);
                    } else {
                        // 添加点赞
                        return addLike(userId, targetType, targetId, likeSetKey, userLikeKey, countKey, 
                                     deviceInfo, location);
                    }
                })
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
                .doOnError(error -> log.error("点赞操作失败: userId={}, targetType={}, targetId={}", 
                                            userId, targetType, targetId, error));
    }

    /**
     * 添加点赞
     */
    private Mono<Boolean> addLike(Long userId, Like.TargetType targetType, String targetId,
                                 String likeSetKey, String userLikeKey, String countKey,
                                 Like.DeviceInfo deviceInfo, Like.Location location) {
        
        Instant now = Instant.now();
        
        return Mono.zip(
                // Redis操作：添加到点赞集合
                redisTemplate.opsForSet().add(likeSetKey, userId.toString()),
                // Redis操作：增加计数
                redisTemplate.opsForValue().increment(countKey),
                // Redis操作：添加到用户点赞记录
                redisTemplate.opsForSet().add(userLikeKey, targetType + ":" + targetId)
        )
        .flatMap(tuple -> {
            // 异步持久化到MongoDB
            Like like = Like.builder()
                    .userId(userId)
                    .targetType(targetType)
                    .targetId(targetId)
                    .status(Like.LikeStatus.ACTIVE)
                    .createdDate(now)
                    .lastModifiedDate(now)
                    .timeBucket(Like.generateTimeBucket(now))
                    .deviceInfo(deviceInfo)
                    .location(location)
                    .build();
            
            // 异步保存到MongoDB (不阻塞主流程)
            persistLikeAsync(like);
            
            // 发布点赞事件
            publishLikeEvent(userId, targetType, targetId, InteractionEvent.EventType.LIKE_ADDED);
            
            return Mono.just(true);
        })
        .onErrorReturn(false);
    }

    /**
     * 移除点赞
     */
    private Mono<Boolean> removeLike(Long userId, Like.TargetType targetType, String targetId,
                                    String likeSetKey, String userLikeKey, String countKey) {
        
        return Mono.zip(
                // Redis操作：从点赞集合移除
                redisTemplate.opsForSet().remove(likeSetKey, userId.toString()),
                // Redis操作：减少计数
                redisTemplate.opsForValue().decrement(countKey),
                // Redis操作：从用户点赞记录移除
                redisTemplate.opsForSet().remove(userLikeKey, targetType + ":" + targetId)
        )
        .flatMap(tuple -> {
            // 异步更新MongoDB状态
            updateLikeStatusAsync(userId, targetType, targetId, Like.LikeStatus.CANCELLED);
            
            // 发布取消点赞事件
            publishLikeEvent(userId, targetType, targetId, InteractionEvent.EventType.LIKE_REMOVED);
            
            return Mono.just(false);
        })
        .onErrorReturn(true);
    }

    /**
     * 获取点赞数量
     */
    public Mono<Long> getLikeCount(Like.TargetType targetType, String targetId) {
        String countKey = LIKE_COUNT_KEY_PREFIX + targetType + ":" + targetId;
        
        return redisTemplate.opsForValue()
                .get(countKey)
                .cast(Long.class)
                .switchIfEmpty(
                    // 如果Redis中没有，从MongoDB查询并缓存
                    countFromDatabase(targetType, targetId)
                        .flatMap(count -> redisTemplate.opsForValue()
                                .set(countKey, count, Duration.ofHours(1))
                                .thenReturn(count))
                );
    }

    /**
     * 检查用户是否已点赞
     */
    public Mono<Boolean> hasUserLiked(Long userId, Like.TargetType targetType, String targetId) {
        String likeSetKey = LIKE_SET_KEY_PREFIX + targetType + ":" + targetId;
        
        return redisTemplate.opsForSet()
                .isMember(likeSetKey, userId.toString())
                .switchIfEmpty(
                    // 如果Redis中没有，从MongoDB查询
                    checkFromDatabase(userId, targetType, targetId)
                );
    }

    /**
     * 获取点赞用户列表 (分页)
     */
    public Flux<Long> getLikeUsers(Like.TargetType targetType, String targetId, int page, int size) {
        String likeSetKey = LIKE_SET_KEY_PREFIX + targetType + ":" + targetId;
        
        return redisTemplate.opsForSet()
                .members(likeSetKey)
                .cast(String.class)
                .map(Long::valueOf)
                .skip((long) page * size)
                .take(size)
                .switchIfEmpty(
                    // 如果Redis中没有数据，从MongoDB查询
                    getLikeUsersFromDatabase(targetType, targetId, page, size)
                );
    }

    /**
     * 批量获取点赞状态
     */
    public Mono<Set<String>> getBatchLikeStatus(Long userId, Set<String> targetIds, Like.TargetType targetType) {
        String userLikeKey = USER_LIKE_KEY_PREFIX + userId;
        
        return redisTemplate.opsForSet()
                .members(userLikeKey)
                .cast(String.class)
                .filter(target -> {
                    String prefix = targetType + ":";
                    return target.startsWith(prefix) && 
                           targetIds.contains(target.substring(prefix.length()));
                })
                .map(target -> target.substring((targetType + ":").length()))
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * 异步持久化点赞数据
     */
    private void persistLikeAsync(Like like) {
        // 获取用户信息并保存
        userServiceClient.getUserInfo(like.getUserId())
                .doOnNext(userInfo -> {
                    like.setUsername(userInfo.getUsername());
                    like.setAvatar(userInfo.getAvatar());
                })
                .then(mongoTemplate.save(like))
                .subscribe(
                    saved -> log.debug("点赞数据已保存: {}", saved.getId()),
                    error -> log.error("点赞数据保存失败", error)
                );
    }

    /**
     * 异步更新点赞状态
     */
    private void updateLikeStatusAsync(Long userId, Like.TargetType targetType, String targetId, 
                                      Like.LikeStatus status) {
        Query query = Query.query(
                Criteria.where("userId").is(userId)
                        .and("targetType").is(targetType)
                        .and("targetId").is(targetId)
                        .and("status").is(Like.LikeStatus.ACTIVE)
        );
        
        Update update = Update.update("status", status)
                .set("lastModifiedDate", Instant.now());
        
        mongoTemplate.updateFirst(query, update, Like.class)
                .subscribe(
                    result -> log.debug("点赞状态已更新: affected={}", result.getModifiedCount()),
                    error -> log.error("点赞状态更新失败", error)
                );
    }

    /**
     * 从数据库查询点赞数量
     */
    private Mono<Long> countFromDatabase(Like.TargetType targetType, String targetId) {
        Query query = Query.query(
                Criteria.where("targetType").is(targetType)
                        .and("targetId").is(targetId)
                        .and("status").is(Like.LikeStatus.ACTIVE)
        );
        
        return mongoTemplate.count(query, Like.class);
    }

    /**
     * 从数据库检查用户点赞状态
     */
    private Mono<Boolean> checkFromDatabase(Long userId, Like.TargetType targetType, String targetId) {
        Query query = Query.query(
                Criteria.where("userId").is(userId)
                        .and("targetType").is(targetType)
                        .and("targetId").is(targetId)
                        .and("status").is(Like.LikeStatus.ACTIVE)
        );
        
        return mongoTemplate.exists(query, Like.class);
    }

    /**
     * 从数据库获取点赞用户列表
     */
    private Flux<Long> getLikeUsersFromDatabase(Like.TargetType targetType, String targetId, int page, int size) {
        Query query = Query.query(
                Criteria.where("targetType").is(targetType)
                        .and("targetId").is(targetId)
                        .and("status").is(Like.LikeStatus.ACTIVE)
        )
        .skip((long) page * size)
        .limit(size);
        
        return mongoTemplate.find(query, Like.class)
                .map(Like::getUserId);
    }

    /**
     * 发布点赞事件
     */
    private void publishLikeEvent(Long userId, Like.TargetType targetType, String targetId, 
                                 InteractionEvent.EventType eventType) {
        InteractionEvent event = InteractionEvent.builder()
                .eventType(eventType)
                .userId(userId)
                .targetType(targetType)
                .targetId(targetId)
                .priority(InteractionEvent.EventPriority.NORMAL)
                .build();
        
        event.setDefaults();
        
        eventPublishingService.publishEvent(event)
                .subscribe(
                    success -> log.debug("点赞事件已发布: {}", eventType),
                    error -> log.error("点赞事件发布失败", error)
                );
    }
}