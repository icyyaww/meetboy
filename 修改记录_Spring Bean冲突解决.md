# Spring Bean 冲突问题解决记录

## 问题描述
API网关启动时出现Bean创建异常：
```
Error creating bean with name 'requestRateLimiterGatewayFilterFactory': 
Unsatisfied dependency expressed through method 'requestRateLimiterGatewayFilterFactory' parameter 1: 
No qualifying bean of type 'org.springframework.cloud.gateway.filter.ratelimit.KeyResolver' available: 
expected single matching bean but found 2: ipKeyResolver,userKeyResolver
```

## 问题分析
1. Spring Cloud Gateway 的 `requestRateLimiterGatewayFilterFactory` 需要一个 `KeyResolver` 类型的 Bean
2. 在 `GatewayConfig.java` 中定义了两个 `KeyResolver` Bean：
   - `ipKeyResolver`: 基于IP地址的限流Key解析器
   - `userKeyResolver`: 基于用户的限流Key解析器
3. Spring Framework 无法确定应该注入哪一个Bean，导致依赖注入失败

## 解决方案
在 `/home/icyyaww/program/meetboy/turms-api-gateway/src/main/java/im/turms/apigateway/config/GatewayConfig.java` 中进行以下修改：

### 1. 添加 @Primary 注解导入
```java
// 添加到导入部分
import org.springframework.context.annotation.Primary;
```

### 2. 标记主要的KeyResolver Bean
将 `ipKeyResolver` 方法标记为 `@Primary`：

**修改前：**
```java
@Bean
public KeyResolver ipKeyResolver() {
```

**修改后：**
```java
@Bean
@Primary
public KeyResolver ipKeyResolver() {
```

## 解决方案说明
- `@Primary` 注解告诉Spring当存在多个相同类型的Bean时，优先选择这个Bean
- 选择 `ipKeyResolver` 作为主要Bean的原因：
  1. 当前所有路由都在使用 `ipKeyResolver()`
  2. 基于IP的限流策略更通用和稳定
  3. `userKeyResolver` 保留作为备用选项，供将来特定路由使用

## 验证结果
修改完成后，API网关编译成功：
```
[INFO] BUILD SUCCESS
[INFO] Total time:  1.017 s
[INFO] Finished at: 2025-06-19T15:36:55+08:00
```

## 当前路由配置
所有API路由都使用 `ipKeyResolver()` 进行限流：
- `/api/v1/im/**` → turms-service
- `/api/v1/tags/**` → turms-tag-service  
- `/api/v1/social/**` → turms-social-service (预留)
- `/api/v1/content/**` → turms-content-service (预留)
- `/api/v1/interaction/**` → turms-interaction-service (预留)
- `/api/v1/recommendation/**` → turms-recommendation-service (预留)

## 备注
- `userKeyResolver` Bean 仍然可用，可以在需要基于用户限流的特定路由中使用
- 如果将来需要切换默认的KeyResolver，只需将 `@Primary` 注解移动到另一个Bean上

## 修改时间
2025-06-19 15:36:55

## 执行用户
使用 `sudo -u icyyaww` 确保所有操作使用正确的用户权限