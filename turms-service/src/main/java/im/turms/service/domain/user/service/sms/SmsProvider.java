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

import reactor.core.publisher.Mono;

/**
 * @author James Chen
 */
public interface SmsProvider {
    
    /**
     * Send SMS message
     * 
     * @param phoneNumber target phone number
     * @param message message content
     * @return true if sent successfully
     */
    Mono<Boolean> sendSms(String phoneNumber, String message);
    
    /**
     * Send verification code SMS
     * 
     * @param phoneNumber target phone number
     * @param code verification code
     * @return true if sent successfully
     */
    default Mono<Boolean> sendVerificationCode(String phoneNumber, String code) {
        String message = String.format("您的验证码是：%s，5分钟内有效，请勿泄露。", code);
        return sendSms(phoneNumber, message);
    }
}