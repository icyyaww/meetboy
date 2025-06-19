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

package im.turms.tag.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import im.turms.server.common.infra.logging.core.logger.Logger;
import im.turms.server.common.infra.logging.core.logger.LoggerFactory;

import jakarta.annotation.PostConstruct;

/**
 * 标签服务核心配置类
 */
@Configuration
@EnableConfigurationProperties(TagServiceProperties.class)
public class TagServiceConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(TagServiceConfiguration.class);

    private final TagServiceProperties properties;

    public TagServiceConfiguration(TagServiceProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        LOGGER.info("Turms标签服务配置初始化完成");
        LOGGER.info("服务端口: {}", properties.getPort());
        LOGGER.info("数据库: {}:{}/{}", 
                properties.getMongo().getHost(),
                properties.getMongo().getPort(),
                properties.getMongo().getDatabase());
        LOGGER.info("标签功能: {}", properties.getTag().isEnabled() ? "启用" : "禁用");
        
        if (properties.getTag().isEnabled()) {
            LOGGER.info("支持的功能:");
            LOGGER.info("- 多领域标签分类");
            LOGGER.info("- 用户自定义标签: {}", properties.getTag().isAllowCustomTags() ? "启用" : "禁用");
            LOGGER.info("- 标签推荐: {}", properties.getTag().getRecommendation().isEnabled() ? "启用" : "禁用");
            LOGGER.info("- 匿名访问: {}", properties.getTag().isAllowAnonymousAccess() ? "允许" : "禁止");
        }
    }
}