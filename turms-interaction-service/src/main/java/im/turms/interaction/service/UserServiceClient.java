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

import im.turms.interaction.dto.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 用户服务客户端 - 与turms-service集成
 * 
 * 功能：
 * - 获取用户基本信息
 * - 缓存用户数据
 * - 批量用户查询
 * - 用户关系检查
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient.Builder webClientBuilder;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    @Value("${turms.interaction-service.integration.turms-service.base-url:http://localhost:8510}")
    private String turmsServiceBaseUrl;

    private static final String USER_CACHE_KEY_PREFIX = "user:info:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    /**
     * 获取用户信息 (带缓存)
     */
    public Mono<UserInfo> getUserInfo(Long userId) {
        String cacheKey = USER_CACHE_KEY_PREFIX + userId;
        
        return redisTemplate.opsForValue()
                .get(cacheKey)
                .cast(Map.class)
                .map(this::mapToUserInfo)
                .switchIfEmpty(
                    // 缓存未命中，从turms-service获取
                    fetchUserFromService(userId)
                            .doOnNext(userInfo -> cacheUserInfo(cacheKey, userInfo))
                );
    }

    /**
     * 批量获取用户信息
     */
    public Mono<Map<Long, UserInfo>> getBatchUserInfo(java.util.Set<Long> userIds) {
        return reactor.core.publisher.Flux.fromIterable(userIds)
                .flatMap(this::getUserInfo)
                .collectMap(UserInfo::getUserId)
                .doOnNext(userMap -> log.debug("批量获取用户信息: count={}", userMap.size()));
    }

    /**
     * 检查用户关系 (是否为好友)
     */
    public Mono<Boolean> areUsersFriends(Long userId1, Long userId2) {
        WebClient webClient = webClientBuilder
                .baseUrl(turmsServiceBaseUrl)
                .build();
        
        return webClient.get()
                .uri("/admin/user-relationship/friends/{userId1}/{userId2}", userId1, userId2)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Object areFriends = response.get("areFriends");
                    return Boolean.TRUE.equals(areFriends);
                })
                .onErrorReturn(false)
                .retryWhen(Retry.backoff(2, Duration.ofMillis(200)))
                .doOnError(error -> log.error("检查用户关系失败: userId1={}, userId2={}", userId1, userId2, error));
    }

    /**
     * 获取用户好友列表
     */
    public Flux<Long> getUserFriends(Long userId) {
        WebClient webClient = webClientBuilder
                .baseUrl(turmsServiceBaseUrl)
                .build();
        
        return webClient.get()
                .uri("/admin/user-relationship/friends/{userId}", userId)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMapMany(response -> {
                    Object friendsData = response.get("friends");
                    if (friendsData instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Number> friendIds = (List<Number>) friendsData;
                        return Flux.fromIterable(friendIds)
                                .map(Number::longValue);
                    }
                    return Flux.empty();
                })
                .onErrorResume(error -> {
                    log.error("获取用户好友列表失败: userId={}", userId, error);
                    return Flux.empty(); // 返回空列表作为降级方案
                })
                .retryWhen(Retry.backoff(2, Duration.ofMillis(200)))
                .doOnComplete(() -> log.debug("获取用户好友列表完成: userId={}", userId));
    }

    /**
     * 检查用户是否存在
     */
    public Mono<Boolean> userExists(Long userId) {
        return getUserInfo(userId)
                .map(userInfo -> true)
                .onErrorReturn(false);
    }

    /**
     * 从turms-service获取用户信息
     */
    private Mono<UserInfo> fetchUserFromService(Long userId) {
        WebClient webClient = webClientBuilder
                .baseUrl(turmsServiceBaseUrl)
                .build();
        
        return webClient.get()
                .uri("/admin/user/{userId}", userId)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToUserInfo)
                .retryWhen(Retry.backoff(2, Duration.ofMillis(200)))
                .doOnError(error -> log.error("获取用户信息失败: userId={}", userId, error))
                .onErrorReturn(createDefaultUserInfo(userId));
    }

    /**
     * 缓存用户信息
     */
    private void cacheUserInfo(String cacheKey, UserInfo userInfo) {
        Map<String, Object> cacheData = Map.of(
                "userId", userInfo.getUserId(),
                "username", userInfo.getUsername() != null ? userInfo.getUsername() : "",
                "avatar", userInfo.getAvatar() != null ? userInfo.getAvatar() : "",
                "nickname", userInfo.getNickname() != null ? userInfo.getNickname() : "",
                "status", userInfo.getStatus() != null ? userInfo.getStatus() : "ACTIVE"
        );
        
        redisTemplate.opsForValue()
                .set(cacheKey, cacheData, CACHE_TTL)
                .subscribe(
                    success -> log.debug("用户信息已缓存: userId={}", userInfo.getUserId()),
                    error -> log.error("用户信息缓存失败: userId={}", userInfo.getUserId(), error)
                );
    }

    /**
     * 将Map转换为UserInfo
     */
    private UserInfo mapToUserInfo(Map<String, Object> data) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(((Number) data.get("userId")).longValue());
        userInfo.setUsername((String) data.get("username"));
        userInfo.setAvatar((String) data.get("avatar"));
        userInfo.setNickname((String) data.get("nickname"));
        userInfo.setStatus((String) data.get("status"));
        return userInfo;
    }

    /**
     * 创建默认用户信息 (服务不可用时的降级方案)
     */
    private UserInfo createDefaultUserInfo(Long userId) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setUsername("用户" + userId);
        userInfo.setAvatar("");
        userInfo.setNickname("用户" + userId);
        userInfo.setStatus("UNKNOWN");
        return userInfo;
    }

    /**
     * 清除用户缓存 (用户信息更新时调用)
     */
    public Mono<Boolean> clearUserCache(Long userId) {
        String cacheKey = USER_CACHE_KEY_PREFIX + userId;
        
        return redisTemplate.delete(cacheKey)
                .map(deleted -> deleted > 0)
                .doOnNext(success -> {
                    if (success) {
                        log.debug("用户缓存已清除: userId={}", userId);
                    }
                });
    }

    /**
     * 预热用户缓存 (批量预加载热门用户)
     */
    public Mono<Long> warmupUserCache(java.util.Set<Long> userIds) {
        return reactor.core.publisher.Flux.fromIterable(userIds)
                .flatMap(this::getUserInfo)
                .count()
                .doOnSuccess(count -> log.info("用户缓存预热完成: count={}", count));
    }
}