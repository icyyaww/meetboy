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

package im.turms.interaction.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;

import java.time.Instant;
import java.util.Map;

/**
 * 互动事件实体 - 实时事件流处理
 * 
 * 设计要点：
 * - 事件溯源和重放
 * - 实时推送和通知
 * - 用户行为分析
 * - 系统监控指标
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("interaction_event")
@CompoundIndexes({
    @CompoundIndex(name = "user_time_idx", def = "{'userId': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "target_time_idx", def = "{'targetType': 1, 'targetId': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "type_time_idx", def = "{'eventType': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "stream_idx", def = "{'eventType': 1, 'status': 1, 'timestamp': -1}")
})
public class InteractionEvent {

    @Id
    private String id;

    /**
     * 事件类型
     */
    @Field("event_type")
    @Indexed
    private EventType eventType;

    /**
     * 操作用户ID
     */
    @Field("user_id")
    @Indexed
    private Long userId;

    /**
     * 目标类型
     */
    @Field("target_type")
    @Indexed
    private Like.TargetType targetType;

    /**
     * 目标ID
     */
    @Field("target_id")
    @Indexed
    private String targetId;

    /**
     * 目标拥有者ID (用于通知推送)
     */
    @Field("target_owner_id")
    @Indexed
    private Long targetOwnerId;

    /**
     * 事件状态
     */
    @Field("status")
    @Indexed
    private EventStatus status;

    /**
     * 事件优先级
     */
    @Field("priority")
    private EventPriority priority;

    /**
     * 事件时间戳
     */
    @Field("timestamp")
    @Indexed
    private Instant timestamp;

    /**
     * 事件载荷数据
     */
    @Field("payload")
    private Map<String, Object> payload;

    /**
     * 事件来源
     */
    @Field("source")
    private EventSource source;

    /**
     * 会话ID (用于事件关联)
     */
    @Field("session_id")
    @Indexed
    private String sessionId;

    /**
     * 批次ID (用于批量处理)
     */
    @Field("batch_id")
    @Indexed
    private String batchId;

    /**
     * 重试次数
     */
    @Field("retry_count")
    private Integer retryCount;

    /**
     * 处理结果
     */
    @Field("processing_result")
    private ProcessingResult processingResult;

    /**
     * 时间分桶键
     */
    @Field("time_bucket")
    @Indexed
    private String timeBucket;

    /**
     * 分区键 (用于流处理分区)
     */
    @Field("partition_key")
    @Indexed
    private String partitionKey;

    /**
     * 扩展元数据
     */
    @Field("metadata")
    private Object metadata;

    /**
     * 事件类型枚举
     */
    public enum EventType {
        // 点赞相关事件
        LIKE_ADDED,
        LIKE_REMOVED,
        LIKE_BATCH_UPDATE,
        
        // 评论相关事件
        COMMENT_ADDED,
        COMMENT_UPDATED,
        COMMENT_DELETED,
        COMMENT_APPROVED,
        COMMENT_REJECTED,
        
        // 用户行为事件
        USER_VIEW,
        USER_SHARE,
        USER_FOLLOW,
        USER_UNFOLLOW,
        
        // 系统事件
        CONTENT_MODERATION,
        SPAM_DETECTION,
        RATE_LIMIT_EXCEEDED,
        
        // 统计事件
        ENGAGEMENT_MILESTONE,
        TRENDING_CONTENT,
        USER_ACHIEVEMENT
    }

    /**
     * 事件状态枚举
     */
    public enum EventStatus {
        CREATED,        // 已创建
        PROCESSING,     // 处理中
        PROCESSED,      // 已处理
        FAILED,         // 处理失败
        RETRYING,       // 重试中
        DEAD_LETTER     // 死信队列
    }

    /**
     * 事件优先级枚举
     */
    public enum EventPriority {
        LOW,            // 低优先级
        NORMAL,         // 普通优先级
        HIGH,           // 高优先级
        URGENT          // 紧急优先级
    }

    /**
     * 事件来源
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventSource {
        private String serviceId;      // 服务标识
        private String instanceId;     // 实例标识
        private String version;        // 版本号
        private String correlationId;  // 关联ID
        private String traceId;        // 链路追踪ID
    }

    /**
     * 处理结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingResult {
        private Boolean success;
        private String message;
        private String errorCode;
        private Instant processedAt;
        private Long processingTime;   // 处理耗时(毫秒)
        private String processorId;    // 处理器标识
    }

    /**
     * 生成分区键
     * 基于目标ID进行分区，确保同一目标的事件有序处理
     */
    public void generatePartitionKey() {
        if (targetId != null) {
            this.partitionKey = targetType + ":" + (targetId.hashCode() % 100);
        } else {
            this.partitionKey = "default:" + (userId.hashCode() % 100);
        }
    }

    /**
     * 设置默认值
     */
    public void setDefaults() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (status == null) {
            status = EventStatus.CREATED;
        }
        if (priority == null) {
            priority = EventPriority.NORMAL;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
        if (timeBucket == null) {
            timeBucket = Like.generateTimeBucket(timestamp);
        }
        generatePartitionKey();
    }
}