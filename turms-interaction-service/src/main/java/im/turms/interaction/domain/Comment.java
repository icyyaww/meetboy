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

import im.turms.interaction.dto.ModerationResult;
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
import java.util.List;
import java.util.Set;

/**
 * 评论实体 - 支持流式处理和树形结构
 * 
 * 设计要点：
 * - 支持多级嵌套评论
 * - 流式数据处理优化
 * - 实时评论推送
 * - 评论审核状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("comment")
@CompoundIndexes({
    @CompoundIndex(name = "target_time_idx", def = "{'targetType': 1, 'targetId': 1, 'createdDate': -1}"),
    @CompoundIndex(name = "parent_time_idx", def = "{'parentId': 1, 'createdDate': 1}"),
    @CompoundIndex(name = "user_time_idx", def = "{'userId': 1, 'createdDate': -1}"),
    @CompoundIndex(name = "stream_idx", def = "{'targetType': 1, 'targetId': 1, 'status': 1, 'createdDate': -1}")
})
public class Comment {

    @Id
    private String id;

    /**
     * 评论用户ID (来自turms-service)
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
     * 目标类型
     */
    @Field("target_type")
    @Indexed
    private Like.TargetType targetType;

    /**
     * 目标ID
     */
    @Field("target_id")
    @Indexed
    private String targetId;

    /**
     * 父评论ID (用于构建评论树)
     */
    @Field("parent_id")
    @Indexed
    private String parentId;

    /**
     * 根评论ID (用于快速定位顶级评论)
     */
    @Field("root_id")
    @Indexed
    private String rootId;

    /**
     * 回复目标用户ID
     */
    @Field("reply_to_user_id")
    private Long replyToUserId;

    /**
     * 回复目标用户名
     */
    @Field("reply_to_username")
    private String replyToUsername;

    /**
     * 评论内容
     */
    @Field("content")
    private String content;

    /**
     * 评论类型
     */
    @Field("type")
    private CommentType type;

    /**
     * 评论状态
     */
    @Field("status")
    @Indexed
    private CommentStatus status;

    /**
     * 审核结果
     */
    @Field("moderation_result")
    private ModerationResult moderationResult;

    /**
     * 附件列表
     */
    @Field("attachments")
    private List<CommentAttachment> attachments;

    /**
     * 点赞数量
     */
    @Field("like_count")
    private Integer likeCount;

    /**
     * 回复数量
     */
    @Field("reply_count")
    private Integer replyCount;

    /**
     * 点赞用户ID列表 (热点数据缓存)
     */
    @Field("liked_users")
    private Set<Long> likedUsers;

    /**
     * 评论层级 (0为顶级评论)
     */
    @Field("level")
    @Indexed
    private Integer level;

    /**
     * 排序权重 (用于热门评论排序)
     */
    @Field("sort_weight")
    private Double sortWeight;

    /**
     * 创建时间
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
     * 时间分桶键
     */
    @Field("time_bucket")
    @Indexed
    private String timeBucket;

    /**
     * 流式处理序号 (用于分布式流处理)
     */
    @Field("stream_sequence")
    @Indexed
    private Long streamSequence;

    /**
     * 设备信息
     */
    @Field("device_info")
    private DeviceInfo deviceInfo;

    /**
     * 地理位置
     */
    @Field("location")
    private Location location;

    /**
     * 扩展数据
     */
    @Field("metadata")
    private Object metadata;

    /**
     * 评论类型枚举
     */
    public enum CommentType {
        TEXT,           // 纯文本
        IMAGE,          // 图片评论
        VOICE,          // 语音评论
        VIDEO,          // 视频评论
        EMOJI,          // 表情评论
        STICKER,        // 贴纸评论
        MIXED           // 混合内容
    }

    /**
     * 评论状态枚举
     */
    public enum CommentStatus {
        PENDING,        // 待审核
        APPROVED,       // 审核通过
        REJECTED,       // 审核拒绝
        DELETED,        // 已删除
        HIDDEN,         // 已隐藏
        FLAGGED         // 被举报
    }


    /**
     * 评论附件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentAttachment {
        private String id;
        private AttachmentType type;
        private String url;
        private String thumbnailUrl;
        private Long size;
        private String mimeType;
        private Integer width;
        private Integer height;
        private Integer duration;  // 视频/音频时长(秒)

        public enum AttachmentType {
            IMAGE, VIDEO, VOICE, GIF, STICKER
        }
    }

    /**
     * 计算评论排序权重
     * 基于点赞数、回复数、时间等因素
     */
    public void calculateSortWeight() {
        long ageHours = (Instant.now().toEpochMilli() - createdDate.toEpochMilli()) / (1000 * 60 * 60);
        double timeFactor = 1.0 / (1.0 + ageHours * 0.1);
        
        this.sortWeight = (likeCount * 2.0 + replyCount * 1.5) * timeFactor;
    }

    /**
     * 生成流式处理分区键
     */
    public String generateStreamPartitionKey() {
        return targetType + ":" + (targetId.hashCode() % 10);
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
}