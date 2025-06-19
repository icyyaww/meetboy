# Turms API Gateway

Turms API Gateway 是 Turms 即时通讯系统的统一API网关，为所有后端服务提供统一的入口点。

## 主要功能

### 🚀 核心特性

- **统一入口管理** - 所有API请求的单一入口点
- **服务路由** - 智能路由到后端微服务
- **负载均衡** - 支持多种负载均衡算法
- **认证授权** - 基于JWT的统一身份认证
- **限流保护** - 基于Redis的分布式限流
- **熔断降级** - 使用Resilience4j实现服务熔断
- **监控可观测** - 集成Prometheus metrics
- **CORS支持** - 跨域资源共享配置

### 🔧 技术栈

- **Spring Cloud Gateway** - 响应式网关框架
- **Spring Security** - 安全框架
- **JWT** - JSON Web Token认证
- **Redis** - 限流和缓存
- **Consul** - 服务发现
- **Resilience4j** - 熔断器
- **Micrometer** - 指标收集
- **Prometheus** - 监控指标

## 架构设计

```
客户端 → API网关 → 后端服务
    ├── WebSocket/TCP → turms-gateway → turms-service (即时通讯)
    ├── HTTP API → turms-service (IM核心API)
    ├── HTTP API → turms-tag-service (标签服务)
    ├── HTTP API → turms-social-service (社交关系服务)
    ├── HTTP API → turms-content-service (内容服务)
    ├── HTTP API → turms-interaction-service (互动服务)
    ├── HTTP API → turms-recommendation-service (推荐服务)
    └── HTTP API → turms-admin (管理界面)
```

## 路由配置

### API路由规则

| 路径模式 | 目标服务 | 说明 |
|---------|---------|------|
| `/websocket/**` | turms-gateway:10510 | WebSocket连接 |
| `/tcp/**` | turms-gateway:9510 | TCP代理 |
| `/api/v1/im/**` | turms-service | 即时通讯API |
| `/api/v1/tags/**` | turms-tag-service | 标签服务API |
| `/api/v1/social/**` | turms-social-service | 社交关系API |
| `/api/v1/content/**` | turms-content-service | 内容管理API |
| `/api/v1/interaction/**` | turms-interaction-service | 互动功能API |
| `/api/v1/recommendation/**` | turms-recommendation-service | 推荐算法API |
| `/admin/**` | turms-admin | 管理界面API |

### 过滤器链

1. **LoggingGlobalFilter** - 请求日志记录
2. **AuthenticationGatewayFilter** - JWT认证
3. **RateLimitFilter** - 请求限流
4. **CircuitBreakerFilter** - 熔断器
5. **LoadBalancerFilter** - 负载均衡

## 配置说明

### 环境配置

支持多环境配置：
- `dev` - 开发环境
- `prod` - 生产环境

### 主要配置项

```yaml
turms:
  gateway:
    jwt:
      secret: "your-jwt-secret-key"
      expiration: 86400
    rate-limit:
      default:
        replenish-rate: 10
        burst-capacity: 20
    circuit-breaker:
      default:
        failure-rate-threshold: 50.0
        wait-duration-in-open-state: 30s
```

## 部署指南

### 1. 环境要求

- Java 21+
- Redis 6.0+
- Consul 1.9+ (生产环境)

### 2. 本地开发

```bash
# 启动Redis
docker run -d --name redis -p 6379:6379 redis:latest

# 启动网关
cd turms-api-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3. 生产部署

```bash
# 构建
mvn clean package

# 运行
java -jar target/turms-api-gateway-${version}.jar --spring.profiles.active=prod
```

### 4. Docker部署

```dockerfile
FROM openjdk:21-jre-slim
COPY target/turms-api-gateway-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 监控与维护

### 健康检查

- **健康检查端点**: `GET /actuator/health`
- **网关路由信息**: `GET /actuator/gateway/routes`
- **指标监控**: `GET /actuator/prometheus`

### 日志配置

```yaml
logging:
  level:
    im.turms.apigateway: DEBUG
    org.springframework.cloud.gateway: INFO
  file:
    name: logs/turms-api-gateway.log
```

### 监控指标

主要监控指标：
- 请求QPS和响应时间
- 服务可用性和错误率
- 限流和熔断触发次数
- JVM内存和GC情况

## 安全配置

### JWT认证

```yaml
turms:
  gateway:
    jwt:
      secret: "your-secret-key-must-be-at-least-32-characters"
      expiration: 3600  # 生产环境建议1小时
```

### CORS配置

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOriginPatterns: "*"  # 生产环境请配置具体域名
            allowedMethods: [GET, POST, PUT, DELETE]
```

## 故障排查

### 常见问题

1. **服务不可用** - 检查后端服务状态和网络连接
2. **认证失败** - 验证JWT密钥配置和token有效性
3. **限流触发** - 调整限流参数或检查客户端请求频率
4. **熔断器打开** - 检查后端服务健康状况

### 调试模式

```yaml
logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    reactor.netty: DEBUG
```

## 性能调优

### JVM参数

```bash
-Xms512m -Xmx1g
-XX:+UseG1GC
-XX:+UseStringDeduplication
-XX:MaxGCPauseMillis=100
```

### 连接池配置

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
```

## 扩展开发

### 自定义过滤器

```java
@Component
public class CustomGatewayFilterFactory 
    extends AbstractGatewayFilterFactory<CustomGatewayFilterFactory.Config> {
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // 自定义逻辑
            return chain.filter(exchange);
        };
    }
}
```

### 自定义限流键解析器

```java
@Bean
public KeyResolver customKeyResolver() {
    return exchange -> {
        // 自定义限流键逻辑
        return Mono.just("custom-key");
    };
}
```

---

**联系我们**

如有问题或建议，请提交Issue或联系开发团队。