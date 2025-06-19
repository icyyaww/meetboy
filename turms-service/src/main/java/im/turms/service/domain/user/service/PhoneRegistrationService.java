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

import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import im.turms.server.common.access.common.ResponseStatusCode;
import im.turms.server.common.domain.user.po.User;
import im.turms.server.common.domain.user.constant.RegistrationType;
import im.turms.server.common.infra.exception.ResponseException;
import im.turms.server.common.infra.property.TurmsPropertiesManager;
import im.turms.server.common.infra.time.DateTimeUtil;
import im.turms.service.domain.user.po.PhoneVerification;
import im.turms.service.domain.user.repository.PhoneVerificationRepository;
import im.turms.service.domain.user.repository.UserRepository;

/**
 * @author James Chen
 */
@Service
public class PhoneRegistrationService {
    
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final SmsService smsService;
    
    // Configuration
    private int verificationCodeLength = 6;
    private int verificationCodeExpireMinutes = 5;
    private int maxRetryCount = 3;
    
    public PhoneRegistrationService(
            PhoneVerificationRepository phoneVerificationRepository,
            UserRepository userRepository,
            UserService userService,
            SmsService smsService,
            TurmsPropertiesManager propertiesManager) {
        this.phoneVerificationRepository = phoneVerificationRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.smsService = smsService;
        
        propertiesManager.notifyAndAddGlobalPropertiesChangeListener(properties -> {
            // TODO: Update phone registration properties
        });
    }
    
    /**
     * Send verification code
     */
    public Mono<Void> sendVerificationCode(String phoneNumber, String ipAddress) {
        String code = generateVerificationCode();
        Date expireTime = new Date(System.currentTimeMillis() + verificationCodeExpireMinutes * 60 * 1000L);
        
        PhoneVerification verification = new PhoneVerification(
            phoneNumber,
            code,
            expireTime,
            0,
            new Date(),
            ipAddress
        );
        
        return phoneVerificationRepository.insertAndReturn(verification)
            .flatMap(saved -> smsService.sendVerificationCode(phoneNumber, code))
            .flatMap(sent -> {
                if (sent) {
                    return Mono.empty();
                } else {
                    return Mono.error(ResponseException.get(
                        ResponseStatusCode.SERVER_INTERNAL_ERROR,
                        "短信发送失败"
                    ));
                }
            });
    }
    
    /**
     * Register user with phone number
     */
    public Mono<User> registerWithPhone(String phoneNumber, String verificationCode,
                                      String password, String nickname) {
        return validateVerificationCode(phoneNumber, verificationCode)
            .flatMap(valid -> {
                if (!valid) {
                    return Mono.error(ResponseException.get(
                        ResponseStatusCode.VERIFICATION_CODE_MISMATCH,
                        "验证码错误或已过期"
                    ));
                }
                
                // Check if phone number already exists
                return userRepository.existsByPhoneNumber(phoneNumber)
                    .flatMap(exists -> {
                        if (exists) {
                            return Mono.error(ResponseException.get(
                                ResponseStatusCode.PHONE_NUMBER_ALREADY_EXISTS,
                                "手机号已注册"
                            ));
                        }
                        
                        // Create user with phone number
                        return userService.addUserWithPhone(
                            phoneNumber,
                            password,
                            nickname,
                            RegistrationType.PHONE
                        );
                    })
                    .doOnSuccess(user -> {
                        // Delete verification code
                        phoneVerificationRepository.deleteByPhoneNumber(phoneNumber)
                            .subscribe();
                    });
            });
    }
    
    /**
     * Validate verification code
     */
    private Mono<Boolean> validateVerificationCode(String phoneNumber, String inputCode) {
        return phoneVerificationRepository.findByPhoneNumber(phoneNumber)
            .map(verification -> {
                // Check expiration
                if (verification.getExpireTime().before(new Date())) {
                    return false;
                }
                
                // Check retry count
                if (verification.getRetryCount() >= maxRetryCount) {
                    return false;
                }
                
                // Validate code
                return verification.getVerificationCode().equals(inputCode);
            })
            .defaultIfEmpty(false)
            .doOnNext(valid -> {
                if (!valid) {
                    // Increment retry count
                    phoneVerificationRepository.incrementRetryCount(phoneNumber)
                        .subscribe();
                }
            });
    }
    
    /**
     * Generate verification code
     */
    private String generateVerificationCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < verificationCodeLength; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}