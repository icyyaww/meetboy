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

package im.turms.apigateway.config;

import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

import java.time.Duration;

/**
 * API网关路由配置
 *
 * @author Turms Project
 */
@Configuration
public class GatewayConfig {

    /**
     * 配置路由规则
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // WebSocket路由 - turms-gateway
                .route("turms-websocket", r -> r
                        .path("/websocket/**")
                        .uri("ws://turms-gateway:10510"))
                
                // TCP代理路由 - turms-gateway  
                .route("turms-tcp", r -> r
                        .path("/tcp/**")
                        .uri("http://turms-gateway:9510"))
                
                // 即时通讯API路由 - turms-service
                .route("turms-im-api", r -> r
                        .path("/api/v1/im/**")
                        .filters(f -> f
                                .stripPrefix(3)
                                .circuitBreaker(c -> c
                                        .setName("turms-service")
                                        .setFallbackUri("forward:/fallback/turms-service"))
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(ipKeyResolver())))
                        .uri("lb://turms-service"))
                
                // 标签服务API路由
                .route("turms-tag-api", r -> r
                        .path("/api/v1/tags/**")
                        .filters(f -> f
                                .stripPrefix(3)
                                .circuitBreaker(c -> c
                                        .setName("turms-tag-service")
                                        .setFallbackUri("forward:/fallback/turms-tag-service"))
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(ipKeyResolver())))
                        .uri("lb://turms-tag-service"))
                
                // 社交关系服务API路由（预留）
                .route("turms-social-api", r -> r
                        .path("/api/v1/social/**")
                        .filters(f -> f
                                .stripPrefix(3)
                                .circuitBreaker(c -> c
                                        .setName("turms-social-service")
                                        .setFallbackUri("forward:/fallback/turms-social-service"))
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(ipKeyResolver())))
                        .uri("lb://turms-social-service"))
                
                // 内容服务API路由（预留）
                .route("turms-content-api", r -> r
                        .path("/api/v1/content/**")
                        .filters(f -> f
                                .stripPrefix(3)
                                .circuitBreaker(c -> c
                                        .setName("turms-content-service")
                                        .setFallbackUri("forward:/fallback/turms-content-service"))
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(ipKeyResolver())))
                        .uri("lb://turms-content-service"))
                
                // 互动服务API路由（预留）
                .route("turms-interaction-api", r -> r
                        .path("/api/v1/interaction/**")
                        .filters(f -> f
                                .stripPrefix(3)
                                .circuitBreaker(c -> c
                                        .setName("turms-interaction-service")
                                        .setFallbackUri("forward:/fallback/turms-interaction-service"))
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(ipKeyResolver())))
                        .uri("lb://turms-interaction-service"))
                
                // 推荐服务API路由（预留）
                .route("turms-recommendation-api", r -> r
                        .path("/api/v1/recommendation/**")
                        .filters(f -> f
                                .stripPrefix(3)
                                .circuitBreaker(c -> c
                                        .setName("turms-recommendation-service")
                                        .setFallbackUri("forward:/fallback/turms-recommendation-service"))
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(ipKeyResolver())))
                        .uri("lb://turms-recommendation-service"))
                
                // 管理API路由 - turms-admin
                .route("turms-admin-api", r -> r
                        .path("/admin/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("turms-admin")
                                        .setFallbackUri("forward:/fallback/turms-admin")))
                        .uri("lb://turms-admin"))
                
                .build();
    }

    /**
     * 基于IP的限流Key解析器
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                exchange.getRequest()
                        .getRemoteAddress()
                        .getAddress()
                        .getHostAddress());
    }

    /**
     * 基于用户的限流Key解析器
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .cast(String.class)
                .switchIfEmpty(Mono.just("anonymous"));
    }

    /**
     * Redis限流器配置
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // 每秒允许10个请求，突发容量20
        return new RedisRateLimiter(10, 20, 1);
    }

    /**
     * 熔断器配置
     */
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50.0f)
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        .slowCallRateThreshold(50.0f)
                        .slowCallDurationThreshold(Duration.ofSeconds(2))
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(10))
                        .build())
                .build());
    }

}