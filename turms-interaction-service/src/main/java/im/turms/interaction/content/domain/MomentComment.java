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
import java.util.Set;

/**
 * 朋友圈评论实体 (已迁移到interaction服务)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("moment_comment")
public class MomentComment {

    @Id
    private String id;

    /**
     * 所属动态ID
     */
    @Field("moment_id")
    private String momentId;

    /**
     * 评论者用户ID
     */
    @Field("user_id")
    private Long userId;

    /**
     * 评论者用户名 (缓存字段)
     */
    @Field("username")
    private String username;

    /**
     * 评论者头像 (缓存字段)
     */
    @Field("avatar")
    private String avatar;

    /**
     * 评论内容
     */
    @Field("content")
    private String content;

    /**
     * 父评论ID (回复评论时使用)
     */
    @Field("parent_id")
    private String parentId;

    /**
     * 根评论ID (用于层级评论)
     */
    @Field("root_id")
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
     * 点赞用户ID列表
     */
    @Field("likes")
    private Set<Long> likes;

    /**
     * 点赞数量
     */
    @Field("like_count")
    private Integer likeCount;

    /**
     * 子评论数量
     */
    @Field("reply_count")
    private Integer replyCount;

    /**
     * 内容审核状态
     */
    @Field("moderation_status")
    private Moment.ModerationStatus moderationStatus;

    /**
     * 审核结果
     */
    @Field("moderation_result")
    private im.turms.interaction.content.domain.ModerationResult moderationResult;

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
}