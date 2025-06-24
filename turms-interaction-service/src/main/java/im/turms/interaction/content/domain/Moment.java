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

package im.turms.interaction.content.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * 朋友圈动态实体 (已迁移到interaction服务)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("moment")
public class Moment {

    @Id
    private String id;

    /**
     * 发布者用户ID (来自turms-service)
     */
    @Field("user_id")
    private Long userId;

    /**
     * 发布者用户名 (缓存字段)
     */
    @Field("username")
    private String username;

    /**
     * 发布者头像 (缓存字段)
     */
    @Field("avatar")
    private String avatar;

    /**
     * 动态内容文本
     */
    @Field("content")
    private String content;

    /**
     * 多媒体附件列表
     */
    @Field("attachments")
    private List<MomentAttachment> attachments;

    /**
     * 动态类型
     */
    @Field("type")
    private MomentType type;

    /**
     * 隐私设置
     */
    @Field("privacy")
    private MomentPrivacy privacy;

    /**
     * 可见用户ID列表 (privacy=CUSTOM时使用)
     */
    @Field("visible_users")
    private Set<Long> visibleUsers;

    /**
     * 不可见用户ID列表
     */
    @Field("invisible_users")
    private Set<Long> invisibleUsers;

    /**
     * 地理位置信息
     */
    @Field("location")
    private MomentLocation location;

    /**
     * 点赞数量 (与interaction-service集成)
     */
    @Field("like_count")
    private Integer likeCount;

    /**
     * 评论数量 (与interaction-service集成)
     */
    @Field("comment_count")
    private Integer commentCount;

    /**
     * 分享数量
     */
    @Field("share_count")
    private Integer shareCount;

    /**
     * 内容审核状态
     */
    @Field("moderation_status")
    private ModerationStatus moderationStatus;

    /**
     * 审核结果详情
     */
    @Field("moderation_result")
    private im.turms.interaction.content.domain.ModerationResult moderationResult;

    /**
     * 是否置顶
     */
    @Field("pinned")
    private Boolean pinned;

    /**
     * 创建时间
     */
    @Field("created_date")
    private Instant createdDate;

    /**
     * 最后修改时间
     */
    @Field("last_modified_date")
    private Instant lastModifiedDate;

    /**
     * 动态过期时间 (可选)
     */
    @Field("expires_at")
    private Instant expiresAt;

    /**
     * 扩展属性
     */
    @Field("extra_data")
    private Object extraData;

    /**
     * 动态类型枚举
     */
    public enum MomentType {
        TEXT,           // 纯文本
        IMAGE,          // 图片
        VIDEO,          // 视频
        AUDIO,          // 音频
        LINK,           // 链接分享
        MIXED           // 混合内容
    }

    /**
     * 隐私设置枚举
     */
    public enum MomentPrivacy {
        PUBLIC,         // 公开
        FRIENDS_ONLY,   // 仅好友可见
        CUSTOM,         // 自定义可见
        PRIVATE         // 仅自己可见
    }

    /**
     * 内容审核状态枚举
     */
    public enum ModerationStatus {
        PENDING,        // 待审核
        APPROVED,       // 审核通过
        REJECTED,       // 审核拒绝
        REVIEW_NEEDED   // 需要人工审核
    }
}