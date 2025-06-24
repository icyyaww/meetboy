# Turms标签服务 (turms-tag-service)

Turms标签系统的独立微服务实现，作为turms-parent的子模块。

## 🌟 功能特性

- 🏷️ **标签管理**：创建、编辑、删除标签
- 📁 **分类管理**：多领域标签分类
- 👥 **用户标签关系**：用户与标签的关联管理
- 🤖 **智能推荐**：基于算法的标签推荐
- ☁️ **标签云**：热门标签展示
- 🔍 **内容发现**：基于标签的内容发现

## 🚀 快速开始

### 前置条件

- Java 21+
- Maven 3.6+
- MongoDB 7.0+

### 构建和运行

```bash
# 在项目根目录（turms-parent）下编译
mvn clean compile -pl turms-tag-service

# 运行服务
mvn spring-boot:run -pl turms-tag-service

# 或者打包后运行
mvn clean package -pl turms-tag-service
java -jar turms-tag-service/target/turms-tag-service.jar
```

### 健康检查

服务启动后，可以访问以下端点检查状态：

```bash
# 基础健康检查
curl http://localhost:8085/api/health

# 详细信息
curl http://localhost:8085/api/health/info

# Spring Actuator端点
curl http://localhost:8085/actuator/health
```

## ⚙️ 配置

主要配置项在 `application.yml` 中：

```yaml
turms:
  tag-service:
    port: 8085
    mongo:
      host: localhost
      port: 27017
      database: turms-tag
    tag:
      enabled: true
      max-tags-per-user: 100
      allow-custom-tags: true
      recommendation:
        enabled: true
        algorithm-type: hybrid
```

## 🏗️ 架构设计

本服务作为Turms生态系统的一部分，采用以下架构原则：

- **独立部署**：可以独立于其他Turms服务运行
- **响应式编程**：基于Spring WebFlux和Project Reactor
- **模块化设计**：清晰的分层架构
- **配置驱动**：通过配置文件控制功能开关

## 📁 项目结构

```
turms-tag-service/
├── src/main/java/im/turms/tag/
│   ├── TagServiceApplication.java    # 启动类
│   ├── config/                       # 配置类
│   ├── controller/                   # 控制器
│   ├── service/                      # 业务服务层
│   ├── repository/                   # 数据访问层
│   └── model/                        # 数据模型
├── src/main/resources/
│   └── application.yml               # 配置文件
└── src/test/java/                    # 测试代码
```

## 🔧 开发指南

### 环境配置

支持多环境配置：

- `dev`：开发环境，详细日志，放宽限制
- `test`：测试环境，独立数据库
- `prod`：生产环境，严格限制，优化性能

使用方式：
```bash
java -jar turms-tag-service.jar --spring.profiles.active=prod
```

### 添加新功能

1. 在相应的包下创建业务类
2. 添加配置项到 `TagServiceProperties`
3. 实现REST API控制器
4. 编写单元测试

## 🧪 测试

```bash
# 运行测试
mvn test -pl turms-tag-service

# 集成测试
mvn verify -pl turms-tag-service
```

## 📚 相关文档

- [Turms项目文档](https://turms-im.github.io/docs/)
- [标签系统架构设计](../docs/用户标签系统架构设计方案.md)
- [开发路径指南](../docs/Turms业务功能开发路径总结.md)

## 📄 许可证

Apache License 2.0

---

**注意**：本服务目前处于初始开发阶段，后续会根据需求添加具体的业务功能实现。