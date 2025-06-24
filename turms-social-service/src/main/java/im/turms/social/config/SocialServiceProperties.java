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

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 社交关系服务配置属性
 * 
 * 包含社交关系服务的所有可配置属性
 */
@Data
@Accessors(chain = true)
public class SocialServiceProperties {

    /**
     * 节点类型
     */
    private String nodeType = "social-service";

    /**
     * 服务端口
     */
    private int port = 8086;

    /**
     * MongoDB配置
     */
    private MongoProperties mongo = new MongoProperties();

    /**
     * 社交关系功能配置
     */
    private SocialProperties social = new SocialProperties();

    @Data
    @Accessors(chain = true)
    public static class MongoProperties {
        private String host = "localhost";
        private int port = 27017;
        private String database = "turms-social";
        private String username = "";
        private String password = "";
        private ConnectionPoolProperties connectionPool = new ConnectionPoolProperties();

        @Data
        @Accessors(chain = true)
        public static class ConnectionPoolProperties {
            private int minSize = 0;
            private int maxSize = 64;
            private int maxWaitTime = 60000;
            private int maxConnectionLifetime = 0;
            private int maxConnectionIdleTime = 0;
        }
    }

    @Data
    @Accessors(chain = true)
    public static class SocialProperties {
        /**
         * 基础功能配置
         */
        private boolean enabled = true;
        private int maxFriendsPerUser = 1000;
        private int maxGroupsPerUser = 100;
        private int maxGroupMembers = 500;
        private boolean allowAnonymousAccess = false;

        /**
         * 推荐功能配置
         */
        private RecommendationProperties recommendation = new RecommendationProperties();

        /**
         * 性能配置
         */
        private PerformanceProperties performance = new PerformanceProperties();

        @Data
        @Accessors(chain = true)
        public static class RecommendationProperties {
            private boolean enabled = true;
            private int maxRecommendedFriends = 20;
            private int maxRecommendedGroups = 10;
            private String algorithmType = "hybrid";
            private int cacheTimeMinutes = 30;
        }

        @Data
        @Accessors(chain = true)
        public static class PerformanceProperties {
            private int defaultPageSize = 20;
            private int maxPageSize = 100;
            private int maxBatchSize = 1000;
            private boolean enableCache = true;
            private int cacheExpireMinutes = 15;
        }
    }
}