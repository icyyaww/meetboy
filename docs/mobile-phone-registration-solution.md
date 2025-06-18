# 手机号注册方案设计

## 🎯 方案概述

将Turms系统改造为支持客户端直接使用手机号+验证码注册的完整解决方案。

## 📊 当前系统限制分析

### 现有问题
1. **无客户端注册API**: 只能通过管理员后台创建用户
2. **缺少手机号验证**: 没有短信验证码机制
3. **用户ID生成方式**: 当前使用雪花算法生成ID，需要适配手机号
4. **认证体系**: 当前基于用户ID+密码，需要支持手机号登录

## 🏗️ 整体架构方案

### 1. 新增组件架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Client Application                       │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                  turms-gateway                              │
│  ┌─────────────────────────────────────────────────────────┐│
│  │           Registration Controller                       ││
│  │  • sendVerificationCode()                              ││
│  │  • registerWithPhone()                                 ││
│  │  • loginWithPhone()                                    ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                  turms-service                              │
│  ┌─────────────────────────────────────────────────────────┐│
│  │         Phone Registration Service                      ││
│  │  • generateVerificationCode()                          ││
│  │  • validateVerificationCode()                          ││
│  │  • registerUserWithPhone()                             ││
│  └─────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │            SMS Service                                  ││
│  │  • sendSms() (集成阿里云/腾讯云等)                        ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                 Data Storage                                │
│  ┌───────────────┬───────────────┬─────────────────────────┐│
│  │   MongoDB     │     Redis     │     SMS Provider        ││
│  │  • Users      │ • Verification│   • Alibaba Cloud       ││
│  │  • UserPhone  │   Codes       │   • Tencent Cloud       ││
│  │               │ • Rate Limits │   • Twilio              ││
│  └───────────────┴───────────────┴─────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### 2. 数据模型设计

#### 2.1 扩展User实体
```java
// 位置: turms-server-common/src/main/java/im/turms/server/common/domain/user/po/User.java
@Document(User.COLLECTION_NAME)
public final class User extends BaseEntity {
    // ... 现有字段
    
    // 新增字段
    @Field(Fields.PHONE_NUMBER)
    @Indexed(unique = true)  // 手机号唯一索引
    private final String phoneNumber;
    
    @Field(Fields.PHONE_VERIFIED)
    private final Boolean phoneVerified;
    
    @Field(Fields.REGISTRATION_TYPE)
    private final RegistrationTypeType registrationType;  // ADMIN, PHONE, EMAIL等
    
    public static final class Fields {
        // ... 现有字段
        public static final String PHONE_NUMBER = "pn";
        public static final String PHONE_VERIFIED = "pv";
        public static final String REGISTRATION_TYPE = "rt";
    }
}

// 注册类型枚举
public enum RegistrationTypeType {
    ADMIN,      // 管理员创建
    PHONE,      // 手机号注册  
    EMAIL,      // 邮箱注册
    THIRD_PARTY // 第三方登录
}
```

#### 2.2 验证码存储模型
```java
// 位置: turms-service/src/main/java/im/turms/service/domain/user/po/PhoneVerification.java
@Document(PhoneVerification.COLLECTION_NAME)
public final class PhoneVerification extends BaseEntity {
    public static final String COLLECTION_NAME = "phoneVerification";
    
    @Id
    private final String phoneNumber;  // 手机号作为主键
    
    @Field(Fields.VERIFICATION_CODE)
    private final String verificationCode;
    
    @Field(Fields.EXPIRE_TIME)
    @Indexed(expireAfterSeconds = 0)  // MongoDB TTL索引，自动过期
    private final Date expireTime;
    
    @Field(Fields.RETRY_COUNT)
    private final Integer retryCount;  // 重试次数
    
    @Field(Fields.CREATED_TIME)
    private final Date createdTime;
    
    @Field(Fields.IP_ADDRESS)
    private final String ipAddress;  // 防刷机制
    
    public static final class Fields {
        public static final String VERIFICATION_CODE = "vc";
        public static final String EXPIRE_TIME = "et";
        public static final String RETRY_COUNT = "rc";
        public static final String CREATED_TIME = "ct";
        public static final String IP_ADDRESS = "ip";
    }
}
```

