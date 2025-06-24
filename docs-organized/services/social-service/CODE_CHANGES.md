# turms-social-service 代码修改记录

## 项目概述
重新设计了 turms-social-service 服务，从好友和群组管理服务转变为专门的社交推荐和关系图分析服务。

## 主要修改内容

### 1. 项目配置文件

#### 父模块 POM 配置 (`/home/icyyaww/program/meetboy/pom.xml`)
```xml
<!-- 添加了新的子模块 -->
<module>turms-social-service</module>
```

#### 服务 POM 配置 (`/home/icyyaww/program/meetboy/turms-social-service/pom.xml`)
```xml
<artifactId>turms-social-service</artifactId>
<description>Turms社交关系服务 - 提供好友关系、群组管理、社交推荐、互动功能等服务</description>

<!-- 核心依赖 -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
        <exclusions>
            <exclusion>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-logging</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-core</artifactId>
    </dependency>
</dependencies>

<!-- 构建配置 -->
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <mainClass>im.turms.social.SocialServiceApplication</mainClass>
        <finalName>turms-social-service</finalName>
    </configuration>
</plugin>
```

### 2. 主启动类

#### SocialServiceApplication.java
```java
package im.turms.social;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

@SpringBootApplication(exclude = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class
})
public class SocialServiceApplication {
    public static void main(String[] args) {
        System.out.println("正在启动Turms社交关系服务...");
        SpringApplication.run(SocialServiceApplication.class, args);
        System.out.println("Turms社交关系服务启动完成！");
    }
}
```

### 3. 控制器层

#### FriendRecommendationController.java（重新设计为推荐算法接口）
```java
package im.turms.social.controller;

import im.turms.social.service.RecommendationEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/social/recommendations")
public class FriendRecommendationController {

    @Autowired
    private RecommendationEngineService recommendationEngineService;

    /**
     * 获取好友推荐列表（支持多种算法）
     */
    @GetMapping("/friends/{userId}")
    public Flux<Map<String, Object>> getFriendRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "hybrid") String algorithm,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0.5") double threshold) {
        
        return switch (algorithm) {
            case "collaborative" -> recommendationEngineService.generateCollaborativeFilteringRecommendations(userId, limit);
            case "content-based" -> recommendationEngineService.generateContentBasedRecommendations(userId, limit);
            case "hybrid" -> recommendationEngineService.generateHybridRecommendations(userId, limit, threshold);
            default -> recommendationEngineService.generateHybridRecommendations(userId, limit, threshold);
        };
    }

    /**
     * 协同过滤推荐
     */
    @GetMapping("/collaborative/{userId}")
    public Flux<Map<String, Object>> getCollaborativeRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        return recommendationEngineService.generateCollaborativeFilteringRecommendations(userId, limit);
    }

    /**
     * 基于内容的推荐
     */
    @GetMapping("/content-based/{userId}")
    public Flux<Map<String, Object>> getContentBasedRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "15") int limit) {
        return recommendationEngineService.generateContentBasedRecommendations(userId, limit);
    }

    /**
     * 解释推荐原因
     */
    @GetMapping("/explain/{userId}/{recommendedUserId}")
    public Mono<Map<String, Object>> explainRecommendation(
            @PathVariable Long userId,
            @PathVariable Long recommendedUserId) {
        return recommendationEngineService.explainRecommendation(userId, recommendedUserId);
    }

    /**
     * 提交推荐反馈
     */
    @PostMapping("/feedback")
    public Mono<Map<String, Object>> submitRecommendationFeedback(
            @RequestBody Map<String, Object> feedback) {
        Long userId = Long.valueOf(feedback.get("userId").toString());
        Long recommendedUserId = Long.valueOf(feedback.get("recommendedUserId").toString());
        String action = feedback.get("action").toString();
        
        return recommendationEngineService.recordRecommendationFeedback(userId, recommendedUserId, action);
    }

    /**
     * 获取推荐算法性能指标
     */
    @GetMapping("/metrics")
    public Mono<Map<String, Object>> getRecommendationMetrics() {
        return recommendationEngineService.getRecommendationMetrics();
    }
}
```

#### SocialGraphController.java（重新设计为关系图分析接口）
```java
package im.turms.social.controller;

import im.turms.social.service.SocialGraphAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/social/graph")
public class SocialGraphController {

    @Autowired
    private SocialGraphAnalysisService socialGraphAnalysisService;

    /**
     * 获取用户社交网络图
     */
    @GetMapping("/{userId}/network")
    public Mono<Map<String, Object>> getUserSocialNetwork(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "2") int depth,
            @RequestParam(defaultValue = "50") int maxNodes) {
        return socialGraphAnalysisService.buildUserSocialNetwork(userId, depth, maxNodes);
    }

    /**
     * 分析用户影响力
     */
    @GetMapping("/{userId}/influence")
    public Mono<Map<String, Object>> getUserInfluence(@PathVariable Long userId) {
        return socialGraphAnalysisService.calculateUserInfluence(userId);
    }

    /**
     * 发现用户间社交路径
     */
    @GetMapping("/path/{userId1}/{userId2}")
    public Flux<Map<String, Object>> findSocialPath(
            @PathVariable Long userId1,
            @PathVariable Long userId2,
            @RequestParam(defaultValue = "6") int maxDegrees) {
        return socialGraphAnalysisService.findSocialPaths(userId1, userId2, maxDegrees);
    }

    /**
     * 社区发现
     */
    @GetMapping("/{userId}/communities")
    public Flux<Map<String, Object>> discoverCommunities(@PathVariable Long userId) {
        return socialGraphAnalysisService.discoverCommunities(userId);
    }

    /**
     * 分析社交趋势
     */
    @GetMapping("/{userId}/trends")
    public Mono<Map<String, Object>> analyzeSocialTrends(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "30") int days) {
        return socialGraphAnalysisService.analyzeSocialTrends(userId, days);
    }

    /**
     * 获取社交网络统计
     */
    @GetMapping("/{userId}/statistics")
    public Mono<Map<String, Object>> getSocialStatistics(@PathVariable Long userId) {
        return socialGraphAnalysisService.getSocialStatistics(userId);
    }
}
```

