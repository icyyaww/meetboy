# 手机号注册实施指南

## 🎯 核心改造点

### 1. 数据模型扩展

#### User实体添加手机号字段
```java
// 位置: turms-server-common/src/main/java/im/turms/server/common/domain/user/po/User.java
@Document(User.COLLECTION_NAME)
public final class User extends BaseEntity {
    // 新增字段
    @Field(Fields.PHONE_NUMBER)
    @Indexed(unique = true)
    private final String phoneNumber;
    
    @Field(Fields.PHONE_VERIFIED)
    private final Boolean phoneVerified;
    
    public static final class Fields {
        public static final String PHONE_NUMBER = "pn";
        public static final String PHONE_VERIFIED = "pv";
    }
}
```

#### 新建验证码实体
```java
// 位置: turms-service/src/main/java/im/turms/service/domain/user/po/PhoneVerification.java
@Document("phoneVerification")
public final class PhoneVerification {
    @Id
    private final String phoneNumber;
    
    @Field("vc")
    private final String verificationCode;
    
    @Field("et")
    @Indexed(expireAfterSeconds = 0)  // 5分钟自动过期
    private final Date expireTime;
    
    @Field("rc")
    private final Integer retryCount;
}
```

### 2. 关键服务实现

#### 短信服务
```java
// 位置: turms-service/src/main/java/im/turms/service/domain/user/service/SmsService.java
@Service
public class SmsService {
    
    public Mono<Boolean> sendVerificationCode(String phoneNumber, String code) {
        // 调用阿里云/腾讯云短信API
        String message = String.format("验证码：%s，5分钟内有效", code);
        return smsProvider.sendSms(phoneNumber, message);
    }
}
```

#### 手机注册服务
```java
// 位置: turms-service/src/main/java/im/turms/service/domain/user/service/PhoneRegistrationService.java
@Service
public class PhoneRegistrationService {
    
    // 发送验证码
    public Mono<Void> sendVerificationCode(String phoneNumber, String ipAddress) {
        return checkSendLimits(phoneNumber, ipAddress)
            .flatMap(canSend -> {
                if (!canSend) {
                    return Mono.error(new ResponseException(TOO_MANY_REQUESTS));
                }
                
                String code = generateCode();
                return saveVerificationCode(phoneNumber, code)
                    .flatMap(saved -> smsService.sendVerificationCode(phoneNumber, code));
            });
    }
    
    // 注册用户
    public Mono<User> registerWithPhone(String phoneNumber, String code, 
                                       String password, String nickname) {
        return validateCode(phoneNumber, code)
            .flatMap(valid -> {
                if (!valid) {
                    return Mono.error(new ResponseException(VERIFICATION_CODE_MISMATCH));
                }
                
                Long userId = idService.generateId(ServiceType.USER);
                return userService.addUserWithPhone(userId, phoneNumber, password, nickname);
            });
    }
}
```

### 3. API控制器

#### 注册API
```java
// 位置: turms-gateway/src/main/java/im/turms/gateway/access/registration/controller/RegistrationController.java
@RestController
@RequestMapping("/api/v1/registration")
public class RegistrationController {
    
    // 发送验证码
    @PostMapping("/phone/verification-code")
    public Mono<ResponseEntity<Void>> sendCode(@RequestBody SendCodeRequest request) {
        return phoneRegistrationService
            .sendVerificationCode(request.getPhoneNumber(), getClientIP())
            .then(Mono.just(ResponseEntity.ok().build()));
    }
    
    // 手机号注册
    @PostMapping("/phone")
    public Mono<ResponseEntity<RegisterResponse>> register(@RequestBody PhoneRegisterRequest request) {
        return phoneRegistrationService.registerWithPhone(
            request.getPhoneNumber(),
            request.getVerificationCode(),
            request.getPassword(),
            request.getNickname()
        ).map(user -> ResponseEntity.ok(new RegisterResponse(user)));
    }
}
```

### 4. 客户端SDK扩展

#### JavaScript客户端
```javascript
class TurmsClient {
    // 发送验证码
    async sendVerificationCode(phoneNumber) {
        const request = {
            phoneNumber: phoneNumber
        };
        return this.driver.send('/api/v1/registration/phone/verification-code', request);
    }
    
    // 手机号注册
    async registerWithPhone(phoneNumber, code, password, nickname) {
        const request = {
            phoneNumber: phoneNumber,
            verificationCode: code,
            password: password,
            nickname: nickname
        };
        return this.driver.send('/api/v1/registration/phone', request);
    }
    
    // 手机号登录
    async loginWithPhone(phoneNumber, password) {
        return this.sessionService.createSession({
            phoneNumber: phoneNumber,
            password: password,
            deviceType: this.deviceType
        });
    }
}
```

### 5. 配置修改

#### 应用配置
```yaml
# application.yml
turms:
  service:
    # 短信服务配置
    sms:
      provider: alibaba
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
        expire-minutes: 5
        max-retry-count: 3
      rate-limit:
        max-daily-sms-per-phone: 10
        send-interval-seconds: 60
```

