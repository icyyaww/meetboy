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

package im.turms.service.domain.user.service.sms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import im.turms.server.common.infra.logging.core.logger.Logger;
import im.turms.server.common.infra.logging.core.logger.LoggerFactory;

/**
 * Console SMS provider for development and testing
 * 
 * @author James Chen
 */
@Component
@ConditionalOnProperty(name = "turms.service.sms.provider", havingValue = "console", matchIfMissing = true)
public class ConsoleSmsProvider implements SmsProvider {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleSmsProvider.class);
    
    @Override
    public Mono<Boolean> sendSms(String phoneNumber, String message) {
        LOGGER.info("Console SMS Provider - Send to {}: {}", 
            maskPhoneNumber(phoneNumber), message);
        return Mono.just(true);
    }
    
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return "***";
        }
        String prefix = phoneNumber.substring(0, 3);
        String suffix = phoneNumber.substring(phoneNumber.length() - 4);
        return prefix + "****" + suffix;
    }
}