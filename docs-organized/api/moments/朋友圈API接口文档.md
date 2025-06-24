# 朋友圈API接口文档 v1.0

## 文档说明

本文档定义了朋友圈功能的完整API接口规范，建议在 **turms-content-service** 中实现。

## 基础信息

**服务名称**: turms-content-service  
**基础URL**: `http://localhost:8520`  
**API版本**: v1  
**认证方式**: JWT Token  

## 数据模型

### 朋友圈动态 (Moment)

```json
{
  "id": "moment_123456789",
  "userId": 1001,
  "userInfo": {
    "userId": 1001,
    "username": "user1001",
    "nickname": "张三",
    "avatar": "https://cdn.example.com/avatar/1001.jpg"
  },
  "content": "今天天气真好！#心情愉快",
  "mediaUrls": [
    "https://cdn.example.com/images/photo1.jpg",
    "https://cdn.example.com/images/photo2.jpg"
  ],
  "locationInfo": {
    "name": "北京市朝阳区",
    "latitude": 39.9042,
    "longitude": 116.4074
  },
  "visibility": "FRIENDS",
  "visibleToUsers": [1002, 1003],
  "invisibleToUsers": [1004],
  "statistics": {
    "likeCount": 15,
    "commentCount": 8,
    "shareCount": 2
  },
  "status": "PUBLISHED",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

### 可见性枚举
- `PUBLIC`: 公开可见
- `FRIENDS`: 仅好友可见
- `PRIVATE`: 仅自己可见
- `CUSTOM`: 自定义可见范围

## API接口

### 1. 朋友圈动态管理

#### 1.1 发布朋友圈

**接口地址**: `POST /api/v1/moments`

**请求参数**:
```json
{
  "content": "今天天气真好！#心情愉快",
  "mediaUrls": [
    "https://cdn.example.com/images/photo1.jpg",
    "https://cdn.example.com/images/photo2.jpg"
  ],
  "locationInfo": {
    "name": "北京市朝阳区",
    "latitude": 39.9042,
    "longitude": 116.4074
  },
  "visibility": "FRIENDS",
  "visibleToUsers": [1002, 1003],
  "invisibleToUsers": [1004]
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "momentId": "moment_123456789",
    "message": "朋友圈发布成功"
  }
}
```

**错误响应**:
```json
{
  "success": false,
  "error": {
    "code": "CONTENT_TOO_LONG",
    "message": "内容长度超过限制",
    "details": "内容不能超过1000个字符"
  }
}
```

#### 1.2 获取朋友圈时间线

**接口地址**: `GET /api/v1/moments/timeline`

**查询参数**:
- `page`: 页码 (默认0)
- `size`: 每页数量 (默认20, 最大50)
- `lastMomentId`: 上次最后一条动态ID (用于下拉刷新)

**请求示例**:
```
GET /api/v1/moments/timeline?page=0&size=20
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "moments": [
      {
        "id": "moment_123456789",
        "userId": 1001,
        "userInfo": {...},
        "content": "今天天气真好！",
        "mediaUrls": [...],
        "statistics": {...},
        "isLiked": false,
        "createdAt": "2024-01-15T10:30:00Z"
      }
    ],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 156,
      "totalPages": 8,
      "hasNext": true
    }
  }
}
```

#### 1.3 获取个人朋友圈

**接口地址**: `GET /api/v1/moments/users/{userId}`

**路径参数**:
- `userId`: 用户ID

**查询参数**:
- `page`: 页码 (默认0)
- `size`: 每页数量 (默认20)
- `viewerUserId`: 查看者用户ID (用于权限验证)

**响应示例**:
```json
{
  "success": true,
  "data": {
    "userInfo": {
      "userId": 1001,
      "username": "user1001",
      "nickname": "张三",
      "avatar": "...",
      "isFriend": true,
      "canViewMoments": true
    },
    "moments": [...],
    "pagination": {...}
  }
}
```

#### 1.4 获取朋友圈详情

**接口地址**: `GET /api/v1/moments/{momentId}`

**路径参数**:
- `momentId`: 朋友圈ID

**查询参数**:
- `viewerUserId`: 查看者用户ID

**响应示例**:
```json
{
  "success": true,
  "data": {
    "moment": {...},
    "permissions": {
      "canLike": true,
      "canComment": true,
      "canShare": false,
      "canEdit": false,
      "canDelete": false
    }
  }
}
```

#### 1.5 编辑朋友圈

**接口地址**: `PUT /api/v1/moments/{momentId}`

**权限要求**: 仅本人可编辑

**请求参数**:
```json
{
  "content": "修改后的内容",
  "visibility": "PRIVATE",
  "visibleToUsers": [],
  "invisibleToUsers": []
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "message": "朋友圈更新成功"
  }
}
```

#### 1.6 删除朋友圈

**接口地址**: `DELETE /api/v1/moments/{momentId}`

**权限要求**: 仅本人可删除

**响应示例**:
```json
{
  "success": true,
  "data": {
    "message": "朋友圈删除成功"
  }
}
```

### 2. 朋友圈互动 (集成interaction-service)

#### 2.1 点赞朋友圈

**接口地址**: `POST /api/v1/moments/{momentId}/like`

**说明**: 该接口会调用 turms-interaction-service 的点赞功能

**请求参数**:
```json
{
  "userId": 1001,
  "deviceType": "mobile",
  "ipAddress": "192.168.1.100"
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "isLiked": true,
    "likeCount": 16,
    "message": "点赞成功"
  }
}
```

#### 2.2 评论朋友圈

**接口地址**: `POST /api/v1/moments/{momentId}/comments`

**说明**: 该接口会调用 turms-interaction-service 的评论功能

**请求参数**:
```json
{
  "userId": 1001,
  "content": "哈哈哈，确实是好天气！",
  "deviceType": "mobile",
  "ipAddress": "192.168.1.100"
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "commentId": 789,
    "message": "评论成功"
  }
}
```

#### 2.3 获取朋友圈点赞列表

**接口地址**: `GET /api/v1/moments/{momentId}/likes`

**查询参数**:
- `page`: 页码 (默认0)
- `size`: 每页数量 (默认20)

**响应示例**:
```json
{
  "success": true,
  "data": {
    "likes": [
      {
        "userId": 1002,
        "username": "user1002",
        "nickname": "李四",
        "avatar": "...",
        "isFriend": true,
        "likedAt": "2024-01-15T11:00:00Z"
      }
    ],
    "pagination": {...}
  }
}
```

#### 2.4 获取朋友圈评论列表

**接口地址**: `GET /api/v1/moments/{momentId}/comments`

**查询参数**:
- `page`: 页码 (默认0)
- `size`: 每页数量 (默认20)
- `sortBy`: 排序方式 (`latest`, `hot`)

**响应示例**:
```json
{
  "success": true,
  "data": {
    "comments": [
      {
        "commentId": 789,
        "userId": 1002,
        "userInfo": {...},
        "content": "哈哈哈，确实是好天气！",
        "likeCount": 3,
        "isLiked": false,
        "createdAt": "2024-01-15T11:05:00Z"
      }
    ],
    "pagination": {...}
  }
}
```

### 3. 朋友圈权限管理

#### 3.1 检查查看权限

**接口地址**: `GET /api/v1/moments/{momentId}/permissions/view`

**查询参数**:
- `viewerUserId`: 查看者用户ID

**响应示例**:
```json
{
  "success": true,
  "data": {
    "canView": true,
    "reason": "USER_IS_FRIEND"
  }
}
```

#### 3.2 检查互动权限

**接口地址**: `GET /api/v1/moments/{momentId}/permissions/interaction`

**查询参数**:
- `userId`: 用户ID

**响应示例**:
```json
{
  "success": true,
  "data": {
    "canLike": true,
    "canComment": true,
    "canShare": false,
    "restrictions": []
  }
}
```

### 4. 朋友圈设置

#### 4.1 获取朋友圈设置

**接口地址**: `GET /api/v1/moments/settings`

**响应示例**:
```json
{
  "success": true,
  "data": {
    "defaultVisibility": "FRIENDS",
    "allowComments": true,
    "allowShares": true,
    "notifyOnLike": true,
    "notifyOnComment": true,
    "blockedUsers": [1005, 1006]
  }
}
```

#### 4.2 更新朋友圈设置

**接口地址**: `PUT /api/v1/moments/settings`

**请求参数**:
```json
{
  "defaultVisibility": "FRIENDS",
  "allowComments": true,
  "allowShares": false,
  "notifyOnLike": true,
  "notifyOnComment": true
}
```

### 5. 朋友圈统计

#### 5.1 获取用户朋友圈统计

**接口地址**: `GET /api/v1/moments/users/{userId}/stats`

**响应示例**:
```json
{
  "success": true,
  "data": {
    "totalMoments": 156,
    "totalLikes": 1234,
    "totalComments": 567,
    "thisMonthMoments": 12,
    "averageLikesPerMoment": 8.5
  }
}
```

## 错误码说明

| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| `MOMENT_NOT_FOUND` | 404 | 朋友圈不存在 |
| `ACCESS_DENIED` | 403 | 没有访问权限 |
| `CONTENT_TOO_LONG` | 400 | 内容超长 |
| `INVALID_MEDIA_URL` | 400 | 无效的媒体URL |
| `RATE_LIMIT_EXCEEDED` | 429 | 请求频率超限 |
| `USER_NOT_FOUND` | 404 | 用户不存在 |
| `FRIENDSHIP_REQUIRED` | 403 | 需要好友关系 |
| `MOMENT_DELETED` | 410 | 朋友圈已删除 |

## 服务集成说明

### 与 turms-interaction-service 集成

朋友圈的点赞和评论功能需要调用 interaction-service 的接口：

```yaml
# content-service 配置
turms:
  content-service:
    integration:
      interaction-service:
        base-url: http://localhost:8530
        timeout: 5000ms