### 4. 业务服务层

#### RecommendationEngineService.java（推荐引擎核心服务）
实现了三种主要推荐算法：
- **混合推荐算法**：结合多种策略的综合推荐
- **协同过滤推荐**：基于用户行为相似性（UserCF 和 ItemCF）
- **基于内容推荐**：根据用户画像和兴趣标签

主要方法：
```java
// 混合推荐
public Flux<Map<String, Object>> generateHybridRecommendations(Long userId, int limit, double threshold)

// 协同过滤推荐
public Flux<Map<String, Object>> generateCollaborativeFilteringRecommendations(Long userId, int limit)

// 基于内容推荐
public Flux<Map<String, Object>> generateContentBasedRecommendations(Long userId, int limit)

// 推荐解释
public Mono<Map<String, Object>> explainRecommendation(Long userId, Long recommendedUserId)

// 反馈记录
public Mono<Map<String, Object>> recordRecommendationFeedback(Long userId, Long recommendedUserId, String action)
```

#### SocialGraphAnalysisService.java（社交关系图分析服务）
实现了完整的社交网络分析功能：
- **社交网络构建**：多层级关系图构建
- **影响力分析**：基于五种中心性指标的综合计算
- **社交路径发现**：BFS/Dijkstra 算法找寻用户间路径
- **社区发现**：Louvain 算法识别社交群体
- **趋势分析**：时间序列分析和预测

主要方法：
```java
// 构建社交网络
public Mono<Map<String, Object>> buildUserSocialNetwork(Long userId, int depth, int maxNodes)

// 计算影响力
public Mono<Map<String, Object>> calculateUserInfluence(Long userId)

// 发现社交路径
public Flux<Map<String, Object>> findSocialPaths(Long userId1, Long userId2, int maxDegrees)

// 社区发现
public Flux<Map<String, Object>> discoverCommunities(Long userId)

// 趋势分析
public Mono<Map<String, Object>> analyzeSocialTrends(Long userId, int days)
```

### 5. 配置文件

#### application.yml（完整的分环境配置）
```yaml
# 服务基础配置
server:
  port: 8086

spring:
  application:
    name: turms-social-service
  profiles:
    active: dev

# 推荐引擎配置
turms:
  social-service:
    recommendation:
      enabled: true
      algorithms:
        hybrid:
          weight-collaborative: 0.4
          weight-content: 0.4
          weight-trending: 0.2
        collaborative:
          min-similarity: 0.1
          neighbor-size: 50
        content-based:
          tag-weight: 0.6
          profile-weight: 0.4

    # 社交图分析配置
    graph-analysis:
      enabled: true
      max-network-depth: 3
      max-network-nodes: 1000
      influence-algorithms:
        degree-centrality: 0.25
        betweenness-centrality: 0.20
        closeness-centrality: 0.20
        eigenvector-centrality: 0.20
        pagerank: 0.15

    # 性能配置
    performance:
      default-page-size: 20
      max-page-size: 100
      enable-cache: true
      cache-expire-minutes: 30
```

### 6. 文档更新

#### README.md（完全重写的项目文档）
更新了项目说明，包括：
- 服务定位：社交推荐与关系图分析
- 技术架构：Spring Boot 3.4.4 + WebFlux
- API 接口文档：推荐 API 和图分析 API
- 配置参数说明
- 部署和运维指南

## 实现效果

### API 接口测试
```bash
# 推荐 API 测试
curl "http://localhost:8086/social/recommendations/friends/12345?algorithm=hybrid&limit=5"
curl http://localhost:8086/social/recommendations/collaborative/12345
curl http://localhost:8086/social/recommendations/content-based/12345

# 社交图分析 API 测试
curl "http://localhost:8086/social/graph/12345/network?depth=2&maxNodes=20"
curl http://localhost:8086/social/graph/12345/influence
curl http://localhost:8086/social/graph/path/12345/67890
curl http://localhost:8086/social/graph/12345/communities
```

### 服务启动验证
- 服务端口：8086
- 健康检查：/actuator/health
- 支持多环境配置（dev/test/prod）
- 响应式编程模型，支持高并发

## 技术特点

1. **响应式编程**：使用 Project Reactor 实现非阻塞 I/O
2. **算法丰富**：三种推荐算法 + 五种影响力计算方法
3. **高度可配置**：支持算法权重调整和性能参数配置
4. **可扩展性**：模块化设计，易于添加新算法
5. **生产就绪**：完整的监控、日志、多环境支持

## 修改总结

将原本的用户和群组管理服务完全重新设计为专门的社交推荐和关系图分析服务，实现了：
- 智能推荐引擎（3种算法）
- 社交网络分析（6种功能模块）
- 完整的 RESTful API 设计
- 生产级的配置和部署方案

服务现在专注于社交数据的智能分析和推荐，为上层应用提供高质量的社交洞察服务。