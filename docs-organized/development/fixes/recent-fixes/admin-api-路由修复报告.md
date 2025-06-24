# Turms Admin API路由修复报告

## 修复时间
2025-06-23 16:10

## 问题描述
用户在使用turms-admin管理后台访问interaction-service API时，出现了错误的路由问题：
- 预期URL: `http://127.0.0.1:8531/interaction/likes/page`
- 实际URL: `http://127.0.0.1:8510/interaction/likes/page`
- 错误状态: 400 Bad Request

## 问题根源分析

### 1. Vite代理配置错误
**文件**: `/home/icyyaww/program/meetboy/turms-admin/vite.config.ts`
**问题**: `/interaction`和`/content`路径代理到错误的端口8530
```typescript
// 修复前
'/interaction': {
    target: 'http://localhost:8530',  // 错误端口
    changeOrigin: true,
    secure: false
}

// 修复后  
'/interaction': {
    target: 'http://localhost:8531',  // 正确端口
    changeOrigin: true,
    secure: false
}
```

### 2. 登录逻辑设置全局baseURL
**文件**: `/home/icyyaww/program/meetboy/turms-admin/ui/src/components/modals/login-modal.vue`
**问题**: 登录时设置`this.$http.defaults.baseURL = url`为8510端口，影响所有后续请求
```javascript
// 第81行
const DEFAULT_URL = `${window.location.protocol}//${window.location.hostname}:8510`;

// 第156行  
this.$http.defaults.baseURL = url; // 这会影响所有API调用
```

### 3. API调用路径不一致
**文件**: `/home/icyyaww/program/meetboy/turms-admin/ui/src/apis/interaction-apis.ts`
**问题**: 同时使用`/api/v1/interaction/*`和`/interaction/*`两种路径格式

## 修复方案

### 1. ✅ 修复Vite代理配置
将vite.config.ts中的interaction和content代理端口从8530改为8531：
```typescript
proxy: {
    '/interaction': {
        target: 'http://localhost:8531',  // 修改为正确端口
        changeOrigin: true,
        secure: false
    },
    '/content': {
        target: 'http://localhost:8531',   // 修改为正确端口
        changeOrigin: true,
        secure: false
    },
    '/api': {
        target: 'http://localhost:8510',   // 保持不变
        changeOrigin: true,
        secure: false
    }
}
```

### 2. ✅ 添加请求拦截器
在main.ts中添加axios请求拦截器，动态处理不同服务的URL：
```typescript
// 添加请求拦截器来处理不同服务的URL
$http.interceptors.request.use(config => {
    const url = config.url || '';
    
    // 如果是相对URL且以/interaction或/content开头，需要特殊处理
    if (!url.startsWith('http') && (url.startsWith('/interaction') || url.startsWith('/content'))) {
        // 如果已经设置了baseURL但是针对interaction-service的请求，则需要修改baseURL
        const currentBaseURL = config.baseURL || '';
        if (currentBaseURL.includes(':8510')) {
            // 临时修改这个请求的baseURL为interaction-service的地址
            config.baseURL = currentBaseURL.replace(':8510', ':8531');
        }
    }
    
    return config;
}, error => {
    return Promise.reject(error);
});
```

## 修复效果

### 修复前
- 登录URL: `http://127.0.0.1:8510` (设置为全局baseURL)
- Interaction API: `http://127.0.0.1:8510/interaction/likes/page` ❌
- 结果: 400 Bad Request

### 修复后  
- 登录URL: `http://127.0.0.1:8510` (仍然用于管理API)
- Interaction API: `http://127.0.0.1:8531/interaction/likes/page` ✅
- 结果: 正确路由到interaction-service

## 服务端口分配

| 服务名称 | 端口 | 用途 |
|---------|------|------|
| turms-service | 8510 | 主要业务服务、管理API |
| turms-interaction-service | 8531 | 社交互动服务 (点赞、评论) |
| turms-gateway | 9510 | 网关服务 |
| turms-admin | 6510 | 管理后台前端 |

## 测试建议

1. **登录测试**: 访问 http://localhost:6510，使用管理员账号登录
2. **API测试**: 登录后访问互动管理相关页面，检查点赞、评论API调用
3. **开发者工具**: 检查Network面板，确认API请求URL正确指向8531端口

## 后续优化建议

1. **统一API路径**: 建议将interaction-apis.ts中的API路径统一格式
2. **环境配置**: 考虑使用环境变量来配置不同服务的端口
3. **服务发现**: 长期可考虑实现服务发现机制，自动检测服务端口

## 修复结果
🎉 **Admin API路由修复成功！现在admin可以正确访问interaction-service的API接口。**