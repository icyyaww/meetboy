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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * 评论实体 - MySQL主导存储
 */
@Entity
@Table(name = "comments",
       indexes = {
           @Index(name = "idx_article_created", columnList = "article_id, created_at DESC"),
           @Index(name = "idx_article_likes", columnList = "article_id, like_count DESC"),
           @Index(name = "idx_user_created", columnList = "user_id, created_at DESC"),
           @Index(name = "idx_status_created", columnList = "status, created_at DESC"),
           @Index(name = "idx_created_at", columnList = "created_at")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_id", nullable = false, length = 100)
    private String articleId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "avatar", length = 200)
    private String avatar;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CommentStatus status = CommentStatus.APPROVED;

    @Builder.Default
    @Column(name = "like_count", nullable = false, columnDefinition = "INT UNSIGNED DEFAULT 0")
    private Integer likeCount = 0;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "location_info", columnDefinition = "JSON")
    private String locationInfo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum CommentStatus {
        PENDING,    // 待审核
        APPROVED,   // 已通过
        REJECTED,   // 已拒绝
        DELETED     // 已删除
    }
}