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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import lombok.Data;

/**
 * 标签服务配置属性
 */
@Data
@ConfigurationProperties(prefix = "turms.tag-service")
public class TagServiceProperties {

    /**
     * 节点类型
     */
    private String nodeType = "tag-service";

    /**
     * 服务端口
     */
    private Integer port = 8085;

    /**
     * MongoDB配置
     */
    @NestedConfigurationProperty
    private MongoProperties mongo = new MongoProperties();

    /**
     * 标签功能配置
     */
    @NestedConfigurationProperty
    private TagProperties tag = new TagProperties();

    @Data
    public static class MongoProperties {
        private String host = "localhost";
        private Integer port = 27017;
        private String database = "turms-tag";
        private String username;
        private String password;
        
        @NestedConfigurationProperty
        private ConnectionPoolProperties connectionPool = new ConnectionPoolProperties();
        
        @Data
        public static class ConnectionPoolProperties {
            private Integer minSize = 0;
            private Integer maxSize = 64;
            private Long maxWaitTime = 60000L;
            private Long maxConnectionLifetime = 0L;
            private Long maxConnectionIdleTime = 0L;
        }
    }

    @Data
    public static class TagProperties {
        
        /**
         * 是否启用标签功能
         */
        private boolean enabled = true;

        /**
         * 标签名称最大长度
         */
        private int maxTagNameLength = 50;

        /**
         * 标签描述最大长度
         */
        private int maxTagDescriptionLength = 200;

        /**
         * 每个用户最大标签数量
         */
        private int maxTagsPerUser = 100;

        /**
         * 是否允许用户创建自定义标签
         */
        private boolean allowCustomTags = true;

        /**
         * 是否允许匿名访问公开标签
         */
        private boolean allowAnonymousAccess = false;

        /**
         * 推荐功能配置
         */
        @NestedConfigurationProperty
        private RecommendationProperties recommendation = new RecommendationProperties();

        /**
         * 性能配置
         */
        @NestedConfigurationProperty
        private PerformanceProperties performance = new PerformanceProperties();

        @Data
        public static class RecommendationProperties {
            
            /**
             * 是否启用推荐功能
             */
            private boolean enabled = true;

            /**
             * 相似用户查找数量限制
             */
            private int maxSimilarUsers = 50;

            /**
             * 推荐标签数量限制
             */
            private int maxRecommendedTags = 20;

            /**
             * 标签云最大显示数量
             */
            private int maxTagCloudSize = 100;

            /**
             * 推荐算法类型
             * - collaborative_filtering: 协同过滤
             * - content_based: 基于内容
             * - hybrid: 混合算法
             */
            private String algorithmType = "hybrid";

            /**
             * 推荐结果缓存时间（分钟）
             */
            private int cacheTimeMinutes = 30;
        }

        @Data
        public static class PerformanceProperties {
            
            /**
             * 标签查询默认分页大小
             */
            private int defaultPageSize = 20;

            /**
             * 标签查询最大分页大小
             */
            private int maxPageSize = 100;

            /**
             * 批量操作最大数量
             */
            private int maxBatchSize = 1000;

            /**
             * 是否启用查询缓存
             */
            private boolean enableCache = true;

            /**
             * 缓存过期时间（分钟）
             */
            private int cacheExpireMinutes = 15;
        }
    }
}