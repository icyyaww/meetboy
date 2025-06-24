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
 * 点赞计数实体 - MySQL汇总存储
 */
@Entity
@Table(name = "like_counts",
       indexes = @Index(name = "idx_count_sync", columnList = "like_count, last_sync_at"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(LikeCountId.class)
public class LikeCountEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private LikeEntity.TargetType targetType;

    @Id
    @Column(name = "target_id", nullable = false, length = 100)
    private String targetId;

    @Builder.Default
    @Column(name = "like_count", nullable = false, columnDefinition = "INT UNSIGNED DEFAULT 0")
    private Integer likeCount = 0;

    @UpdateTimestamp
    @Column(name = "last_sync_at", nullable = false)
    private Instant lastSyncAt;
}