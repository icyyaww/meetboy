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
 * 点赞记录实体 - MySQL持久化存储
 */
@Entity
@Table(name = "likes", 
       uniqueConstraints = @UniqueConstraint(name = "uk_user_target", 
                                           columnNames = {"user_id", "target_type", "target_id"}),
       indexes = {
           @Index(name = "idx_target_created", columnList = "target_type, target_id, created_at"),
           @Index(name = "idx_user_created", columnList = "user_id, created_at"),
           @Index(name = "idx_created_target", columnList = "created_at, target_type")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false, length = 100)
    private String targetId;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "device_id", length = 100)
    private String deviceId;

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

    public enum TargetType {
        POST, COMMENT, REPLY, USER, GROUP
    }
}