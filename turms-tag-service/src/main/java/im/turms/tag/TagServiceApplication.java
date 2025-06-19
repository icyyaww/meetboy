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

package im.turms.tag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

import im.turms.server.common.infra.logging.core.logger.Logger;
import im.turms.server.common.infra.logging.core.logger.LoggerFactory;

/**
 * Turms标签服务独立应用启动类
 * 
 * 这是一个独立的标签系统服务，基于Turms框架构建
 * 提供完整的标签管理、用户标签关系、标签推荐等功能
 * 
 * 特性：
 * - 多领域标签分类（星座、性取向、生活方式、兴趣爱好等）
 * - 自定义标签功能
 * - 标签内容管理
 * - 个性化标签推荐
 * - 标签云展示（按热度）
 * - 标签相关内容/群组展示
 */
@SpringBootApplication(exclude = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class
})
public class TagServiceApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(TagServiceApplication.class);

    public static void main(String[] args) {
        LOGGER.info("正在启动Turms标签服务...");
        SpringApplication.run(TagServiceApplication.class, args);
        LOGGER.info("Turms标签服务启动完成！");
    }
}