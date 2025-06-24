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

package im.turms.social.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

/**
 * 健康检查控制器
 * 
 * 提供服务健康状态检查和基本信息查询
 */
@RestController
@RequestMapping("/social")
public class HealthController {

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("Social Service is running");
    }

    /**
     * 服务信息端点
     */
    @GetMapping("/info")
    public Mono<String> info() {
        return Mono.just("Turms Social Service v1.0 - 社交关系管理服务");
    }

    /**
     * 版本信息端点
     */
    @GetMapping("/version")
    public Mono<String> version() {
        return Mono.just("1.0.0-SNAPSHOT");
    }
}