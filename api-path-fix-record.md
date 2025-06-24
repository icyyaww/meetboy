# API路径修复记录

## 修复原因
前端Admin调用审核相关API时，使用的路径缺少 `/admin` 前缀，导致404错误。

## 问题分析
- 后端控制器路径：`/interaction/admin/moderation/*`
- 前端API调用路径：`/interaction/moderation/*` 
- 缺少 `/admin` 路径段导致路由匹配失败

## 修复内容
修复文件：`turms-admin/ui/src/apis/interaction-apis.ts`

### 修复的API方法路径
1. `fetchModerationRules`: `/interaction/moderation/rules` → `/interaction/admin/moderation/rules`
2. `fetchModerationRulesPage`: `/interaction/moderation/rules/page` → `/interaction/admin/moderation/rules/page`
3. `createModerationRule`: `/interaction/moderation/rules` → `/interaction/admin/moderation/rules`
4. `updateModerationRule`: `/interaction/moderation/rules/{id}` → `/interaction/admin/moderation/rules/{id}`
5. `deleteModerationRules`: `/interaction/moderation/rules` → `/interaction/admin/moderation/rules`
6. `enableModerationRule`: `/interaction/moderation/rules/{id}/enable` → `/interaction/admin/moderation/rules/{id}/enable`
7. `disableModerationRule`: `/interaction/moderation/rules/{id}/disable` → `/interaction/admin/moderation/rules/{id}/disable`
8. `fetchModerationLogs`: `/interaction/moderation/logs` → `/interaction/admin/moderation/logs`
9. `fetchModerationLogsPage`: `/interaction/moderation/logs/page` → `/interaction/admin/moderation/logs/page`
10. `fetchModerationStats`: `/interaction/moderation/stats` → `/interaction/admin/moderation/stats`
11. `fetchModerationTrend`: `/interaction/moderation/trend` → `/interaction/admin/moderation/trend`
12. `exportModerationLogs`: `/interaction/moderation/logs/export` → `/interaction/admin/moderation/logs/export`

## 验证结果
修复后，`http://127.0.0.1:8531/interaction/admin/moderation/rules/page` 接口正常响应，返回审核规则分页数据。

## 第二次修复：Vue组件URL属性
### 发现问题
前端构建后的文件中仍包含旧路径，因为Vue组件中的`:url`属性没有更新。

### 修复内容
修复文件：`turms-admin/ui/src/components/pages/interaction/moderation/index.vue`

修复的URL属性：
1. `:url="'/interaction/moderation/pending'"` → `:url="'/interaction/admin/moderation/pending'"`
2. `:url="'/interaction/moderation/rules'"` → `:url="'/interaction/admin/moderation/rules'"`  
3. `:url="'/interaction/moderation/logs'"` → `:url="'/interaction/admin/moderation/logs'"`

### 验证结果
- CORS预检请求(OPTIONS)：返回200 OK
- 实际GET请求：返回200，数据正常
- 服务端日志：正常接收和处理请求

## 修复时间
- 第一次修复：2025-06-24 13:55
- 第二次修复：2025-06-24 14:15

## 修复内容确认
- [x] 所有审核相关API路径已更新
- [x] 所有Vue组件URL属性已更新
- [x] 前端重新构建完成
- [x] 保持路径格式一致性
- [x] CORS预检请求正常
- [x] API功能测试通过