### 3. 服务层设计

#### 3.1 短信服务接口
```java
// 位置: turms-service/src/main/java/im/turms/service/domain/user/service/SmsService.java
@Service
public class SmsService {
    
    private final SmsProvider smsProvider;  // 可配置的短信提供商
    private final TurmsPropertiesManager propertiesManager;
    
    /**
     * 发送验证码短信
     */
    public Mono<Boolean> sendVerificationCode(String phoneNumber, String code) {
        return validatePhoneNumber(phoneNumber)
            .flatMap(valid -> {
                if (!valid) {
                    return Mono.error(new ResponseException(ResponseStatusCode.ILLEGAL_ARGUMENT, "Invalid phone number"));
                }
                
                String message = String.format("您的验证码是：%s，5分钟内有效。", code);
                return smsProvider.sendSms(phoneNumber, message);
            });
    }
    
    /**
     * 验证手机号格式
     */
    private Mono<Boolean> validatePhoneNumber(String phoneNumber) {
        // 支持国际手机号格式验证
        String regex = "^(\\+?86)?1[3-9]\\d{9}$";  // 中国手机号
        return Mono.just(phoneNumber.matches(regex));
    }
}

// 短信提供商接口
public interface SmsProvider {
    Mono<Boolean> sendSms(String phoneNumber, String message);
}

// 阿里云短信实现
@Component
@ConditionalOnProperty(name = "turms.service.sms.provider", havingValue = "alibaba")
public class AlibabaSmsProvider implements SmsProvider {
    
    private final IAcsClient client;
    
    @Override
    public Mono<Boolean> sendSms(String phoneNumber, String message) {
        return Mono.fromCallable(() -> {
            CommonRequest request = new CommonRequest();
            request.setSysMethod(MethodType.POST);
            request.setSysDomain("dysmsapi.aliyuncs.com");
            request.setSysVersion("2017-05-25");
            request.setSysAction("SendSms");
            request.putQueryParameter("PhoneNumbers", phoneNumber);
            request.putQueryParameter("SignName", "您的签名");
            request.putQueryParameter("TemplateCode", "SMS_123456789");
            request.putQueryParameter("TemplateParam", "{\"code\":\"" + extractCode(message) + "\"}");
            
            CommonResponse response = client.getCommonResponse(request);
            return response.getHttpStatus() == 200;
        }).onErrorReturn(false);
    }
}
```

