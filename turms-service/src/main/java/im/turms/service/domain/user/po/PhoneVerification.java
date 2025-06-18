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

package im.turms.service.domain.user.po;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;

import im.turms.server.common.domain.common.po.BaseEntity;
import im.turms.server.common.storage.mongo.entity.annotation.Document;
import im.turms.server.common.storage.mongo.entity.annotation.Field;
import im.turms.server.common.storage.mongo.entity.annotation.Id;
import im.turms.server.common.storage.mongo.entity.annotation.Indexed;

/**
 * @author James Chen
 */
@AllArgsConstructor
@Data
@Document(PhoneVerification.COLLECTION_NAME)
public final class PhoneVerification extends BaseEntity {

    public static final String COLLECTION_NAME = "phoneVerification";

    @Id
    private final String phoneNumber;

    @Field(Fields.VERIFICATION_CODE)
    private final String verificationCode;

    @Field(Fields.EXPIRE_TIME)
    @Indexed(expireAfterSeconds = 0)
    private final Date expireTime;

    @Field(Fields.RETRY_COUNT)
    private final Integer retryCount;

    @Field(Fields.CREATED_TIME)
    private final Date createdTime;

    @Field(Fields.IP_ADDRESS)
    private final String ipAddress;

    public static final class Fields {
        public static final String VERIFICATION_CODE = "vc";
        public static final String EXPIRE_TIME = "et";
        public static final String RETRY_COUNT = "rc";
        public static final String CREATED_TIME = "ct";
        public static final String IP_ADDRESS = "ip";

        private Fields() {
        }
    }
}