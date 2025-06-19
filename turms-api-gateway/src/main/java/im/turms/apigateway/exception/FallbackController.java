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

package im.turms.apigateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 熔断降级控制器
 *
 * @author Turms Project
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/turms-service")
    public ResponseEntity<Map<String, Object>> turmsServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "SERVICE_UNAVAILABLE",
                        "message", "即时通讯服务暂时不可用，请稍后重试",
                        "service", "turms-service",
                        "timestamp", LocalDateTime.now(),
                        "suggestion", "请检查网络连接或联系技术支持"
                ));
    }

    @GetMapping("/turms-tag-service")
    public ResponseEntity<Map<String, Object>> turmsTagServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "SERVICE_UNAVAILABLE",
                        "message", "标签服务暂时不可用，请稍后重试",
                        "service", "turms-tag-service",
                        "timestamp", LocalDateTime.now(),
                        "suggestion", "请检查网络连接或联系技术支持"
                ));
    }

    @GetMapping("/turms-social-service")
    public ResponseEntity<Map<String, Object>> turmsSocialServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "SERVICE_UNAVAILABLE",
                        "message", "社交关系服务暂时不可用，请稍后重试",
                        "service", "turms-social-service",
                        "timestamp", LocalDateTime.now(),
                        "suggestion", "请检查网络连接或联系技术支持"
                ));
    }

    @GetMapping("/turms-content-service")
    public ResponseEntity<Map<String, Object>> turmsContentServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "SERVICE_UNAVAILABLE",
                        "message", "内容服务暂时不可用，请稍后重试",
                        "service", "turms-content-service",
                        "timestamp", LocalDateTime.now(),
                        "suggestion", "请检查网络连接或联系技术支持"
                ));
    }

    @GetMapping("/turms-interaction-service")
    public ResponseEntity<Map<String, Object>> turmsInteractionServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "SERVICE_UNAVAILABLE",
                        "message", "互动服务暂时不可用，请稍后重试",
                        "service", "turms-interaction-service",
                        "timestamp", LocalDateTime.now(),
                        "suggestion", "请检查网络连接或联系技术支持"
                ));
    }

    @GetMapping("/turms-recommendation-service")
    public ResponseEntity<Map<String, Object>> turmsRecommendationServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "SERVICE_UNAVAILABLE",
                        "message", "推荐服务暂时不可用，请稍后重试",
                        "service", "turms-recommendation-service",
                        "timestamp", LocalDateTime.now(),
                        "suggestion", "请检查网络连接或联系技术支持"
                ));
    }

    @GetMapping("/turms-admin")
    public ResponseEntity<Map<String, Object>> turmsAdminFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "SERVICE_UNAVAILABLE",
                        "message", "管理服务暂时不可用，请稍后重试",
                        "service", "turms-admin",
                        "timestamp", LocalDateTime.now(),
                        "suggestion", "请检查网络连接或联系技术支持"
                ));
    }

}