#### 3.2 手机号注册服务
```java
// 位置: turms-service/src/main/java/im/turms/service/domain/user/service/PhoneRegistrationService.java
@Service
public class PhoneRegistrationService {
    
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final UserService userService;
    private final SmsService smsService;
    private final PasswordManager passwordManager;
    private final IdService idService;
    
    private final RedisTemplate<String, String> redisTemplate;
    
    // 配置项
    private int verificationCodeLength = 6;
    private int verificationCodeExpireMinutes = 5;
    private int maxRetryCount = 3;
    private int maxDailySmsCount = 10;
    
    /**
     * 发送验证码
     */
    public Mono<Void> sendVerificationCode(String phoneNumber, String ipAddress) {
        return checkSendLimits(phoneNumber, ipAddress)
            .flatMap(canSend -> {
                if (!canSend) {
                    return Mono.error(new ResponseException(
                        ResponseStatusCode.TOO_MANY_REQUESTS, 
                        "发送验证码过于频繁"
                    ));
                }
                
                String code = generateVerificationCode();
                Date expireTime = DateTimeUtil.add(new Date(), verificationCodeExpireMinutes, TimeUnit.MINUTES);
                
                PhoneVerification verification = new PhoneVerification(
                    null,
                    phoneNumber,
                    code,
                    expireTime,
                    0,
                    new Date(),
                    ipAddress
                );
                
                return phoneVerificationRepository.save(verification)
                    .flatMap(saved -> smsService.sendVerificationCode(phoneNumber, code))
                    .flatMap(sent -> {
                        if (sent) {
                            // 记录发送限制
                            recordSmsCount(phoneNumber, ipAddress);
                            return Mono.empty();
                        } else {
                            return Mono.error(new ResponseException(
                                ResponseStatusCode.SERVER_INTERNAL_ERROR, 
                                "短信发送失败"
                            ));
                        }
                    });
            });
    }
    
    /**
     * 验证验证码并注册用户
     */
    public Mono<User> registerWithPhone(String phoneNumber, String verificationCode, 
                                       String password, String nickname) {
        return validateVerificationCode(phoneNumber, verificationCode)
            .flatMap(valid -> {
                if (!valid) {
                    return Mono.error(new ResponseException(
                        ResponseStatusCode.VERIFICATION_CODE_MISMATCH,
                        "验证码错误或已过期"
                    ));
                }
                
                // 检查手机号是否已注册
                return userService.existsByPhoneNumber(phoneNumber)
                    .flatMap(exists -> {
                        if (exists) {
                            return Mono.error(new ResponseException(
                                ResponseStatusCode.PHONE_NUMBER_ALREADY_EXISTS,
                                "手机号已注册"
                            ));
                        }
                        
                        // 创建用户
                        Long userId = idService.generateId(ServiceType.USER);
                        return userService.addUserWithPhone(
                            userId,
                            phoneNumber,
                            password,
                            nickname,
                            RegistrationTypeType.PHONE
                        );
                    })
                    .doOnSuccess(user -> {
                        // 删除验证码记录
                        phoneVerificationRepository.deleteByPhoneNumber(phoneNumber)
                            .subscribe();
                    });
            });
    }
    
    /**
     * 验证验证码
     */
    private Mono<Boolean> validateVerificationCode(String phoneNumber, String inputCode) {
        return phoneVerificationRepository.findByPhoneNumber(phoneNumber)
            .map(verification -> {
                // 检查过期时间
                if (verification.getExpireTime().before(new Date())) {
                    return false;
                }
                
                // 检查重试次数
                if (verification.getRetryCount() >= maxRetryCount) {
                    return false;
                }
                
                // 验证验证码
                return verification.getVerificationCode().equals(inputCode);
            })
            .defaultIfEmpty(false)
            .doOnNext(valid -> {
                if (!valid) {
                    // 增加重试次数
                    phoneVerificationRepository.incrementRetryCount(phoneNumber)
                        .subscribe();
                }
            });
    }
    
    /**
     * 生成验证码
     */
    private String generateVerificationCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < verificationCodeLength; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
    
    /**
     * 检查发送限制
     */
    private Mono<Boolean> checkSendLimits(String phoneNumber, String ipAddress) {
        String phoneKey = "sms_count:phone:" + phoneNumber;
        String ipKey = "sms_count:ip:" + ipAddress;
        
        return Mono.fromCallable(() -> {
            // 检查手机号每日发送次数
            String phoneCount = redisTemplate.opsForValue().get(phoneKey);
            if (phoneCount != null && Integer.parseInt(phoneCount) >= maxDailySmsCount) {
                return false;
            }
            
            // 检查IP每日发送次数
            String ipCount = redisTemplate.opsForValue().get(ipKey);
            if (ipCount != null && Integer.parseInt(ipCount) >= maxDailySmsCount * 5) {
                return false;
            }
            
            // 检查手机号60秒内是否已发送
            String recentKey = "sms_recent:" + phoneNumber;
            String recent = redisTemplate.opsForValue().get(recentKey);
            return recent == null;
        });
    }
    
    /**
     * 记录发送次数
     */
    private void recordSmsCount(String phoneNumber, String ipAddress) {
        String phoneKey = "sms_count:phone:" + phoneNumber;
        String ipKey = "sms_count:ip:" + ipAddress;
        String recentKey = "sms_recent:" + phoneNumber;
        
        // 记录每日发送次数
        redisTemplate.opsForValue().increment(phoneKey);
        redisTemplate.expire(phoneKey, Duration.ofDays(1));
        
        redisTemplate.opsForValue().increment(ipKey);
        redisTemplate.expire(ipKey, Duration.ofDays(1));
        
        // 记录60秒限制
        redisTemplate.opsForValue().set(recentKey, "1", Duration.ofSeconds(60));
    }
}
```

