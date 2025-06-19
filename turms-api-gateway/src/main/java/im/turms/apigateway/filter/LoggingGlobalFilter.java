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

package im.turms.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 全局日志过滤器
 *
 * @author Turms Project
 */
@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {
    
    private static final Logger log = LoggerFactory.getLogger(LoggingGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        final long startTime = System.currentTimeMillis();
        
        String requestId = request.getHeaders().getFirst("X-Request-ID");
        if (requestId == null) {
            requestId = generateRequestId();
        }
        
        final String finalRequestId = requestId;
        
        log.info("Gateway Request: {} {} - Request ID: {}", 
                request.getMethod(), 
                request.getURI(), 
                finalRequestId);

        return chain.filter(exchange).then(
                Mono.fromRunnable(() -> {
                    ServerHttpResponse response = exchange.getResponse();
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    
                    log.info("Gateway Response: {} {} - Status: {} - Duration: {}ms - Request ID: {}", 
                            request.getMethod(),
                            request.getURI(),
                            response.getStatusCode(),
                            duration,
                            finalRequestId);
                })
        );
    }

    @Override
    public int getOrder() {
        return -1; // 最高优先级
    }

    private String generateRequestId() {
        return "REQ-" + System.currentTimeMillis() + "-" + 
               (int)(Math.random() * 1000);
    }

}