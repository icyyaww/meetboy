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

package im.turms.service.domain.user.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import im.turms.server.common.domain.common.repository.BaseRepository;
import im.turms.server.common.storage.mongo.TurmsMongoClient;
import im.turms.server.common.storage.mongo.operation.option.Filter;
import im.turms.server.common.storage.mongo.operation.option.Update;
import im.turms.service.domain.user.po.PhoneVerification;

/**
 * @author James Chen
 */
@Repository
public class PhoneVerificationRepository extends BaseRepository<PhoneVerification, String> {

    public PhoneVerificationRepository(@Qualifier("userMongoClient") TurmsMongoClient mongoClient) {
        super(mongoClient, PhoneVerification.class);
    }

    public Mono<PhoneVerification> findByPhoneNumber(String phoneNumber) {
        Filter filter = Filter.newBuilder(1)
                .eq("_id", phoneNumber);
        return mongoClient.findOne(entityClass, filter);
    }

    public Mono<Boolean> deleteByPhoneNumber(String phoneNumber) {
        Filter filter = Filter.newBuilder(1)
                .eq("_id", phoneNumber);
        return mongoClient.deleteOne(entityClass, filter)
                .map(result -> result.getDeletedCount() > 0);
    }

    public Mono<Boolean> incrementRetryCount(String phoneNumber) {
        // 由于Turms的Update类不支持$inc操作，我们先查询再更新
        return findByPhoneNumber(phoneNumber)
                .flatMap(verification -> {
                    Filter filter = Filter.newBuilder(1)
                            .eq("_id", phoneNumber);
                    Update update = Update.newBuilder(1)
                            .set(PhoneVerification.Fields.RETRY_COUNT, verification.getRetryCount() + 1);
                    return mongoClient.updateOne(entityClass, filter, update)
                            .map(result -> result.getModifiedCount() > 0);
                })
                .defaultIfEmpty(false);
    }

    public Mono<PhoneVerification> insertAndReturn(PhoneVerification phoneVerification) {
        return mongoClient.insert(phoneVerification)
                .thenReturn(phoneVerification);
    }
}