#### 3.3 扩展用户服务
```java
// 位置: turms-service/src/main/java/im/turms/service/domain/user/service/UserService.java
// 在现有UserService中添加方法

/**
 * 使用手机号创建用户
 */
public Mono<User> addUserWithPhone(
    @NotNull Long userId,
    @NotNull String phoneNumber,
    @Nullable String rawPassword,
    @Nullable String name,
    @NotNull RegistrationTypeType registrationType) {
    
    try {
        Validator.notNull(userId, "userId");
        Validator.notNull(phoneNumber, "phoneNumber");
        Validator.notNull(registrationType, "registrationType");
    } catch (ResponseException e) {
        return Mono.error(e);
    }
    
    Date now = new Date();
    String hashedPassword = passwordManager.encode(rawPassword);
    
    User user = new User(
        userId,
        hashedPassword,
        name != null ? name : "用户" + phoneNumber.substring(phoneNumber.length() - 4),
        null, // intro
        null, // profilePicture
        ProfileAccessStrategy.ALL,
        DEFAULT_USER_ROLE_ID,
        now, // registrationDate
        null, // lastModifiedDate
        true, // isActive - 手机号注册默认激活
        phoneNumber,
        true, // phoneVerified
        registrationType
    );
    
    return userRepository.inTransaction(session ->
        userRepository.insert(user, session)
            .then(userRelationshipGroupService.createRelationshipGroup(
                userId,
                0,
                DEFAULT_RELATIONSHIP_GROUP_NAME,
                now,
                session
            ))
            .then(userVersionService.upsert(
                userId,
                now,
                now,
                now,
                now,
                now,
                session
            ))
            .thenReturn(user)
    ).doOnSuccess(createdUser -> {
        registeredUsersCounter.increment();
        // 可选：同步到Elasticsearch
        elasticsearchManager.upsertUserDoc(createdUser).subscribe();
    });
}

/**
 * 检查手机号是否已存在
 */
public Mono<Boolean> existsByPhoneNumber(@NotNull String phoneNumber) {
    try {
        Validator.notNull(phoneNumber, "phoneNumber");
    } catch (ResponseException e) {
        return Mono.error(e);
    }
    return userRepository.existsByPhoneNumber(phoneNumber);
}

/**
 * 通过手机号查找用户
 */
public Mono<User> findByPhoneNumber(@NotNull String phoneNumber) {
    try {
        Validator.notNull(phoneNumber, "phoneNumber");
    } catch (ResponseException e) {
        return Mono.error(e);
    }
    return userRepository.findByPhoneNumber(phoneNumber);
}
```

### 4. API控制器设计

