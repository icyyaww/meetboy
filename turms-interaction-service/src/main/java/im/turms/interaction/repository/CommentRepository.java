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

import im.turms.interaction.domain.mysql.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * 评论Repository - 一级评论MySQL主导
 */
@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    /**
     * 根据文章ID分页查询评论（按时间降序）
     */
    Page<CommentEntity> findByArticleIdAndStatusOrderByCreatedAtDesc(
            String articleId, CommentEntity.CommentStatus status, Pageable pageable);

    /**
     * 根据文章ID分页查询评论（按点赞数降序）
     */
    Page<CommentEntity> findByArticleIdAndStatusOrderByLikeCountDescCreatedAtDesc(
            String articleId, CommentEntity.CommentStatus status, Pageable pageable);

    /**
     * 根据用户ID查询评论历史（分页）
     */
    Page<CommentEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 统计文章评论总数
     */
    long countByArticleIdAndStatus(String articleId, CommentEntity.CommentStatus status);

    /**
     * 根据时间范围统计评论数
     */
    @Query("SELECT COUNT(c) FROM CommentEntity c WHERE c.articleId = :articleId " +
           "AND c.status = :status AND c.createdAt BETWEEN :startTime AND :endTime")
    long countByArticleAndTimeRange(@Param("articleId") String articleId,
                                   @Param("status") CommentEntity.CommentStatus status,
                                   @Param("startTime") Instant startTime,
                                   @Param("endTime") Instant endTime);

    /**
     * 批量查询多个文章的评论数
     */
    @Query("SELECT c.articleId, COUNT(c) FROM CommentEntity c WHERE c.articleId IN :articleIds " +
           "AND c.status = :status GROUP BY c.articleId")
    List<Object[]> countByArticleIds(@Param("articleIds") List<String> articleIds,
                                    @Param("status") CommentEntity.CommentStatus status);

    /**
     * 查询文章的最新N条评论
     */
    @Query("SELECT c FROM CommentEntity c WHERE c.articleId = :articleId " +
           "AND c.status = :status ORDER BY c.createdAt DESC")
    List<CommentEntity> findTopCommentsByArticle(@Param("articleId") String articleId,
                                                @Param("status") CommentEntity.CommentStatus status,
                                                Pageable pageable);

    /**
     * 查询热门评论（按点赞数排序）
     */
    @Query("SELECT c FROM CommentEntity c WHERE c.articleId = :articleId " +
           "AND c.status = :status AND c.likeCount >= :minLikes " +
           "ORDER BY c.likeCount DESC, c.createdAt DESC")
    List<CommentEntity> findHotCommentsByArticle(@Param("articleId") String articleId,
                                                @Param("status") CommentEntity.CommentStatus status,
                                                @Param("minLikes") int minLikes,
                                                Pageable pageable);

    /**
     * 更新评论点赞数
     */
    @Modifying
    @Query("UPDATE CommentEntity c SET c.likeCount = :likeCount WHERE c.id = :commentId")
    int updateLikeCount(@Param("commentId") Long commentId, @Param("likeCount") Integer likeCount);

    /**
     * 批量删除过期评论（软删除）
     */
    @Modifying
    @Query("UPDATE CommentEntity c SET c.status = 'DELETED' WHERE c.createdAt < :expireTime")
    int deleteExpiredComments(@Param("expireTime") Instant expireTime);

    /**
     * 根据状态查询评论（分页）
     */
    Page<CommentEntity> findByStatusOrderByCreatedAtDesc(CommentEntity.CommentStatus status, Pageable pageable);
}