# Turms 用户注册登录模块位置详解

## 📍 核心模块位置总览

### 1. 用户注册模块
**位置：turms-service**
```
turms-service/src/main/java/im/turms/service/domain/user/
├── service/
│   └── UserService.java                      # 核心用户服务，包含addUser注册方法
├── access/admin/controller/
│   └── UserController.java                   # 管理员用户注册API
├── access/admin/dto/request/
│   └── AddUserDTO.java                       # 用户注册请求数据结构
├── repository/
│   └── UserRepository.java                   # 用户数据访问层
└── po/
    └── User.java (在turms-server-common中)   # 用户实体模型
```

### 2. 用户登录/认证模块
**位置：turms-gateway**
```
turms-gateway/src/main/java/im/turms/gateway/domain/session/
├── access/client/controller/
│   └── SessionClientController.java          # 客户端登录API控制器
├── service/
│   ├── SessionService.java                   # 会话管理核心服务
│   ├── UserService.java                      # 网关层用户认证服务
│   ├── PasswordSessionIdentityAccessManager.java  # 密码认证管理器
│   ├── JwtSessionIdentityAccessManager.java  # JWT认证管理器
│   ├── LdapSessionIdentityAccessManager.java # LDAP认证管理器
│   └── SessionIdentityAccessManager.java     # 认证管理器接口
├── repository/
│   └── UserRepository.java                   # 网关层用户数据访问
├── manager/
│   └── UserSessionsManager.java              # 用户会话管理器
└── bo/
    ├── UserLoginInfo.java                    # 用户登录信息封装
    └── UserPermissionInfo.java               # 用户权限信息封装
```

## 🔐 认证架构分析

### 登录认证流程

#### 1. 客户端发起登录请求
```java
// 位置：turms-gateway/domain/session/access/client/controller/SessionClientController.java
public Mono<RequestHandlerResult> handleCreateSessionRequest(
    UserSessionWrapper sessionWrapper,
    CreateSessionRequest createSessionRequest) {
    
    long userId = createSessionRequest.getUserId();
    String password = createSessionRequest.getPassword();
    DeviceType deviceType = createSessionRequest.getDeviceType();
    
    // 调用会话服务处理登录
    return sessionService.handleLoginRequest(
        createSessionRequest.getVersion(),
        sessionWrapper.getIp(),
        userId,
        password,
        deviceType,
        deviceDetails,
        userStatus,
        location,
        sessionWrapper.getIpStr()
    );
}
```

#### 2. 会话服务处理登录
```java
// 位置：turms-gateway/domain/session/service/SessionService.java
public Mono<UserSession> handleLoginRequest(
    int version,
    byte[] ip,
    Long userId,
    String password,
    DeviceType deviceType,
    Map<String, String> deviceDetails,
    UserStatus userStatus,
    Location location,
    String ipStr) {
    
    // 1. 身份验证
    return sessionIdentityAccessManager.verifyAndGrant(userLoginInfo)
        .flatMap(permissionInfo -> {
            // 2. 创建用户会话
            // 3. 管理设备连接
            // 4. 更新在线状态
        });
}
```

#### 3. 身份认证验证
```java
// 位置：turms-gateway/domain/session/service/PasswordSessionIdentityAccessManager.java
@Override
public Mono<UserPermissionInfo> verifyAndGrant(UserLoginInfo userLoginInfo) {
    Long userId = userLoginInfo.userId();
    String password = userLoginInfo.password();
    
    return userService.isActiveAndNotDeleted(userId)  // 检查用户状态
        .flatMap(isActive -> isActive
            ? userService.authenticate(userId, password)  // 密码验证
                .map(authenticated -> authenticated
                    ? GRANTED_WITH_ALL_PERMISSIONS
                    : LOGIN_AUTHENTICATION_FAILED)
            : LOGGING_IN_USER_NOT_ACTIVE_MONO);
}
```

#### 4. 密码验证
```java
// 位置：turms-gateway/domain/session/service/UserService.java
public Mono<Boolean> authenticate(@NotNull Long userId, @Nullable String rawPassword) {
    return userRepository.findPassword(userId)
        .map(user -> passwordManager.matchesUserPassword(rawPassword, user.getPassword()))
        .defaultIfEmpty(false);  // 用户不存在返回false
}
```

### 用户注册流程

#### 1. 管理员API注册用户
```java
// 位置：turms-service/domain/user/access/admin/controller/UserController.java
@PostMapping
@RequiredPermission(USER_CREATE)
public Mono<ResponseEntity<ResponseDto<User>>> addUser(@RequestBody AddUserDTO addUserDTO) {
    Mono<User> userMono = userService.addUser(
        addUserDTO.getId(),
        addUserDTO.getPassword(),
        addUserDTO.getName(),
        addUserDTO.getIntro(),
        addUserDTO.getProfilePicture(),
        addUserDTO.getProfileAccessStrategy(),
        addUserDTO.getPermissionGroupId(),
        addUserDTO.getRegistrationDate(),
        addUserDTO.isActive()
    );
    return ResponseFactory.okIfTruthy(userMono);
}
```

