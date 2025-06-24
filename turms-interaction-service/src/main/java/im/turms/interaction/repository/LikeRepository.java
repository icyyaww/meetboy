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

import im.turms.interaction.domain.mysql.LikeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 点赞记录Repository
 */
@Repository
public interface LikeRepository extends JpaRepository<LikeEntity, Long> {

    /**
     * 查找用户对特定目标的点赞记录
     */
    Optional<LikeEntity> findByUserIdAndTargetTypeAndTargetId(
            Long userId, LikeEntity.TargetType targetType, String targetId);

    /**
     * 检查用户是否已点赞
     */
    boolean existsByUserIdAndTargetTypeAndTargetId(
            Long userId, LikeEntity.TargetType targetType, String targetId);

    /**
     * 根据目标查询点赞列表（分页）
     */
    Page<LikeEntity> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            LikeEntity.TargetType targetType, String targetId, Pageable pageable);

    /**
     * 根据用户查询点赞历史（分页）
     */
    Page<LikeEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 统计目标的点赞总数
     */
    long countByTargetTypeAndTargetId(LikeEntity.TargetType targetType, String targetId);

    /**
     * 根据时间范围统计点赞数
     */
    @Query("SELECT COUNT(l) FROM LikeEntity l WHERE l.targetType = :targetType " +
           "AND l.targetId = :targetId AND l.createdAt BETWEEN :startTime AND :endTime")
    long countByTargetAndTimeRange(@Param("targetType") LikeEntity.TargetType targetType,
                                  @Param("targetId") String targetId,
                                  @Param("startTime") Instant startTime,
                                  @Param("endTime") Instant endTime);

    /**
     * 批量查询多个目标的点赞状态
     */
    @Query("SELECT l FROM LikeEntity l WHERE l.userId = :userId " +
           "AND l.targetType = :targetType AND l.targetId IN :targetIds")
    List<LikeEntity> findUserLikesByTargets(@Param("userId") Long userId,
                                          @Param("targetType") LikeEntity.TargetType targetType,
                                          @Param("targetIds") List<String> targetIds);

    /**
     * 删除用户对特定目标的点赞
     */
    @Modifying
    @Query("DELETE FROM LikeEntity l WHERE l.userId = :userId " +
           "AND l.targetType = :targetType AND l.targetId = :targetId")
    int deleteByUserIdAndTargetTypeAndTargetId(@Param("userId") Long userId,
                                              @Param("targetType") LikeEntity.TargetType targetType,
                                              @Param("targetId") String targetId);

    /**
     * 批量删除过期的点赞记录
     */
    @Modifying
    @Query("DELETE FROM LikeEntity l WHERE l.createdAt < :expireTime")
    int deleteExpiredLikes(@Param("expireTime") Instant expireTime);
}