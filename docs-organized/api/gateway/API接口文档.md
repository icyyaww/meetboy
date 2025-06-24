# Turms API网关接口文档

## 文档信息

| 项目 | 值 |
|------|-----|
| 文档名称 | Turms API网关接口文档 |
| 版本 | v1.0.0 |
| 创建日期 | 2025-01-19 |
| 最后更新 | 2025-01-19 |
| 基础URL | https://api.turms.im |
| 网关端口 | 8080 |

## 1. 认证说明

### 1.1 认证方式

Turms API网关使用JWT Bearer Token进行认证。

**请求头格式：**
```http
Authorization: Bearer <your-jwt-token>
```

### 1.2 获取Token

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "your-username",
  "password": "your-password"
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "expires": 3600,
    "user": {
      "id": "12345",
      "username": "username",
      "role": "USER"
    }
  }
}
```

### 1.3 Token刷新

```http
POST /api/v1/auth/refresh
Authorization: Bearer <current-token>
```

## 2. 公共响应格式

### 2.1 成功响应

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": "2025-01-19T10:30:00.000Z"
}
```

### 2.2 错误响应

```json
{
  "code": 400,
  "message": "请求参数错误",
  "error": "INVALID_PARAMETER",
  "timestamp": "2025-01-19T10:30:00.000Z"
}
```

### 2.3 状态码说明

| 状态码 | 说明 | 示例 |
|--------|------|------|
| 200 | 成功 | 操作成功完成 |
| 400 | 请求错误 | 参数格式错误 |
| 401 | 未认证 | Token无效或过期 |
| 403 | 无权限 | 权限不足 |
| 404 | 未找到 | 资源不存在 |
| 429 | 限流 | 请求过于频繁 |
| 500 | 服务错误 | 内部服务异常 |
| 503 | 服务不可用 | 服务暂时不可用 |

## 3. WebSocket连接

### 3.1 WebSocket端点

```
wss://api.turms.im/websocket
```

### 3.2 连接认证

WebSocket连接需要在URL参数中提供token：

```
wss://api.turms.im/websocket?token=<your-jwt-token>
```

### 3.3 消息格式

WebSocket使用Protobuf二进制格式进行通信。详细的协议定义请参考turms-server-common模块。

## 4. HTTP API路由

### 4.1 即时通讯API

**基础路径：** `/api/v1/im`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/users/{userId}` | 获取用户信息 | 是 |
| POST | `/users` | 创建用户 | 是 |
| PUT | `/users/{userId}` | 更新用户信息 | 是 |
| DELETE | `/users/{userId}` | 删除用户 | 是 |
| GET | `/groups` | 获取群组列表 | 是 |
| POST | `/groups` | 创建群组 | 是 |
| GET | `/messages` | 获取消息列表 | 是 |
| POST | `/messages` | 发送消息 | 是 |

### 4.2 标签服务API

**基础路径：** `/api/v1/tags`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/categories` | 获取标签分类 | 否 |
| GET | `/categories/{categoryId}/tags` | 获取分类下的标签 | 否 |
| POST | `/user-tags` | 添加用户标签 | 是 |
| GET | `/user-tags/{userId}` | 获取用户标签 | 是 |
| DELETE | `/user-tags/{tagId}` | 删除用户标签 | 是 |
| GET | `/recommendations` | 获取标签推荐 | 是 |
| GET | `/cloud` | 获取标签云 | 否 |

### 4.3 社交关系API

**基础路径：** `/api/v1/social`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/friends` | 获取好友列表 | 是 |
| POST | `/friends/{userId}` | 添加好友 | 是 |
| DELETE | `/friends/{userId}` | 删除好友 | 是 |
| GET | `/followers` | 获取粉丝列表 | 是 |
| GET | `/following` | 获取关注列表 | 是 |
| POST | `/follow/{userId}` | 关注用户 | 是 |
| DELETE | `/follow/{userId}` | 取消关注 | 是 |

### 4.4 内容服务API

**基础路径：** `/api/v1/content`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/posts` | 获取动态列表 | 否 |
| POST | `/posts` | 发布动态 | 是 |
| GET | `/posts/{postId}` | 获取动态详情 | 否 |
| PUT | `/posts/{postId}` | 更新动态 | 是 |
| DELETE | `/posts/{postId}` | 删除动态 | 是 |
| POST | `/posts/{postId}/like` | 点赞动态 | 是 |
| DELETE | `/posts/{postId}/like` | 取消点赞 | 是 |

### 4.5 互动服务API