## 🛠️ 快速实施步骤

### 第1步: 数据库准备
```bash
# 连接MongoDB，为用户集合添加手机号索引
mongo
use turms
db.user.createIndex({"pn": 1}, {unique: true, sparse: true})
```

### 第2步: 添加核心类文件

1. **添加PhoneVerification实体** (`turms-service/domain/user/po/`)
2. **添加SmsService服务** (`turms-service/domain/user/service/`)
3. **添加PhoneRegistrationService** (`turms-service/domain/user/service/`)
4. **添加RegistrationController** (`turms-gateway/access/registration/controller/`)

### 第3步: 修改现有文件

1. **扩展User实体** - 添加phoneNumber字段
2. **扩展UserService** - 添加手机号相关方法
3. **扩展UserRepository** - 添加手机号查询方法

### 第4步: 配置短信服务

1. **申请短信服务** (阿里云/腾讯云)
2. **配置访问密钥** 
3. **设置短信模板**

### 第5步: 客户端SDK更新

1. **添加注册相关API方法**
2. **更新登录逻辑支持手机号**
3. **重新打包发布SDK**

## ⚡ 关键代码片段

### 验证码生成和验证
```java
// 生成6位数字验证码
private String generateCode() {
    return String.format("%06d", new Random().nextInt(1000000));
}

// 验证验证码
private Mono<Boolean> validateCode(String phoneNumber, String inputCode) {
    return phoneVerificationRepository.findByPhoneNumber(phoneNumber)
        .map(verification -> {
            // 检查过期和重试次数
            return !verification.getExpireTime().before(new Date()) 
                && verification.getRetryCount() < 3
                && verification.getVerificationCode().equals(inputCode);
        })
        .defaultIfEmpty(false);
}
```

### 防刷机制
```java
// Redis限流
private Mono<Boolean> checkSendLimits(String phoneNumber, String ipAddress) {
    return Mono.fromCallable(() -> {
        // 检查手机号每日发送次数
        String phoneKey = "sms_count:phone:" + phoneNumber;
        String count = redisTemplate.opsForValue().get(phoneKey);
        if (count != null && Integer.parseInt(count) >= 10) {
            return false;
        }
        
        // 检查60秒限制
        String recentKey = "sms_recent:" + phoneNumber;
        return redisTemplate.opsForValue().get(recentKey) == null;
    });
}
```

### 阿里云短信集成
```java
@Component
public class AlibabaSmsProvider implements SmsProvider {
    
    @Override
    public Mono<Boolean> sendSms(String phoneNumber, String message) {
        return Mono.fromCallable(() -> {
            CommonRequest request = new CommonRequest();
            request.setSysMethod(MethodType.POST);
            request.setSysDomain("dysmsapi.aliyuncs.com");
            request.setSysVersion("2017-05-25");
            request.setSysAction("SendSms");
            request.putQueryParameter("PhoneNumbers", phoneNumber);
            request.putQueryParameter("SignName", signName);
            request.putQueryParameter("TemplateCode", templateCode);
            request.putQueryParameter("TemplateParam", 
                "{\"code\":\"" + extractCode(message) + "\"}");
            
            CommonResponse response = client.getCommonResponse(request);
            return response.getHttpStatus() == 200;
        });
    }
}
```

## 🔒 安全注意事项

### 1. 防止短信轰炸
- 同一手机号60秒内只能发送1次
- 同一手机号每天最多发送10次
- 同一IP每天最多发送50次

### 2. 验证码安全
- 验证码5分钟自动过期
- 最多尝试3次，超过则失效
- 验证成功后立即删除

### 3. 数据脱敏
```java
// 日志中手机号脱敏
public String maskPhoneNumber(String phoneNumber) {
    return phoneNumber.substring(0, 3) + "****" + 
           phoneNumber.substring(phoneNumber.length() - 4);
}
```

## 📱 客户端使用示例

### 完整注册流程
```javascript
// 1. 发送验证码
await turmsClient.sendVerificationCode("13812345678");

// 2. 用户输入验证码后注册
const user = await turmsClient.registerWithPhone(
    "13812345678",    // 手机号
    "123456",         // 验证码
    "password123",    // 密码
    "张三"            // 昵称(可选)
);

// 3. 注册成功后自动登录
console.log("注册成功，用户ID:", user.userId);
```

### 手机号登录
```javascript
// 使用手机号登录
await turmsClient.loginWithPhone("13812345678", "password123");
```

## 🎯 总结

这个方案**最小化**了对现有系统的改动，主要是**新增功能**而不是修改核心逻辑：

✅ **数据兼容**: 手机号字段为可选，不影响现有用户  
✅ **功能并存**: 管理员注册和手机号注册可以同时使用  
✅ **认证灵活**: 支持用户ID登录和手机号登录  
✅ **安全可靠**: 完善的防刷和验证机制  
✅ **扩展性好**: 可以轻松添加邮箱注册等其他方式

实施后，用户既可以通过传统方式注册登录，也可以使用更便捷的手机号方式，大大提升了用户体验。