#### 4.1 注册相关API
```java
// 位置: turms-gateway/src/main/java/im/turms/gateway/domain/registration/controller/RegistrationController.java
@RestController
@RequestMapping("/api/v1/registration")
public class RegistrationController {
    
    private final PhoneRegistrationService phoneRegistrationService;
    private final IpRequestThrottler ipRequestThrottler;
    
    /**
     * 发送手机验证码
     */
    @PostMapping("/phone/verification-code")
    public Mono<ResponseEntity<Void>> sendVerificationCode(
        @RequestBody SendVerificationCodeRequest request,
        HttpServletRequest httpRequest) {
        
        String ipAddress = InetAddressUtil.getClientIpAddress(httpRequest);
        
        // IP限流检查
        return ipRequestThrottler.isAllowed(ipAddress)
            .flatMap(allowed -> {
                if (!allowed) {
                    return Mono.error(new ResponseException(
                        ResponseStatusCode.TOO_MANY_REQUESTS,
                        "请求过于频繁"
                    ));
                }
                
                return phoneRegistrationService.sendVerificationCode(
                    request.getPhoneNumber(),
                    ipAddress
                );
            })
            .then(Mono.just(ResponseEntity.ok().build()))
            .onErrorResume(ResponseException.class, e -> 
                Mono.just(ResponseEntity.status(e.getStatusCode().getHttpStatusCode())
                    .body(null))
            );
    }
    
    /**
     * 手机号注册
     */
    @PostMapping("/phone")
    public Mono<ResponseEntity<RegisterResponse>> registerWithPhone(
        @RequestBody @Valid PhoneRegistrationRequest request,
        HttpServletRequest httpRequest) {
        
        String ipAddress = InetAddressUtil.getClientIpAddress(httpRequest);
        
        return phoneRegistrationService.registerWithPhone(
            request.getPhoneNumber(),
            request.getVerificationCode(),
            request.getPassword(),
            request.getNickname()
        ).map(user -> {
            RegisterResponse response = new RegisterResponse(
                user.getId(),
                user.getPhoneNumber(),
                user.getName(),
                user.getRegistrationDate()
            );
            return ResponseEntity.ok(response);
        }).onErrorResume(ResponseException.class, e ->
            Mono.just(ResponseEntity.status(e.getStatusCode().getHttpStatusCode())
                .body(null))
        );
    }
}

// 请求/响应DTO
@Data
public class SendVerificationCodeRequest {
    @NotBlank
    @Pattern(regexp = "^(\\+?86)?1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phoneNumber;
}

@Data
public class PhoneRegistrationRequest {
    @NotBlank
    @Pattern(regexp = "^(\\+?86)?1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phoneNumber;
    
    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "验证码为6位数字")
    private String verificationCode;
    
    @NotBlank
    @Size(min = 6, max = 32, message = "密码长度为6-32位")
    private String password;
    
    @Size(max = 50, message = "昵称最大长度为50")
    private String nickname;
}

@Data
@AllArgsConstructor
public class RegisterResponse {
    private Long userId;
    private String phoneNumber;
    private String nickname;
    private Date registrationDate;
}
```

#### 4.2 手机号登录API
```java
// 位置: turms-gateway/src/main/java/im/turms/gateway/domain/session/service/PhoneSessionIdentityAccessManager.java
@Component
public class PhoneSessionIdentityAccessManager implements SessionIdentityAccessManagementSupport {
    
    private final UserService userService;
    
    @Override
    public Mono<UserPermissionInfo> verifyAndGrant(UserLoginInfo userLoginInfo) {
        String phoneNumber = userLoginInfo.phoneNumber();
        String password = userLoginInfo.password();
        
        return userService.findByPhoneNumber(phoneNumber)
            .flatMap(user -> {
                if (!user.getIsActive() || user.getDeletionDate() != null) {
                    return LOGGING_IN_USER_NOT_ACTIVE_MONO;
                }
                
                return userService.authenticate(user.getId(), password)
                    .map(authenticated -> authenticated
                        ? GRANTED_WITH_ALL_PERMISSIONS
                        : LOGIN_AUTHENTICATION_FAILED);
            })
            .switchIfEmpty(Mono.just(LOGIN_AUTHENTICATION_FAILED));
    }
}

// 扩展登录信息
@Data
public class UserLoginInfo {
    private final Long userId;
    private final String password;
    private final String phoneNumber;  // 新增手机号字段
    
    // 构造函数
    public static UserLoginInfo forUserId(Long userId, String password) {
        return new UserLoginInfo(userId, password, null);
    }
    
    public static UserLoginInfo forPhoneNumber(String phoneNumber, String password) {
        return new UserLoginInfo(null, password, phoneNumber);
    }
}
```

