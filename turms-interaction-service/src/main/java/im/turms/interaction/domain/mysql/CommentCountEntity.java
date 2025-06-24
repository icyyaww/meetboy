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

package im.turms.interaction.domain.mysql;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * 评论计数实体 - 文章评论统计
 */
@Entity
@Table(name = "comment_counts",
       indexes = {
           @Index(name = "idx_count_sync", columnList = "comment_count, last_sync_at"),
           @Index(name = "idx_last_comment", columnList = "last_comment_at DESC")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentCountEntity {

    @Id
    @Column(name = "article_id", nullable = false, length = 100)
    private String articleId;

    @Builder.Default
    @Column(name = "comment_count", nullable = false, columnDefinition = "INT UNSIGNED DEFAULT 0")
    private Integer commentCount = 0;

    @Builder.Default
    @Column(name = "approved_count", nullable = false, columnDefinition = "INT UNSIGNED DEFAULT 0")
    private Integer approvedCount = 0;

    @Column(name = "last_comment_at")
    private Instant lastCommentAt;

    @UpdateTimestamp
    @Column(name = "last_sync_at", nullable = false)
    private Instant lastSyncAt;
}