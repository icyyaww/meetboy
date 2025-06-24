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

package im.turms.interaction.service;

import im.turms.interaction.domain.InteractionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * 事件发布服务
 * 
 * 功能：
 * - 事件持久化
 * - Kafka消息发布
 * - 事件重试机制
 * - 死信队列处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublishingService {

    private final ReactiveMongoTemplate mongoTemplate;
    private final KafkaSender<String, Object> kafkaSender;
    
    private static final String INTERACTION_TOPIC = "turms.interaction.events";
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * 发布互动事件
     */
    public Mono<Boolean> publishEvent(InteractionEvent event) {
        // 设置默认值
        event.setDefaults();
        
        return mongoTemplate.save(event)
                .flatMap(savedEvent -> {
                    // 发送到Kafka
                    return sendToKafka(savedEvent)
                            .doOnSuccess(success -> {
                                if (success) {
                                    updateEventStatus(savedEvent.getId(), InteractionEvent.EventStatus.PROCESSED);
                                } else {
                                    handleEventFailure(savedEvent);
                                }
                            })
                            .doOnError(error -> {
                                log.error("事件发布失败: eventId={}", savedEvent.getId(), error);
                                handleEventFailure(savedEvent);
                            });
                })
                .onErrorReturn(false);
    }

    /**
     * 发送事件到Kafka
     */
    private Mono<Boolean> sendToKafka(InteractionEvent event) {
        try {
            ProducerRecord<String, Object> record = new ProducerRecord<>(
                    INTERACTION_TOPIC,
                    event.getPartitionKey(),
                    event
            );
            
            SenderRecord<String, Object, String> senderRecord = SenderRecord.create(
                    record, event.getId()
            );
            
            return kafkaSender.send(Mono.just(senderRecord))
                    .next()
                    .map(result -> {
                        log.debug("事件已发送到Kafka: eventId={}, partition={}, offset={}", 
                                event.getId(), 
                                result.recordMetadata().partition(), 
                                result.recordMetadata().offset());
                        return true;
                    })
                    .onErrorReturn(false);
            
        } catch (Exception e) {
            log.error("Kafka发送异常", e);
            return Mono.just(false);
        }
    }

    /**
     * 更新事件状态
     */
    private void updateEventStatus(String eventId, InteractionEvent.EventStatus status) {
        mongoTemplate.findById(eventId, InteractionEvent.class)
                .flatMap(event -> {
                    event.setStatus(status);
                    if (status == InteractionEvent.EventStatus.PROCESSED) {
                        InteractionEvent.ProcessingResult result = InteractionEvent.ProcessingResult.builder()
                                .success(true)
                                .message("事件处理成功")
                                .processedAt(Instant.now())
                                .processorId("interaction-service")
                                .build();
                        event.setProcessingResult(result);
                    }
                    return mongoTemplate.save(event);
                })
                .subscribe(
                    updated -> log.debug("事件状态已更新: eventId={}, status={}", eventId, status),
                    error -> log.error("事件状态更新失败: eventId={}", eventId, error)
                );
    }

    /**
     * 处理事件失败
     */
    private void handleEventFailure(InteractionEvent event) {
        int retryCount = event.getRetryCount() != null ? event.getRetryCount() : 0;
        
        if (retryCount < MAX_RETRY_COUNT) {
            // 重试
            event.setRetryCount(retryCount + 1);
            event.setStatus(InteractionEvent.EventStatus.RETRYING);
            
            mongoTemplate.save(event)
                    .delayElement(java.time.Duration.ofSeconds(retryCount * 2)) // 指数退避
                    .flatMap(savedEvent -> publishEvent(savedEvent))
                    .subscribe(
                            success -> log.debug("事件重试: eventId={}, retryCount={}", event.getId(), retryCount + 1),
                            error -> log.error("事件重试失败: eventId={}", event.getId(), error)
                    );
        } else {
            // 移入死信队列
            event.setStatus(InteractionEvent.EventStatus.DEAD_LETTER);
            InteractionEvent.ProcessingResult result = InteractionEvent.ProcessingResult.builder()
                    .success(false)
                    .message("重试次数超限，移入死信队列")
                    .processedAt(Instant.now())
                    .processorId("interaction-service")
                    .build();
            event.setProcessingResult(result);
            
            mongoTemplate.save(event)
                    .subscribe(
                            deadLetter -> log.warn("事件移入死信队列: eventId={}", event.getId()),
                            error -> log.error("死信队列处理失败: eventId={}", event.getId(), error)
                    );
        }
    }

    /**
     * 批量发布事件
     */
    public Mono<Long> publishEventBatch(reactor.core.publisher.Flux<InteractionEvent> events) {
        return events
                .flatMap(this::publishEvent)
                .count()
                .doOnSuccess(count -> log.info("批量发布事件完成: count={}", count));
    }
}