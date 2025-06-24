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

package im.turms.interaction.repository;

import im.turms.interaction.domain.mysql.LikeCountEntity;
import im.turms.interaction.domain.mysql.LikeCountId;
import im.turms.interaction.domain.mysql.LikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 点赞计数Repository
 */
@Repository
public interface LikeCountRepository extends JpaRepository<LikeCountEntity, LikeCountId> {

    /**
     * 根据目标查询点赞计数
     */
    Optional<LikeCountEntity> findByTargetTypeAndTargetId(
            LikeEntity.TargetType targetType, String targetId);

    /**
     * 批量查询多个目标的点赞计数
     */
    @Query("SELECT l FROM LikeCountEntity l WHERE l.targetType = :targetType " +
           "AND l.targetId IN :targetIds")
    List<LikeCountEntity> findByTargetTypeAndTargetIdIn(
            @Param("targetType") LikeEntity.TargetType targetType,
            @Param("targetIds") List<String> targetIds);

    /**
     * 增加点赞计数
     */
    @Modifying
    @Query("UPDATE LikeCountEntity l SET l.likeCount = l.likeCount + :delta " +
           "WHERE l.targetType = :targetType AND l.targetId = :targetId")
    int incrementLikeCount(@Param("targetType") LikeEntity.TargetType targetType,
                          @Param("targetId") String targetId,
                          @Param("delta") int delta);

    /**
     * 重置点赞计数（从实际点赞记录重新计算）
     */
    @Modifying
    @Query("UPDATE LikeCountEntity l SET l.likeCount = " +
           "(SELECT COUNT(le) FROM LikeEntity le WHERE le.targetType = l.targetType " +
           "AND le.targetId = l.targetId) WHERE l.targetType = :targetType AND l.targetId = :targetId")
    int resetLikeCount(@Param("targetType") LikeEntity.TargetType targetType,
                      @Param("targetId") String targetId);

    /**
     * 查询热门内容（按点赞数排序）
     */
    @Query("SELECT l FROM LikeCountEntity l WHERE l.targetType = :targetType " +
           "ORDER BY l.likeCount DESC")
    List<LikeCountEntity> findTopLikedTargets(@Param("targetType") LikeEntity.TargetType targetType);
}