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

package im.turms.social.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

/**
 * 社交关系服务核心配置类
 * 
 * 负责配置社交关系服务的核心组件和路由
 */
@Configuration
public class SocialServiceConfiguration {

    /**
     * 配置社交关系服务的路由
     */
    @Bean
    public RouterFunction<ServerResponse> socialRoutes() {
        return RouterFunctions
            .route(GET("/social/health"), request -> 
                ServerResponse.ok().bodyValue("Social Service is running"))
            .andRoute(GET("/social/info"), request -> 
                ServerResponse.ok().bodyValue("Turms Social Service v1.0"));
    }

    /**
     * 社交关系服务属性配置
     */
    @Bean
    @ConfigurationProperties(prefix = "turms.social-service")
    public SocialServiceProperties socialServiceProperties() {
        return new SocialServiceProperties();
    }
}