```

### 调用示例

```java
// 朋友圈点赞
@PostMapping("/{momentId}/like")
public Mono<ResponseEntity<?>> likeMoment(@PathVariable String momentId, @RequestBody LikeRequest request) {
    return momentService.validateMomentAccess(momentId, request.getUserId())
            .flatMap(hasAccess -> {
                if (!hasAccess) {
                    return Mono.just(ResponseEntity.status(403).body("无权限"));
                }
                
                // 调用 interaction-service
                return interactionServiceClient.toggleLike(
                    request.getUserId(), "MOMENT", momentId, 
                    request.getDeviceType(), request.getIpAddress())
                    .map(result -> ResponseEntity.ok(result));
            });
}
```

### 与 turms-service 集成

好友关系验证需要调用 turms-service：

```java
// 验证好友关系
public Mono<Boolean> canViewMoment(String momentId, Long viewerUserId) {
    return momentRepository.findById(momentId)
            .flatMap(moment -> {
                if (moment.getVisibility() == Visibility.PUBLIC) {
                    return Mono.just(true);
                }
                if (moment.getVisibility() == Visibility.FRIENDS) {
                    return turmsServiceClient.areUsersFriends(
                        moment.getUserId(), viewerUserId);
                }
                return Mono.just(false);
            });
}
```

## 前端调用示例

### JavaScript/TypeScript

```typescript
// 朋友圈API客户端
class MomentsApi {
    private baseUrl = 'http://localhost:8520/api/v1/moments';
    
