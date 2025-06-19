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

package im.turms.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Turms API Gateway Application
 * 
 * 职责:
 * 1. 统一API入口管理
 * 2. 服务路由和负载均衡
 * 3. 认证授权和安全控制
 * 4. 限流熔断和降级
 * 5. 监控和日志记录
 * 6. 协议转换和数据格式化
 *
 * @author Turms Project
 */
@SpringBootApplication
@EnableDiscoveryClient
public class TurmsApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(TurmsApiGatewayApplication.class, args);
    }

}