**基础路径：** `/api/v1/interaction`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/comments/{postId}` | 获取评论列表 | 否 |
| POST | `/comments` | 发表评论 | 是 |
| DELETE | `/comments/{commentId}` | 删除评论 | 是 |
| POST | `/likes` | 点赞操作 | 是 |
| DELETE | `/likes/{likeId}` | 取消点赞 | 是 |
| POST | `/shares` | 分享操作 | 是 |
| GET | `/interactions/{userId}` | 获取用户互动记录 | 是 |

### 4.6 推荐服务API

**基础路径：** `/api/v1/recommendation`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/users` | 获取用户推荐 | 是 |
| GET | `/content` | 获取内容推荐 | 是 |
| GET | `/groups` | 获取群组推荐 | 是 |
| POST | `/feedback` | 提交推荐反馈 | 是 |
| GET | `/settings` | 获取推荐设置 | 是 |
| PUT | `/settings` | 更新推荐设置 | 是 |

### 4.7 管理API

**基础路径：** `/admin`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/dashboard` | 获取控制台数据 | 是 |
| GET | `/users` | 用户管理 | 是 |
| GET | `/statistics` | 统计数据 | 是 |
| GET | `/system/config` | 系统配置 | 是 |
| PUT | `/system/config` | 更新系统配置 | 是 |

## 5. 请求示例

### 5.1 获取用户信息

```http
GET /api/v1/im/users/12345
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**响应：**
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "id": "12345",
    "username": "john_doe",
    "nickname": "John",
    "avatar": "https://cdn.turms.im/avatars/12345.jpg",
    "status": "ONLINE",
    "lastActiveTime": "2025-01-19T10:30:00.000Z"
  }
}
```

### 5.2 发送消息

```http
POST /api/v1/im/messages
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
Content-Type: application/json

{
  "recipientId": "67890",
  "type": "TEXT",
  "content": "Hello, how are you?",
  "attachments": []
}
```

**响应：**
```json
{
  "code": 200,
  "message": "消息发送成功",
  "data": {
    "messageId": "msg_123456789",
    "senderId": "12345",
    "recipientId": "67890",
    "content": "Hello, how are you?",
    "timestamp": "2025-01-19T10:30:00.000Z",
    "status": "SENT"
  }
}
```

### 5.3 添加用户标签

```http
POST /api/v1/tags/user-tags
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
Content-Type: application/json

{
  "tagIds": ["tag_001", "tag_002"],
  "customTags": ["编程爱好者", "音乐发烧友"]
}
```

**响应：**
```json
{
  "code": 200,
  "message": "标签添加成功",
  "data": {
    "userId": "12345",
    "tags": [
      {
        "id": "tag_001",
        "name": "技术",
        "category": "兴趣爱好"
      },
      {
        "id": "tag_002",
        "name": "摄影",
        "category": "兴趣爱好"
      }
    ],
    "customTags": ["编程爱好者", "音乐发烧友"]
  }
}
```

### 5.4 获取内容推荐

```http
GET /api/v1/recommendation/content?page=1&size=20&type=INTEREST
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**响应：**
```json
{
  "code": 200,
  "message": "推荐获取成功",
  "data": {
    "page": 1,
    "size": 20,
    "total": 150,
    "items": [
      {
        "id": "post_001",
        "title": "Spring Boot最佳实践",
        "author": "tech_guru",
        "summary": "分享Spring Boot开发经验...",
        "tags": ["Java", "Spring Boot", "后端开发"],
        "score": 0.95,
        "reason": "基于您的技术兴趣推荐"
      }
    ]
  }
}
```

## 6. 错误处理

### 6.1 认证错误

```json
{
  "code": 401,
  "message": "认证失败，请提供有效的访问令牌",
  "error": "UNAUTHORIZED",
  "timestamp": "2025-01-19T10:30:00.000Z"
}
```

### 6.2 权限错误

```json
{
  "code": 403,
  "message": "权限不足，无法访问该资源",
  "error": "FORBIDDEN",
  "timestamp": "2025-01-19T10:30:00.000Z"
}
```

### 6.3 限流错误

```json
{
  "code": 429,
  "message": "请求过于频繁，请稍后重试",
  "error": "TOO_MANY_REQUESTS",
  "retryAfter": 60,
  "timestamp": "2025-01-19T10:30:00.000Z"
}
```

### 6.4 服务不可用

