# Turms Social Service - 社交推荐与关系图分析服务

## 项目简介

Turms Social Service 是基于 Spring Boot 构建的独立社交推荐和关系图分析服务，专注于提供智能的社交推荐算法和深度的社交网络分析功能。

## 核心功能

### 智能推荐引擎
- **混合推荐算法**: 结合协同过滤、内容推荐、热度推荐等多种策略
- **协同过滤推荐**: 基于用户行为相似性的 UserCF 和 ItemCF 算法
- **基于内容推荐**: 根据用户画像、兴趣标签、技能匹配进行推荐
- **推荐解释系统**: 提供推荐原因的详细解释和可解释性分析
- **反馈学习机制**: 收集用户反馈，持续优化推荐算法

### 社交关系图分析
- **社交网络构建**: 构建用户的多度社交关系网络图
- **影响力分析**: 基于多种中心性指标计算用户社交影响力
- **社交路径发现**: 计算用户之间的社交路径和关系强度  
- **社区发现算法**: 识别用户所在的社交社区和兴趣群体
- **趋势分析**: 分析社交网络的变化趋势和发展预测
- **网络统计**: 提供全面的社交网络统计和洞察分析

## 技术架构

- **框架**: Spring Boot 3.4.4
- **Java版本**: Java 21
- **Web框架**: Spring WebFlux (响应式编程)
- **数据库**: MongoDB (配置但未启用)
- **构建工具**: Maven

## 服务配置

### 端口配置
- **HTTP端口**: 8086
- **管理端点**: `/actuator/health`, `/actuator/info`

### 环境配置
- **开发环境** (`dev`): 宽松限制，允许匿名访问
- **生产环境** (`prod`): 严格限制，需要认证
- **测试环境** (`test`): 独立测试数据库

## API 接口

### 健康检查
```bash
GET /social/health       # 服务健康状态
GET /social/info         # 服务信息
GET /social/version      # 版本信息
```

### 智能推荐 API
```bash
# 好友推荐
GET /social/recommendations/friends/{userId}           # 获取好友推荐列表
GET /social/recommendations/collaborative/{userId}     # 协同过滤推荐
GET /social/recommendations/content-based/{userId}     # 基于内容推荐
GET /social/recommendations/explain/{userId}/{recommendedUserId}  # 推荐解释
POST /social/recommendations/feedback                  # 提交推荐反馈

# 推荐参数示例
# ?algorithm=hybrid&limit=10&threshold=0.5
# ?algorithm=collaborative&limit=20
# ?algorithm=content-based&limit=15
```

### 社交关系图分析 API
```bash
# 社交网络分析
GET /social/graph/{userId}/network        # 获取用户社交关系图
GET /social/graph/{userId}/influence      # 分析用户影响力
GET /social/graph/path/{userId1}/{userId2} # 发现社交路径
GET /social/graph/{userId}/communities    # 社区发现
GET /social/graph/{userId}/trends         # 趋势分析
GET /social/graph/{userId}/statistics     # 网络统计

# 分析参数示例
# ?depth=2&maxNodes=50          # 网络深度和节点数限制
# ?maxDegrees=6                 # 最大分离度数
# ?days=30                      # 趋势分析时间窗口
```

## 快速开始

### 编译服务
```bash
mvn clean compile -pl turms-social-service
```

### 启动服务
```bash
# 开发环境启动
mvn spring-boot:run -pl turms-social-service -Dspring-boot.run.profiles=dev

# 后台启动
nohup mvn spring-boot:run -pl turms-social-service -Dspring-boot.run.profiles=dev > logs/social-service.log 2>&1 &
```

### 验证服务
```bash
# 检查服务状态
curl http://localhost:8086/social/health

# 查看服务信息
curl http://localhost:8086/social/info

# 测试推荐API
curl "http://localhost:8086/social/recommendations/friends/12345?algorithm=hybrid&limit=5"
curl http://localhost:8086/social/recommendations/collaborative/12345

# 测试社交图分析API  
curl "http://localhost:8086/social/graph/12345/network?depth=2&maxNodes=20"
curl http://localhost:8086/social/graph/12345/influence
```