    // 发布朋友圈
    async publishMoment(data: PublishMomentRequest): Promise<MomentResponse> {
        const response = await fetch(`${this.baseUrl}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${this.getToken()}`
            },
            body: JSON.stringify(data)
        });
        return response.json();
    }
    
    // 获取时间线
    async getTimeline(page = 0, size = 20): Promise<TimelineResponse> {
        const response = await fetch(
            `${this.baseUrl}/timeline?page=${page}&size=${size}`,
            {
                headers: {
                    'Authorization': `Bearer ${this.getToken()}`
                }
            }
        );
        return response.json();
    }
    
    // 点赞朋友圈
    async likeMoment(momentId: string, userId: number): Promise<LikeResponse> {
        const response = await fetch(`${this.baseUrl}/${momentId}/like`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${this.getToken()}`
            },
            body: JSON.stringify({ userId })
        });
        return response.json();
    }
}
```

## 数据库设计

参考 MOMENTS_USER_INTEGRATION.md 中的数据表设计。

## 部署说明

1. **服务依赖**: 需要先启动 turms-service 和 turms-interaction-service
2. **数据库**: 需要 MySQL 用于朋友圈数据存储
3. **缓存**: 需要 Redis 用于权限和内容缓存
4. **文件存储**: 需要配置图片/视频存储服务

## 测试用例

### Postman 测试集合

```json
{
  "info": {
    "name": "朋友圈API测试",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "发布朋友圈",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"content\": \"测试朋友圈发布\",\n  \"visibility\": \"FRIENDS\"\n}"
        },
        "url": {
          "raw": "{{baseUrl}}/api/v1/moments",
          "host": ["{{baseUrl}}"],
          "path": ["api", "v1", "moments"]
        }
      }
    }
  ],
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8520"
    }
  ]
}
```

## 更新日志

- **v1.0** (2024-06-20): 初始版本，定义基础API接口
- 后续版本将根据实际开发需求进行更新

---

**注意**: 这是API设计文档，实际实现需要在 turms-content-service 中开发对应的功能代码。