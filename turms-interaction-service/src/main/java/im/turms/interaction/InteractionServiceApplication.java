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

package im.turms.interaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.kafka.annotation.EnableKafka;
import reactor.core.publisher.Hooks;

/**
 * Turms 互动服务应用启动类
 * 
 * 主要功能：
 * - 高并发点赞系统
 * - 评论流式处理
 * - 实时互动事件
 * - 用户行为统计
 * - 朋友圈内容管理 (已集成)
 * - 智能内容审核 (已集成)
 */
@SpringBootApplication
@EnableJpaRepositories
@EnableReactiveMongoRepositories
@EnableScheduling
@EnableKafka
public class InteractionServiceApplication {

    public static void main(String[] args) {
        // 启用Reactor调试模式 (开发环境)
        Hooks.onOperatorDebug();
        
        System.out.println("正在启动Turms互动服务...");
        System.out.println("功能模块: 高并发点赞, 评论流式处理, 实时互动");
        
        SpringApplication.run(InteractionServiceApplication.class, args);
        
        System.out.println("Turms互动服务启动完成!");
    }
}