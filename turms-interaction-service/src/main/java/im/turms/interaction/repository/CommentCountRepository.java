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

import im.turms.interaction.domain.mysql.CommentCountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 评论计数Repository
 */
@Repository
public interface CommentCountRepository extends JpaRepository<CommentCountEntity, String> {

    /**
     * 根据文章ID查询评论计数
     */
    Optional<CommentCountEntity> findByArticleId(String articleId);

    /**
     * 批量查询多个文章的评论计数
     */
    @Query("SELECT c FROM CommentCountEntity c WHERE c.articleId IN :articleIds")
    List<CommentCountEntity> findByArticleIdIn(@Param("articleIds") List<String> articleIds);

    /**
     * 增加评论计数
     */
    @Modifying
    @Query("UPDATE CommentCountEntity c SET c.commentCount = c.commentCount + :delta, " +
           "c.approvedCount = c.approvedCount + :approvedDelta WHERE c.articleId = :articleId")
    int incrementCommentCount(@Param("articleId") String articleId,
                             @Param("delta") int delta,
                             @Param("approvedDelta") int approvedDelta);

    /**
     * 重置评论计数（从实际评论记录重新计算）
     */
    @Modifying
    @Query("UPDATE CommentCountEntity c SET " +
           "c.commentCount = (SELECT COUNT(ce) FROM CommentEntity ce WHERE ce.articleId = c.articleId), " +
           "c.approvedCount = (SELECT COUNT(ce) FROM CommentEntity ce WHERE ce.articleId = c.articleId AND ce.status = 'APPROVED') " +
           "WHERE c.articleId = :articleId")
    int resetCommentCount(@Param("articleId") String articleId);

    /**
     * 查询热门文章（按评论数排序）
     */
    @Query("SELECT c FROM CommentCountEntity c ORDER BY c.commentCount DESC")
    List<CommentCountEntity> findTopCommentedArticles();

    /**
     * 查询最近有评论的文章
     */
    @Query("SELECT c FROM CommentCountEntity c WHERE c.lastCommentAt IS NOT NULL " +
           "ORDER BY c.lastCommentAt DESC")
    List<CommentCountEntity> findRecentlyCommentedArticles();
}