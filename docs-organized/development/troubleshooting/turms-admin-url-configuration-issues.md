# Turms Admin URL配置问题分析报告

## 问题描述
在turms-admin项目中，访问interaction-service的API时，请求的URL使用了错误的端口8510，而应该使用8531端口。

## 根本原因分析

### 1. vite.config.ts配置冲突
**文件路径：** `/home/icyyaww/program/meetboy/turms-admin/vite.config.ts`

**问题：**
- 第16-20行：`/interaction`路径代理到`http://localhost:8530`
- 第26-30行：`/api`路径代理到`http://localhost:8510`

**proxy.config.js中的正确配置：**
- 第7行：`/interaction`路径代理到`http://localhost:8531`

存在配置不一致的问题。

### 2. 登录默认URL配置错误
**文件路径：** `/home/icyyaww/program/meetboy/turms-admin/ui/src/components/modals/login-modal.vue`

**问题：**
- 第81行：`const DEFAULT_URL = \`\${window.location.protocol}//\${window.location.hostname}:8510\``
- 默认端口使用8510，但interaction-service运行在8531端口

**影响：**
- 第156行：`this.$http.defaults.baseURL = url` 会将用户输入的URL设置为axios的基础URL
- 这会影响所有后续的HTTP请求

### 3. API路径定义不一致
**文件路径：** `/home/icyyaww/program/meetboy/turms-admin/ui/src/apis/interaction-apis.ts`

**问题：**
- 第6行和第10行：使用`/api/v1/interaction/likes`
- 第14行及以后：使用`/interaction/likes`
- 路径前缀不统一

## 需要修复的配置

### 1. vite.config.ts修复
```javascript
// 修改前
'/interaction': {
    target: 'http://localhost:8530',
    changeOrigin: true,
    secure: false
}

// 修改后
'/interaction': {
    target: 'http://localhost:8531',
    changeOrigin: true,
    secure: false
}
```

### 2. login-modal.vue修复
```javascript
// 修改前
const DEFAULT_URL = `${window.location.protocol}//${window.location.hostname}:8510`;

// 修改后（根据实际需要选择）
// 如果需要连接到interaction-service：
const DEFAULT_URL = `${window.location.protocol}//${window.location.hostname}:8531`;
// 或者如果需要连接到主服务：
const DEFAULT_URL = `${window.location.protocol}//${window.location.hostname}:8510`;
```

### 3. API路径统一化
需要确定使用哪种路径格式：
- 使用`/api/v1/interaction/*`格式（需要在代理中配置）
- 或使用`/interaction/*`格式（当前proxy.config.js的配置）

## 建议的修复步骤

1. **确定服务架构：** 明确interaction-service是否应该通过代理访问或直接访问
2. **统一代理配置：** 使vite.config.ts和proxy.config.js配置一致
3. **修复默认URL：** 根据实际部署情况修改login-modal.vue中的默认端口
4. **统一API路径：** 确保interaction-apis.ts中的路径格式一致
5. **测试验证：** 验证修改后的配置是否正常工作

## 相关文件列表
1. `/home/icyyaww/program/meetboy/turms-admin/vite.config.ts`
2. `/home/icyyaww/program/meetboy/turms-admin/ui/proxy.config.js`
3. `/home/icyyaww/program/meetboy/turms-admin/ui/src/components/modals/login-modal.vue`
4. `/home/icyyaww/program/meetboy/turms-admin/ui/src/apis/interaction-apis.ts`
5. `/home/icyyawv/program/meetboy/turms-admin/ui/src/store/index.ts`
6. `/home/icyyaww/program/meetboy/turms-admin/ui/src/main.ts`

生成时间：2025-06-23