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

package im.turms.service.domain.user.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import im.turms.server.common.access.common.ResponseStatusCode;
import im.turms.server.common.infra.exception.ResponseException;
import im.turms.server.common.infra.property.TurmsPropertiesManager;
import im.turms.service.domain.user.service.sms.SmsProvider;

/**
 * @author James Chen
 */
@Service
public class SmsService {
    
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+?86)?1[3-9]\\d{9}$");
    
    private final SmsProvider smsProvider;
    private boolean enabled;
    
    public SmsService(SmsProvider smsProvider, TurmsPropertiesManager propertiesManager) {
        this.smsProvider = smsProvider;
        // TODO: Add SMS configuration properties
        this.enabled = true;
        
        propertiesManager.notifyAndAddGlobalPropertiesChangeListener(properties -> {
            // TODO: Update SMS properties
        });
    }
    
    /**
     * Send verification code SMS
     */
    public Mono<Boolean> sendVerificationCode(String phoneNumber, String code) {
        if (!enabled) {
            return Mono.error(ResponseException.get(ResponseStatusCode.SERVER_INTERNAL_ERROR, "SMS service is disabled"));
        }
        
        return validatePhoneNumber(phoneNumber)
            .flatMap(valid -> {
                if (!valid) {
                    return Mono.error(ResponseException.get(
                        ResponseStatusCode.ILLEGAL_ARGUMENT, 
                        "Invalid phone number format"
                    ));
                }
                return smsProvider.sendVerificationCode(phoneNumber, code);
            });
    }
    
    /**
     * Validate phone number format
     */
    private Mono<Boolean> validatePhoneNumber(String phoneNumber) {
        return Mono.just(phoneNumber != null && PHONE_PATTERN.matcher(phoneNumber).matches());
    }
}