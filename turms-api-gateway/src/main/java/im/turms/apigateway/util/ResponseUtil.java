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

package im.turms.apigateway.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 响应工具类
 *
 * @author Turms Project
 */
public class ResponseUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 返回JSON响应
     */
    public static Mono<Void> writeResponse(ServerHttpResponse response, HttpStatus status, Object body) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        try {
            String json = objectMapper.writeValueAsString(body);
            DataBuffer buffer = response.bufferFactory().wrap(json.getBytes());
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return response.setComplete();
        }
    }

    /**
     * 返回错误响应
     */
    public static Mono<Void> writeErrorResponse(ServerHttpResponse response, HttpStatus status, String message) {
        Map<String, Object> errorBody = Map.of(
                "error", status.getReasonPhrase(),
                "message", message,
                "timestamp", LocalDateTime.now(),
                "status", status.value()
        );
        return writeResponse(response, status, errorBody);
    }

    /**
     * 返回认证失败响应
     */
    public static Mono<Void> writeUnauthorizedResponse(ServerHttpResponse response) {
        return writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "认证失败，请提供有效的访问令牌");
    }

    /**
     * 返回权限不足响应
     */
    public static Mono<Void> writeForbiddenResponse(ServerHttpResponse response) {
        return writeErrorResponse(response, HttpStatus.FORBIDDEN, "权限不足，无法访问该资源");
    }

    /**
     * 返回限流响应
     */
    public static Mono<Void> writeRateLimitResponse(ServerHttpResponse response) {
        return writeErrorResponse(response, HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁，请稍后重试");
    }

    /**
     * 返回服务不可用响应
     */
    public static Mono<Void> writeServiceUnavailableResponse(ServerHttpResponse response, String serviceName) {
        return writeErrorResponse(response, HttpStatus.SERVICE_UNAVAILABLE, 
                "服务 " + serviceName + " 暂时不可用，请稍后重试");
    }

}