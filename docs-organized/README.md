# Turms 项目文档组织结构

本目录包含了 Turms 项目的所有文档，按照功能和项目模块进行分类整理。

## 📁 文档目录结构

### 🏗️ 架构文档 (architecture/)

#### 架构分析 (architecture/analysis/)
- `01-turms-architecture-analysis-report.md` - Turms 架构分析报告
- `turms-service-architecture-analysis.md` - 核心服务架构分析
- `availability-risk-analysis.md` - 可用性风险分析

#### 架构优化 (architecture/optimization/)
- `02-microservice-decomposition-feasibility-study.md` - 微服务拆分可行性研究
- `03-architecture-optimization-implementation-plan.md` - 架构优化实施方案
- `04-user-messaging-module-separation-difficulty-assessment.md` - 用户消息模块拆分难度评估

#### 架构设计 (architecture/design/)
- `用户标签系统架构设计方案.md` - 用户标签系统设计
- `架构设计文档.md` - API 网关架构设计

### 🚀 服务文档 (services/)

#### API Gateway 服务 (services/api-gateway/)
- `README.md` - API Gateway 服务说明
- `BUILD-SUCCESS-REPORT.md` - 构建成功报告
- `build-troubleshooting.md` - 构建故障排查
- `开发者指南.md` - 开发者使用指南
- `部署运维文档.md` - 部署和运维指南

#### 互动服务 (services/interaction-service/)
- `README.md` - 互动服务说明
- `SYSTEM_SUMMARY.md` - 系统功能总结
- `FINAL_STATUS.md` - 最终状态报告
- `CONTENT_SERVICE_MERGE_SUMMARY.md` - 内容服务合并总结
- `MOMENTS_USER_INTEGRATION.md` - 朋友圈用户集成
- `REDIS_MYSQL_LIKE_SYSTEM.md` - Redis+MySQL 点赞系统
- `MYSQL_REDIS_COMMENT_SYSTEM.md` - MySQL+Redis 评论系统
- `MySQL分析报告.md` - MySQL 数据库分析

#### 社交服务 (services/social-service/)
- `README.md` - 社交服务说明
- `CODE_CHANGES.md` - 代码变更记录

#### 标签服务 (services/tag-service/)
- `README.md` - 标签服务说明

#### 核心服务 (services/core-service/)
- `turms-service好友和群组管理分析报告.md` - 好友和群组管理分析

### 💻 开发文档 (development/)

#### 开发指南 (development/guides/)
- `新业务功能开发指南.md` - 新功能开发指南
- `Turms业务功能开发路径总结.md` - 业务功能开发路径
- `mobile-phone-registration-implementation-guide.md` - 手机注册实现指南
- `标签系统模块使用指南.md` - 标签系统使用指南

#### 问题修复 (development/fixes/)
- `CLASS_DUPLICATE_FIX.md` - 类重复问题修复
- `CHANNEL_OPTION_FIX.md` - Channel Option 问题修复
- `UPDATE_SYNTAX_FIX.md` - 更新语法问题修复
- `DEPENDENCY_FIX_RECORD.md` - 依赖问题修复记录
- `修改记录_依赖问题解决.md` - 依赖问题解决记录
- `修改记录_Spring Bean冲突解决.md` - Spring Bean 冲突解决
- `修改记录_社交关系服务创建.md` - 社交关系服务创建记录

##### 最新修复报告 (development/fixes/recent-fixes/)
- `CORS跨域问题完整修复报告.md` - CORS 跨域问题修复
- `缺失API接口完整修复报告.md` - 缺失 API 接口修复
- `admin-interaction-API完整修复报告.md` - Admin 互动 API 修复
- `admin-api-路由修复报告.md` - Admin API 路由修复
- `getLikes-method-fix.md` - getLikes 方法修复
- `getUserFriends-method-fix.md` - getUserFriends 方法修复
- `路由冲突修复报告.md` - 路由冲突修复报告

#### 故障排查 (development/troubleshooting/)
- `COMPILATION_STATUS.md` - 编译状态报告
- `turms-admin-url-configuration-issues.md` - Admin URL 配置问题

### 📋 API接口文档 (api/)

#### Gateway API (api/gateway/)
- `API接口文档.md` - API Gateway 接口文档

#### 朋友圈 API (api/moments/)
- `朋友圈API接口文档.md` - 朋友圈功能API接口

### 🚢 部署文档 (deployment/)

#### 部署设置 (deployment/setup/)
- `服务启动指南.md` - 服务启动和部署指南

#### 启动报告 (deployment/startup-reports/)
- `项目启动完成报告.md` - 项目启动完成状态报告
- `项目启动修复完成报告.md` - 项目启动修复完成报告

### 📊 分析报告 (analysis/)

#### 业务分析 (analysis/business/)
- `群组管理系统分析报告.md` - 群组管理系统分析
- `user-registration-analysis.md` - 用户注册分析
- `user-authentication-modules-location.md` - 用户认证模块位置
- `turms-permission-management-analysis.md` - 权限管理分析
- `mobile-phone-registration-solution.md` - 手机注册解决方案
- `标签系统重新设计方案.md` - 标签系统重新设计
- `用户模块与即时通讯模块拆分难度评估.md` - 模块拆分难度评估

#### 技术分析 (analysis/technical/)
- `message-service-microservice-analysis.md` - 消息服务微服务分析
- `turms-service-messageservice-dependency-analysis.md` - 服务依赖分析
- `phone-registration-code-changes.md` - 手机注册代码变更分析
- `turms-admin-analysis.md` - Turms Admin 前端分析
- `turms-interaction-service-analysis.md` - 互动服务分析
- `turms-admin-认证配置分析报告.md` - Admin 认证配置分析
- `turms-admin-interaction-integration.md` - Admin 与互动服务集成分析
- `Redis配置搜索结果.md` - Redis 配置分析结果

#### 依赖分析 (analysis/dependency/)
- `dependency_analysis.md` - 依赖关系分析

### 📦 项目文档 (project/)

#### 根目录 (project/root/)
- `README_zh.md` - 项目中文说明
- `CLAUDE.md` - Claude AI 工作指南

#### 插件 (project/plugin/)
- `plugin-demo-CLAUDE.md` - 插件演示 Claude 配置

#### 总结 (project/summary/)
- `INTERACTION_SERVICE_SUMMARY.md` - 互动服务总结

## 📈 文档统计

- **总文档数**: 74 个 Markdown 文件
- **主要分类**: 7 大类（架构、服务、开发、API、部署、分析、项目）
- **子分类**: 25 个具体分类
- **覆盖范围**: 架构设计、服务开发、API接口、部署运维、问题修复、业务分析、最新修复报告

## 🔍 快速导航

### 新手入门
1. 阅读 `project/root/README_zh.md` - 了解项目概况
2. 查看 `deployment/setup/服务启动指南.md` - 学习如何启动服务
3. 参考 `development/guides/新业务功能开发指南.md` - 开始开发

### 架构了解
1. `architecture/analysis/01-turms-architecture-analysis-report.md` - 整体架构
2. `architecture/design/` - 具体设计方案
3. `services/` - 各服务详细说明

### 问题解决
1. `development/troubleshooting/` - 故障排查
2. `development/fixes/` - 已知问题修复
3. `analysis/technical/` - 技术分析报告

### API使用
1. `api/gateway/` - API Gateway 接口
2. `api/moments/` - 朋友圈接口
3. `services/*/README.md` - 各服务API说明

---

**注意**: 所有文档均保持原始内容不变，仅进行分类整理。如需查看最新版本，请参考各服务目录下的原始文档。