#### 2. 用户服务创建用户
```java
// 位置：turms-service/domain/user/service/UserService.java
public Mono<User> addUser(
    @Nullable Long userId,
    @Nullable String rawPassword,
    @Nullable String name,
    @Nullable String intro,
    @Nullable String profilePicture,
    @Nullable @ValidProfileAccess ProfileAccessStrategy profileAccessStrategy,
    @Nullable Long permissionGroupId,
    @Nullable @PastOrPresent Date registrationDate,
    @Nullable Boolean isActive) {
    
    // 1. 参数验证
    // 2. 生成用户ID
    // 3. 密码加密
    // 4. 设置默认值
    // 5. 数据库事务操作
    return userRepository.inTransaction(session ->
        userRepository.insert(user, session)
            .then(userRelationshipGroupService.createRelationshipGroup(...))
            .then(userVersionService.upsert(...))
            .then(elasticsearchManager.upsertUserDoc(...))
    );
}
```

## 🏗️ 模块架构特点

### 1. 分离式设计
- **注册功能**：在 `turms-service` 中实现，负责用户数据管理
- **登录功能**：在 `turms-gateway` 中实现，负责会话和认证管理
- **职责清晰**：服务端管数据，网关管连接

### 2. 多重认证支持
```java
// 支持多种认证方式
- PasswordSessionIdentityAccessManager  // 密码认证
- JwtSessionIdentityAccessManager       // JWT令牌认证  
- LdapSessionIdentityAccessManager      // LDAP认证
- HttpSessionIdentityAccessManager      // HTTP会话认证
- NoopSessionIdentityAccessManager      // 无认证（测试用）
```

### 3. 安全机制
- **密码加密**：BCrypt、Salted SHA256等多种算法
- **会话管理**：支持多设备登录、设备踢出
- **权限控制**：基于角色的访问控制(RBAC)
- **状态验证**：用户激活状态、删除状态检查

### 4. 数据存储
```java
// 用户数据存储在MongoDB
- User集合：用户基本信息
- UserRelationship：用户关系数据  
- UserSettings：用户设置
- UserVersion：版本控制

// 会话数据存储在内存+Redis
- UserSession：用户会话信息
- 在线状态：分布式状态管理
```

## 📱 客户端集成

### 1. 登录请求格式
```protobuf
message CreateSessionRequest {
    int64 user_id = 1;
    optional string password = 2;
    UserStatus user_status = 3;
    DeviceType device_type = 4;
    map<string, string> device_details = 5;
    optional UserLocation location = 6;
}
```

### 2. 客户端SDK调用示例
```javascript
// JavaScript客户端
const turmsClient = new TurmsClient();
await turmsClient.userService.login(userId, password, UserStatus.AVAILABLE);

// Android客户端
val turmsClient = TurmsClient()
turmsClient.userService.login(userId, password, UserStatus.AVAILABLE).block()
```

### 3. 注册流程（通过管理员）
由于Turms没有直接的客户端注册API，用户注册通常通过以下方式：
1. **管理员注册**：通过管理后台创建用户
2. **集成第三方**：通过扩展插件集成外部用户系统
3. **自定义服务**：基于UserService.addUser()开发自定义注册API

## 🔧 配置和扩展

### 1. 认证配置
```yaml
turms:
  gateway:
    session:
      identity-access-management:
        enabled: true
        type: password  # password, jwt, ldap, http, noop
```

### 2. 用户注册配置  
```yaml
turms:
  service:
    user:
      activate-user-when-added: true
      delete-user-logically: true
      info:
        min-password-length: 6
        max-password-length: 32
```

### 3. 插件扩展点
```java
// 自定义认证器
public interface UserAuthenticator extends ExtensionPoint {
    Mono<Boolean> authenticate(Long userId, String password);
}

// 在线状态变化处理器
public interface UserOnlineStatusChangeHandler extends ExtensionPoint {
    Mono<Void> goOnline(UserSessionsManager manager, UserSession session);
    Mono<Void> goOffline(UserSessionsManager manager, UserSession session);
}
```

## 🎯 总结

**用户注册登录模块具有以下特点**：

1. **模块分离明确**：注册在service，登录在gateway
2. **认证方式灵活**：支持多种认证机制
3. **安全性完善**：密码加密、会话管理、权限控制
4. **扩展性强**：插件机制支持自定义认证
5. **高性能设计**：响应式编程、分布式会话管理
6. **企业级特性**：事务一致性、监控指标、故障恢复

这个设计充分体现了Turms作为专业级IM系统的架构水准，既保证了安全性，又提供了良好的扩展性和性能。