```json
{
  "code": 503,
  "message": "标签服务暂时不可用，请稍后重试",
  "error": "SERVICE_UNAVAILABLE",
  "service": "turms-tag-service",
  "suggestion": "请检查网络连接或联系技术支持",
  "timestamp": "2025-01-19T10:30:00.000Z"
}
```

## 7. 限流说明

### 7.1 限流规则

| API类型 | 限制 | 窗口期 | 说明 |
|---------|------|--------|------|
| 认证API | 5次/分钟 | 60秒 | 防止暴力破解 |
| IM核心API | 100次/分钟 | 60秒 | 即时通讯API |
| 标签API | 50次/分钟 | 60秒 | 标签相关操作 |
| 社交API | 30次/分钟 | 60秒 | 社交关系操作 |
| 内容API | 20次/分钟 | 60秒 | 内容发布和查看 |
| 互动API | 40次/分钟 | 60秒 | 点赞评论等操作 |
| 推荐API | 15次/分钟 | 60秒 | 推荐算法调用 |

### 7.2 限流响应头

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1705659060
```

## 8. 分页参数

### 8.1 通用分页参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | integer | 1 | 页码，从1开始 |
| size | integer | 20 | 每页大小，最大100 |
| sort | string | - | 排序字段，格式：field,direction |

### 8.2 分页响应格式

```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "page": 1,
    "size": 20,
    "total": 150,
    "totalPages": 8,
    "items": []
  }
}
```

## 9. 监控端点

### 9.1 健康检查

```http
GET /actuator/health
```

**响应：**
```json
{
  "status": "UP",
  "components": {
    "redis": {
      "status": "UP"
    },
    "consul": {
      "status": "UP"
    }
  }
}
```

### 9.2 网关路由信息

```http
GET /actuator/gateway/routes
```

### 9.3 指标数据

```http
GET /actuator/prometheus
```

## 10. WebSocket消息类型

### 10.1 连接管理

```protobuf
// 创建会话请求
CreateSessionRequest {
  int64 user_id = 1;
  DeviceType device_type = 2;
  string device_details = 3;
}

// 会话响应
CreateSessionResponse {
  string session_id = 1;
  int64 server_timestamp = 2;
}
```

### 10.2 消息收发

```protobuf
// 发送消息请求
CreateMessageRequest {
  int64 group_id = 1;
  int64 recipient_id = 2;
  MessageType message_type = 3;
  string text = 4;
  repeated bytes records = 5;
}

// 消息通知
MessageNotification {
  int64 message_id = 1;
  int64 sender_id = 2;
  string text = 3;
  int64 creation_date = 4;
}
```

## 11. SDK示例

### 11.1 JavaScript SDK

```javascript
// 创建网关客户端
const gateway = new TurmsGateway({
  baseUrl: 'https://api.turms.im',
  websocketUrl: 'wss://api.turms.im/websocket'
});

// 登录认证
const token = await gateway.auth.login('username', 'password');

// 发送HTTP请求
const users = await gateway.api.get('/api/v1/im/users');

// WebSocket连接
await gateway.websocket.connect(token);
gateway.websocket.on('message', (message) => {
  console.log('收到消息:', message);
});
```

### 11.2 Python SDK

```python
# 创建网关客户端
from turms_gateway import TurmsGateway

gateway = TurmsGateway(
    base_url='https://api.turms.im',
    websocket_url='wss://api.turms.im/websocket'
)

# 登录认证
token = await gateway.auth.login('username', 'password')

# 发送HTTP请求
users = await gateway.api.get('/api/v1/im/users')

# WebSocket连接
await gateway.websocket.connect(token)
gateway.websocket.on_message(lambda msg: print(f'收到消息: {msg}'))
```

## 12. 测试工具

### 12.1 Postman集合

导入Postman集合文件进行API测试：
```json
{
  "info": {
    "name": "Turms API Gateway",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "https://api.turms.im"
    },
    {
      "key": "token",
      "value": ""
    }
  ]
}
```

### 12.2 cURL示例

```bash
# 登录获取Token
curl -X POST https://api.turms.im/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"password"}'

# 获取用户信息
curl -X GET https://api.turms.im/api/v1/im/users/12345 \
  -H "Authorization: Bearer YOUR_TOKEN"

# 发送消息
curl -X POST https://api.turms.im/api/v1/im/messages \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"recipientId":"67890","type":"TEXT","content":"Hello"}'
```

---

**更新记录**

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| v1.0.0 | 2025-01-19 | 初始版本 |

**联系支持**

如需技术支持，请联系：
- 邮箱：support@turms.im
- 文档：https://docs.turms.im
- GitHub：https://github.com/turms-im/turms