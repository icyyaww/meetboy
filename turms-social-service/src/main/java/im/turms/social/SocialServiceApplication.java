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

package im.turms.social;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

/**
 * Turms社交关系服务独立应用启动类
 * 
 * 这是一个独立的社交关系系统服务，基于Spring Boot构建
 * 提供完整的社交网络功能、好友关系管理、群组管理等功能
 * 
 * 特性：
 * - 好友关系管理（添加、删除、黑名单、分组）
 * - 群组管理（创建、加入、退出、权限管理）
 * - 社交推荐（基于共同好友、兴趣标签的推荐算法）
 * - 社交互动（点赞、评论、分享、关注）
 * - 社交统计（好友数量、群组活跃度、互动数据）
 * - 关系链分析（朋友圈、社交图谱、影响力计算）
 */
@SpringBootApplication(exclude = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class
})
public class SocialServiceApplication {

    public static void main(String[] args) {
        System.out.println("正在启动Turms社交关系服务...");
        SpringApplication.run(SocialServiceApplication.class, args);
        System.out.println("Turms社交关系服务启动完成！");
    }
}