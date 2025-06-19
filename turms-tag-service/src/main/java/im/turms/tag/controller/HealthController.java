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

package im.turms.tag.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import im.turms.tag.config.TagServiceProperties;

/**
 * 健康检查控制器
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final TagServiceProperties properties;

    public HealthController(TagServiceProperties properties) {
        this.properties = properties;
    }

    /**
     * 基础健康检查
     */
    @GetMapping
    public Mono<Map<String, Object>> health() {
        return Mono.just(Map.of(
                "status", "UP",
                "service", "turms-tag-service",
                "version", "0.10.0-SNAPSHOT",
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "port", properties.getPort(),
                "nodeType", properties.getNodeType()
        ));
    }

    /**
     * 详细状态信息
     */
    @GetMapping("/info")
    public Mono<Map<String, Object>> info() {
        return Mono.just(Map.of(
                "application", Map.of(
                        "name", "turms-tag-service",
                        "description", "Turms标签系统独立服务",
                        "version", "0.10.0-SNAPSHOT"
                ),
                "features", Map.of(
                        "tag-management", "标签管理",
                        "user-tag-relations", "用户标签关系",
                        "tag-recommendations", "标签推荐",
                        "tag-cloud", "标签云",
                        "content-discovery", "内容发现"
                ),
                "configuration", Map.of(
                        "tagEnabled", properties.getTag().isEnabled(),
                        "customTagsAllowed", properties.getTag().isAllowCustomTags(),
                        "recommendationEnabled", properties.getTag().getRecommendation().isEnabled(),
                        "anonymousAccess", properties.getTag().isAllowAnonymousAccess(),
                        "maxTagsPerUser", properties.getTag().getMaxTagsPerUser()
                )
        ));
    }
}