### 5. 协议和客户端支持

#### 5.1 扩展Proto协议
```protobuf
// 新增注册相关请求
message SendVerificationCodeRequest {
    string phone_number = 1;
}

message RegisterWithPhoneRequest {
    string phone_number = 1;
    string verification_code = 2;
    string password = 3;
    optional string nickname = 4;
}

message LoginWithPhoneRequest {
    string phone_number = 1;
    string password = 2;
    UserStatus user_status = 3;
    DeviceType device_type = 4;
    map<string, string> device_details = 5;
    optional UserLocation location = 6;
}

// 扩展TurmsRequest
message TurmsRequest {
    // ... 现有字段
    oneof kind {
        // ... 现有请求类型
        SendVerificationCodeRequest send_verification_code_request = 200;
        RegisterWithPhoneRequest register_with_phone_request = 201;
        LoginWithPhoneRequest login_with_phone_request = 202;
    }
}
```

#### 5.2 客户端SDK扩展
```javascript
// JavaScript客户端扩展
class TurmsClient {
    // ... 现有方法
    
    /**
     * 发送手机验证码
     */
    async sendVerificationCode(phoneNumber) {
        const request = new SendVerificationCodeRequest();
        request.setPhoneNumber(phoneNumber);
        
        return this.driver.send(request);
    }
    
    /**
     * 手机号注册
     */
    async registerWithPhone(phoneNumber, verificationCode, password, nickname) {
        const request = new RegisterWithPhoneRequest();
        request.setPhoneNumber(phoneNumber);
        request.setVerificationCode(verificationCode);
        request.setPassword(password);
        if (nickname) {
            request.setNickname(nickname);
        }
        
        return this.driver.send(request);
    }
    
    /**
     * 手机号登录
     */
    async loginWithPhone(phoneNumber, password, userStatus = UserStatus.AVAILABLE, deviceType = DeviceType.UNKNOWN) {
        const request = new LoginWithPhoneRequest();
        request.setPhoneNumber(phoneNumber);
        request.setPassword(password);
        request.setUserStatus(userStatus);
        request.setDeviceType(deviceType);
        
        return this.driver.send(request);
    }
}
```

### 6. 配置管理

#### 6.1 应用配置
```yaml
# application.yml
turms:
  service:
    # SMS服务配置
    sms:
      provider: alibaba  # alibaba, tencent, twilio
      enabled: true
      alibaba:
        access-key-id: ${SMS_ACCESS_KEY_ID}
        access-key-secret: ${SMS_ACCESS_KEY_SECRET}
        sign-name: "您的应用"
        template-code: "SMS_123456789"
      
    # 手机注册配置
    phone-registration:
      enabled: true
      verification-code:
        length: 6
        expire-minutes: 5
        max-retry-count: 3
      rate-limit:
        max-daily-sms-per-phone: 10
        max-daily-sms-per-ip: 50
        send-interval-seconds: 60
      
    user:
      # 现有配置
      activate-user-when-added: true
      info:
        # 手机号相关配置
        phone-number-required: false
        allow-phone-number-login: true
```

#### 6.2 环境变量配置
```bash
# 短信服务配置
SMS_ACCESS_KEY_ID=your_access_key_id
SMS_ACCESS_KEY_SECRET=your_access_key_secret

# Redis配置（验证码存储）
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
```

### 7. 数据库脚本

#### 7.1 MongoDB索引创建
```javascript
// 用户集合新增索引
db.user.createIndex({ "pn": 1 }, { unique: true, sparse: true, name: "phone_number_unique" });
db.user.createIndex({ "pn": 1, "pv": 1 }, { name: "phone_verified_index" });

// 验证码集合索引
db.phoneVerification.createIndex({ "et": 1 }, { expireAfterSeconds: 0, name: "expire_time_ttl" });
db.phoneVerification.createIndex({ "ip": 1, "ct": 1 }, { name: "ip_created_time_index" });
```

