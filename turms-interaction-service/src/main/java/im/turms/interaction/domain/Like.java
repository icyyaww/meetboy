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

package im.turms.interaction.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;

import java.time.Instant;

/**
 * 点赞实体 - 优化高并发场景
 * 
 * 设计要点：
 * - 复合索引优化查询性能
 * - 分片键设计支持水平扩展
 * - 时间分桶减少热点数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("like")
@CompoundIndexes({
    @CompoundIndex(name = "target_user_idx", def = "{'targetType': 1, 'targetId': 1, 'userId': 1}"),
    @CompoundIndex(name = "user_time_idx", def = "{'userId': 1, 'createdDate': -1}"),
    @CompoundIndex(name = "target_time_idx", def = "{'targetType': 1, 'targetId': 1, 'createdDate': -1}")
})
public class Like {

    @Id
    private String id;

    /**
     * 点赞用户ID (来自turms-service)
     */
    @Field("user_id")
    @Indexed
    private Long userId;

    /**
     * 用户名 (缓存字段)
     */
    @Field("username")
    private String username;

    /**
     * 用户头像 (缓存字段)
     */
    @Field("avatar")
    private String avatar;

    /**
     * 目标类型 (分片键的一部分)
     */
    @Field("target_type")
    @Indexed
    private TargetType targetType;

    /**
     * 目标ID (分片键的一部分)
     */
    @Field("target_id")
    @Indexed
    private String targetId;

    /**
     * 点赞状态
     */
    @Field("status")
    private LikeStatus status;

    /**
     * 创建时间 (用于时间分桶)
     */
    @Field("created_date")
    @Indexed
    private Instant createdDate;

    /**
     * 最后修改时间
     */
    @Field("last_modified_date")
    private Instant lastModifiedDate;

    /**
     * 时间分桶键 (年月日小时)
     * 用于减少热点数据，提高查询性能
     */
    @Field("time_bucket")
    @Indexed
    private String timeBucket;

    /**
     * 设备信息
     */
    @Field("device_info")
    private DeviceInfo deviceInfo;

    /**
     * 地理位置 (可选)
     */
    @Field("location")
    private Location location;

    /**
     * 扩展数据
     */
    @Field("metadata")
    private Object metadata;

    /**
     * 目标类型枚举
     */
    public enum TargetType {
        MOMENT,         // 朋友圈动态
        COMMENT,        // 评论
        VIDEO,          // 视频
        ARTICLE,        // 文章
        USER,           // 用户
        LIVE_STREAM,    // 直播
        SHORT_VIDEO     // 短视频
    }

    /**
     * 点赞状态枚举
     */
    public enum LikeStatus {
        ACTIVE,         // 有效点赞
        CANCELLED,      // 已取消
        EXPIRED         // 已过期
    }

    /**
     * 设备信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceInfo {
        private String deviceId;
        private String deviceType;    // mobile, desktop, tablet
        private String platform;     // iOS, Android, Web
        private String appVersion;
        private String userAgent;
    }

    /**
     * 地理位置信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        private Double longitude;
        private Double latitude;
        private String country;
        private String province;
        private String city;
        private String district;
    }

    /**
     * 生成时间分桶键
     * 格式: YYYY-MM-DD-HH
     */
    public static String generateTimeBucket(Instant instant) {
        return instant.toString().substring(0, 13);
    }

    /**
     * 生成分片键
     * 组合: targetType + targetId + timeBucket
     */
    public String generateShardKey() {
        return targetType + ":" + targetId + ":" + timeBucket;
    }
}