## 配置参数

### 核心配置
```yaml
turms:
  social-service:
    # 推荐引擎配置
    recommendation:
      enabled: true                        # 启用推荐功能
      max-recommended-friends: 20          # 最大推荐好友数
      algorithms:
        hybrid:
          enabled: true                    # 混合推荐算法
          weight-collaborative: 0.4        # 协同过滤权重
          weight-content: 0.4              # 内容推荐权重  
          weight-trending: 0.2             # 热度推荐权重
        collaborative:
          user-based-enabled: true         # 基于用户的协同过滤
          item-based-enabled: true         # 基于物品的协同过滤
          min-similarity: 0.1              # 最小相似度阈值
        content-based:
          tag-weight: 0.6                  # 标签匹配权重
          profile-weight: 0.4              # 画像匹配权重
      
    # 社交图分析配置  
    graph-analysis:
      enabled: true                        # 启用图分析功能
      max-network-depth: 3                 # 最大网络深度
      max-network-nodes: 1000              # 最大网络节点数
      influence-algorithms:
        degree-centrality: 0.25            # 度中心性权重
        betweenness-centrality: 0.20       # 介数中心性权重
        closeness-centrality: 0.20         # 接近中心性权重
        eigenvector-centrality: 0.20       # 特征向量中心性权重
        pagerank: 0.15                     # PageRank权重
      
    # 性能配置
    performance:
      default-page-size: 20                # 默认分页大小
      max-page-size: 100                   # 最大分页大小
      enable-cache: true                   # 启用缓存
      cache-expire-minutes: 30             # 缓存过期时间
      async-processing: true               # 异步处理
```

## 项目结构

```
turms-social-service/
├── src/main/java/im/turms/social/
│   ├── SocialServiceApplication.java     # 主启动类
│   ├── config/                          # 配置类
│   │   ├── SocialServiceConfiguration.java
│   │   └── SocialServiceProperties.java
│   ├── controller/                      # 控制器
│   │   ├── HealthController.java        # 健康检查
│   │   ├── FriendRecommendationController.java  # 好友推荐
│   │   └── SocialGraphController.java   # 社交关系图分析
│   └── service/                         # 业务服务
│       ├── RecommendationEngineService.java     # 推荐引擎
│       └── SocialGraphAnalysisService.java      # 图分析
├── src/main/resources/
│   └── application.yml                  # 应用配置
├── src/test/java/
│   └── im/turms/social/
│       └── SocialServiceApplicationTest.java
├── pom.xml                             # Maven配置
└── README.md                           # 项目说明
```

## 开发指南

### 添加新推荐算法
1. 在 `RecommendationEngineService` 中实现新算法
2. 在 `FriendRecommendationController` 中添加新接口
3. 更新配置文件中的算法权重
4. 编写算法测试用例

### 扩展图分析功能
1. 在 `SocialGraphAnalysisService` 中实现新分析方法
2. 在 `SocialGraphController` 中添加新分析端点
3. 优化图算法性能和内存使用
4. 添加可视化数据格式支持

### 测试指南
```bash
# 运行单元测试
mvn test -pl turms-social-service

# 运行集成测试
mvn verify -pl turms-social-service
```

## 部署说明

### 开发环境
- 直接使用 Maven 启动
- 允许匿名访问，便于测试
- 详细的调试日志

### 生产环境
- 构建 JAR 包部署
- 启用认证和权限控制
- 优化的日志级别和缓存策略

## 监控和运维

### 健康检查
- Spring Boot Actuator 端点
- 自定义健康检查逻辑
- 服务状态监控

### 日志管理
- 结构化日志输出
- 分环境的日志级别
- 日志文件轮转配置

## 版本历史

- **v1.0.0**: 初始版本，基础好友和群组功能
- 支持好友关系管理
- 支持群组管理
- 提供RESTful API接口

## 贡献指南

1. Fork 项目
2. 创建特性分支
3. 提交更改
4. 发起 Pull Request

## 许可证

Apache License 2.0