### 8. 安全性考虑

#### 8.1 防刷机制
```java
// 多维度限流
@Component
public class RegistrationSecurityService {
    
    /**
     * 综合安全检查
     */
    public Mono<SecurityCheckResult> performSecurityCheck(
        String phoneNumber, String ipAddress, String userAgent) {
        
        return Mono.zip(
            checkPhoneFrequency(phoneNumber),
            checkIpFrequency(ipAddress),
            checkDeviceFingerprint(userAgent),
            checkSuspiciousPattern(phoneNumber, ipAddress)
        ).map(tuple -> {
            boolean phoneOk = tuple.getT1();
            boolean ipOk = tuple.getT2();
            boolean deviceOk = tuple.getT3();
            boolean patternOk = tuple.getT4();
            
            if (!phoneOk || !ipOk || !deviceOk || !patternOk) {
                return SecurityCheckResult.blocked("请求被拦截");
            }
            
            return SecurityCheckResult.allowed();
        });
    }
    
    /**
     * 检查可疑模式
     */
    private Mono<Boolean> checkSuspiciousPattern(String phoneNumber, String ipAddress) {
        // 检查短时间内大量不同手机号从同一IP注册
        String pattern = "suspicious:ip:" + ipAddress;
        return redisTemplate.opsForZSet()
            .count(pattern, 
                System.currentTimeMillis() - Duration.ofHours(1).toMillis(),
                System.currentTimeMillis())
            .map(count -> count < 5);  // 1小时内最多5个不同手机号
    }
}
```

#### 8.2 数据脱敏
```java
// 日志中的手机号脱敏
@Component
public class PhoneNumberMasker {
    
    public String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return "***";
        }
        
        String prefix = phoneNumber.substring(0, 3);
        String suffix = phoneNumber.substring(phoneNumber.length() - 4);
        return prefix + "****" + suffix;
    }
}
```

## 🚀 实施步骤

### 第一阶段: 基础准备（1-2周）
1. **数据库调整**
   - 扩展User实体，添加手机号字段
   - 创建PhoneVerification集合
   - 添加必要的索引

2. **短信服务集成**
   - 集成阿里云/腾讯云短信服务
   - 实现SmsService基础功能
   - 配置短信模板和签名

### 第二阶段: 核心功能开发（2-3周）
1. **注册服务开发**
   - 实现PhoneRegistrationService
   - 验证码生成和验证逻辑
   - 防刷和限流机制

2. **API控制器开发**
   - 实现RegistrationController
   - 添加手机号登录支持
   - 请求参数验证

### 第三阶段: 客户端支持（1-2周）
1. **协议扩展**
   - 扩展Proto定义
   - 重新生成客户端代码

2. **SDK更新**
   - 各语言客户端SDK添加注册接口
   - 更新文档和示例

### 第四阶段: 测试和部署（1-2周）
1. **功能测试**
   - 单元测试
   - 集成测试
   - 压力测试

2. **安全测试**
   - 验证码暴力破解测试
   - 短信轰炸防护测试
   - 注册流程安全测试

## 📊 预期效果

### 用户体验提升
- ✅ 用户可直接通过手机号注册，无需管理员创建
- ✅ 支持手机号登录，更符合国内用户习惯
- ✅ 验证码机制保证手机号真实性

### 系统能力增强
- ✅ 完善的防刷和限流机制
- ✅ 多种短信服务商支持
- ✅ 灵活的配置管理
- ✅ 完善的监控和日志

### 安全性保障
- ✅ 多维度安全检查
- ✅ 数据脱敏处理
- ✅ 验证码防暴力破解
- ✅ IP和设备指纹识别

这个方案既保持了Turms原有的架构优势，又添加了现代化的手机号注册功能，为用户提供更好的使用体验。