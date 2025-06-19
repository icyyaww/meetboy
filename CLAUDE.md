# CLAUDE.md
每次回答都必须用中文回答
在每次执行任务的时候都要说为什么执行这次任务
每次修改代码后都需要把修改的代码记录到文件中

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Turms is the most advanced open-source instant messaging engine designed for 100K~10M concurrent users. It is a professional-grade, high-performance, reactive instant messaging system built on modern architecture and technology stack.

### System Architecture

**Core Server Components:**
- `turms-api-gateway`: Spring Cloud Gateway-based API gateway providing unified entry point, service routing, authentication, rate limiting, and circuit breaker
- `turms-gateway`: WebSocket/TCP gateway for client connections, handles authentication, session management, and load balancing
- `turms-service`: Core business logic service implementing all IM features and providing admin APIs with RBAC
- `turms-tag-service`: User tagging system with categories, recommendations, tag clouds, and content discovery
- `turms-server-common`: Shared libraries and utilities used by both gateway and service modules
- `turms-admin`: Vue 3 web-based administration interface for business data and cluster management

**Client SDKs:**
- `turms-client-js`: JavaScript/TypeScript client with WebSocket support and tab connection sharing
- `turms-client-dart`: Dart client for Flutter applications
- `turms-client-swift`: Swift client for iOS/macOS applications  
- `turms-client-kotlin`: Kotlin client for JVM/Android applications
- `turms-client-cpp`: C++ client for native applications

**Plugin System:**
- `turms-plugin-demo`: Reference plugin implementation demonstrating extension capabilities
- `turms-plugin-antispam`: Anti-spam protection using Aho-Corasick automaton with double array trie
- `turms-plugin-minio`: MinIO storage integration for file services
- `turms-plugin-push`: Push notification support
- `turms-plugin-rasa`: Chatbot integration with Rasa NLP platform
- `turms-plugin-livekit`: LiveKit integration for real-time communication

**Additional Components:**
- `turms-ai-serving`: AI services integration layer
- `turms-chat-demo-flutter`: Complete Flutter demo application showcasing Turms capabilities
- `turms-docs`: VitePress-based documentation system with internationalization support

### Technology Stack

**Backend (Java 21):**
- Spring Boot 3.4.4 with reactive programming model
- Project Reactor for non-blocking I/O operations
- Netty for high-performance network communication
- MongoDB sharded clusters for data persistence
- Redis for caching and session management
- Protobuf for efficient binary serialization

**Frontend (Vue 3):**
- Vue 3 composition API with TypeScript
- Ant Design Vue component library
- Vite build system for development and production
- Express.js server for admin interface backend

**DevOps & Infrastructure:**
- Docker containerization with multi-stage builds
- Terraform modules for cloud deployment (Alibaba Cloud)
- Prometheus + Grafana for monitoring and observability
- GitHub Actions for CI/CD pipelines

## Build and Development Commands

### Server Components (Maven)

**Core Building:**
```bash
# Build all server modules
mvn clean compile

# Build specific modules
mvn clean compile -pl turms-api-gateway
mvn clean compile -pl turms-gateway
mvn clean compile -pl turms-service
mvn clean compile -pl turms-tag-service
mvn clean compile -pl turms-server-common

# Create executable JARs with all dependencies
mvn clean package -Partifact-fat-jar
```

**Testing Strategy:**
```bash
# Run unit tests only
mvn clean test

# Run integration tests (requires Docker services)
mvn clean verify

# Run tests for specific module
mvn clean verify -pl turms-service

# Skip integration tests
mvn clean verify -DskipITs=true

# Skip unit tests  
mvn clean verify -DskipUTs=true
```

**Test Infrastructure Setup:**
```bash
# Start required services for integration tests
docker compose -f turms-server-test-common/src/main/resources/docker-compose.test.yml up -d

# Run full test suite
mvn clean verify

# Cleanup test infrastructure
docker compose -f turms-server-test-common/src/main/resources/docker-compose.test.yml down
```

**Code Quality and Standards:**
```bash
# Apply code formatting (Spotless)
mvn spotless:apply

# Check code formatting compliance
mvn spotless:check

# Run static analysis (Checkstyle)
mvn clean compile -Pcheckstyle

# Run bug detection analysis (SpotBugs)
mvn spotbugs:check

# Check for security vulnerabilities
mvn org.owasp:dependency-check-maven:check
```

### Client SDK Development

**JavaScript Client:**
```bash
cd turms-client-js

# Full build with protobuf generation
npm run fullbuild

# Quick build without protobuf regeneration
npm run quickbuild

# Run comprehensive test suite
npm test

# Development server with hot reload
npm run dev
```

**Dart Client:**
```bash
cd turms-client-dart

# Install dependencies
dart pub get

# Generate protobuf files
./tool/generate_proto.sh

# Run tests
dart test

# Analyze code quality
dart analyze
```

**Swift Client:**
```bash
cd turms-client-swift

# Resolve package dependencies
swift package resolve

# Generate protobuf files
./generate_proto.sh

# Build project
swift build

# Run test suite
swift test
```

**Kotlin Client:**
```bash
cd turms-client-kotlin

# Compile project
mvn compile

# Generate protobuf files
./generate_proto.sh

# Run tests
mvn test

# Package JAR
mvn package
```

### turms-admin (Web Interface)

```bash
cd turms-admin

# Development setup
npm install
npm run serve       # Start dev server on http://localhost:6510

# Production build
npm run build

# Production deployment with PM2
npm run quickstart  # Install dependencies, build, and start

# Testing
npm test           # Run Cypress e2e tests
npm run cypress    # Open Cypress test runner

# Code quality
npm run lint       # Run ESLint and Stylelint
npm run eslint     # JavaScript/TypeScript linting
npm run stylelint  # CSS/SCSS linting
```

### Documentation System

```bash
cd turms-docs

# Development server
npm run dev        # Start VitePress dev server

# Build documentation
npm run build      # Generate static documentation

# Upgrade dependencies
npm run upgrade    # Update all npm packages
```

### Docker and Deployment

**Development Environment:**
```bash
# Quick start with all services
docker compose -f docker-compose.standalone.yml up --force-recreate

# Development environment with demo data
ENV=dev,demo docker compose -f docker-compose.standalone.yml --profile monitoring up --force-recreate -d

# Install Grafana Loki driver for log aggregation
docker plugin install grafana/loki-docker-driver:latest --alias loki --grant-all-permissions
```

**Cloud Deployment (Terraform):**
```bash
# Alibaba Cloud deployment (automatic infrastructure provisioning)
cd terraform/alicloud/playground
export ALICLOUD_ACCESS_KEY=<your_access_key>
export ALICLOUD_SECRET_KEY=<your_secret_key>
terraform init
terraform apply
```

## Development Environment Setup

**Prerequisites:**
- Java 21 (project extensively uses modern Java features)
- Node.js 18+ (for turms-admin and JavaScript client)
- Docker & Docker Compose (for integration testing and local development)
- Maven 3.6+ (for Java project builds)

**Quick Development Setup:**
```bash
# 1. Clone repository
git clone --depth 1 https://github.com/turms-im/turms.git
cd turms

# 2. Start infrastructure services
docker compose -f docker-compose.standalone.yml up -d

# 3. Build core server components
mvn clean compile -pl turms-service,turms-gateway

# 4. Start admin interface for system management
cd turms-admin && npm run serve
```

## Code Quality Standards

The project enforces strict code quality standards across all components:

**Java Code Standards:**
- **Line length**: 100 characters maximum
- **Indentation**: 4 spaces (no tabs allowed)
- **Import organization**: java|javax|jakarta, then im.turms packages, then static imports
- **License headers**: Apache License 2.0 (automatically applied by Spotless)
- **Formatting**: Eclipse formatter configuration in `codequality/eclipse.xml`
- **Testing**: AssertJ assertions required instead of JUnit assertions
- **Documentation**: Comprehensive Javadoc for public APIs

**Frontend Code Standards:**
- **Vue 3**: Composition API with TypeScript
- **ESLint**: Strict TypeScript and Vue linting rules
- **Stylelint**: CSS/SCSS formatting and best practices
- **Component structure**: Single File Components with clear separation of concerns

**General Standards:**
- **Reactive Programming**: Non-blocking I/O using Project Reactor patterns
- **Error Handling**: Comprehensive error handling with proper exception types
- **Security**: Input validation, XSS protection, and secure authentication
- **Performance**: Optimized for high concurrency and low latency

## Architecture Patterns and Principles

**Core Design Principles:**
- **Reactive Architecture**: Non-blocking I/O throughout the system using Project Reactor and Netty
- **Microservice Architecture**: Separate gateway and service components for scalability
- **Plugin Extensibility**: Comprehensive plugin system for custom functionality
- **Multi-tenancy**: Support for multiple isolated IM applications
- **High Performance**: Optimized for extreme performance and scalability (100K-10M users)

**Communication Patterns:**
- Client ↔ Gateway: WebSocket/TCP with Protobuf serialization
- Gateway ↔ Service: Custom high-performance RPC protocol
- Admin ↔ Service: HTTP REST API with JSON
- Data Storage: MongoDB (sharded clusters) + Redis for caching

**State Management:**
- Stateless servers enabling horizontal scaling
- Session state managed in Redis for persistence
- Business data in MongoDB with comprehensive sharding support
- Event-driven architecture for loose coupling

## Testing Strategy

**Test Categories:**
- **Unit Tests** (`*Tests.java`): Fast, isolated tests using MockK/Mockito
- **Integration Tests** (`*IT.java`): Full component integration with real databases
- **Stress Tests** (`*ST.java`): Performance and load testing scenarios
- **E2E Tests**: Cypress tests for turms-admin web interface

**Test Infrastructure:**
- TestContainers for integration test dependencies (MongoDB, Redis)
- Playwright for browser automation in specific tests
- JMH for micro-benchmarking performance-critical code
- Comprehensive test data factories and builders

## Common Development Workflows

**Adding New Features:**
1. Implement core business logic in turms-service
2. Add gateway endpoints if client-facing functionality required
3. Update relevant client SDKs with new APIs
4. Add admin interface support for management features
5. Write comprehensive tests at all levels (unit, integration, e2e)
6. Update documentation and API specifications

**Plugin Development:**
1. Extend base plugin classes from turms-plugin framework
2. Implement required lifecycle hooks and extension points
3. Configure plugin metadata in `plugin.yaml`
4. Test plugin with turms-plugin-demo as reference implementation
5. Package as shaded JAR for distribution

**Performance Optimization:**
1. Use JMH benchmarks to establish performance baselines
2. Profile using async-profiler or JProfiler for bottleneck identification
3. Implement optimizations with A/B testing approach
4. Verify improvements through stress tests and monitoring
5. Monitor production metrics for real-world validation

## Security and Authentication

**Authentication System:**
- Multi-level authentication (admin, user, guest)
- JWT token-based authentication for admin interface
- Session management with Redis backing store
- Rate limiting and brute force protection

**Security Measures:**
- Input validation at all API boundaries
- XSS and CSRF protection in web interfaces
- Comprehensive audit logging for security events
- Regular security scanning with OWASP dependency check

## Observability and Monitoring

**Monitoring Stack:**
- Prometheus metrics collection
- Grafana dashboards for visualization
- Custom business and technical metrics
- Jaeger distributed tracing support

**Logging Strategy:**
- Structured logging with JSON format
- Three log categories: monitoring, business, and statistics
- Log aggregation with Grafana Loki
- Comprehensive error tracking and alerting

## Documentation and API Management

**Documentation System:**
- VitePress-based documentation with markdown
- Comprehensive API documentation with OpenAPI/Swagger
- Multi-language support (English and Chinese)
- Interactive examples and tutorials

**API Versioning:**
- Semantic versioning across all components
- Backward compatibility guarantees
- Deprecation policies for breaking changes
- Client SDK synchronization tools

## Deployment and Operations

**Container Strategy:**
- Multi-stage Docker builds for optimization
- Health checks and readiness probes
- Resource limits and monitoring
- Blue-green deployment support

**Cloud Integration:**
- Terraform modules for infrastructure as code
- Support for major cloud providers
- Auto-scaling capabilities
- Disaster recovery procedures

**Configuration Management:**
- Environment-specific configuration files
- Kubernetes ConfigMaps and Secrets support
- Dynamic configuration updates without restarts
- Configuration validation and testing

## Performance Characteristics

**System Capabilities:**
- Support for 100K to 10M concurrent users
- Sub-millisecond message delivery latency
- Horizontal scaling across multiple data centers
- 99.9%+ uptime with proper deployment

**Optimization Features:**
- Connection pooling and multiplexing
- Efficient memory management with direct allocation
- CPU cache-friendly thread models
- Optimized serialization with Protobuf

## Development Best Practices

**Code Review Process:**
- Mandatory code review for all changes
- Automated quality gates with CI/CD
- Performance impact assessment for critical paths
- Security review for authentication/authorization changes

**Version Control:**
- Feature branch workflow
- Conventional commit messages
- Semantic versioning for releases
- Comprehensive release notes

**Continuous Integration:**
- Automated testing on multiple environments
- Code quality checks with quality gates
- Dependency vulnerability scanning
- Performance regression testing

This project represents a production-ready, enterprise-grade instant messaging system with comprehensive tooling, documentation, and development practices suitable for large-scale deployments.

## API Gateway System (turms-api-gateway)

**架构设计:**
- 基于Spring Cloud Gateway的响应式网关
- 统一API入口管理和服务路由
- 支持WebSocket代理和HTTP API路由
- 集成服务发现、负载均衡、熔断降级

**核心功能:**
- JWT认证和权限控制
- 基于Redis的分布式限流
- Resilience4j熔断器保护
- CORS跨域支持
- 请求日志和监控指标
- 自动服务发现和健康检查

**路由配置:**
```
/websocket/** → turms-gateway:10510 (WebSocket)
/api/v1/im/** → turms-service (即时通讯API)
/api/v1/tags/** → turms-tag-service (标签服务)
/api/v1/social/** → turms-social-service (社交关系)
/api/v1/content/** → turms-content-service (内容管理)
/api/v1/interaction/** → turms-interaction-service (互动功能)
/api/v1/recommendation/** → turms-recommendation-service (推荐算法)
/admin/** → turms-admin (管理界面)
```

**部署命令:**
```bash
# 本地开发启动
cd turms-api-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 生产环境构建
mvn clean package
java -jar target/turms-api-gateway-*.jar --spring.profiles.active=prod

# Docker部署
docker compose -f turms-api-gateway/docker/docker-compose.yml up -d
```

**监控端点:**
- 健康检查: GET /actuator/health
- 网关路由: GET /actuator/gateway/routes  
- 指标监控: GET /actuator/prometheus
- 服务状态: GET /actuator/gateway/refresh

**配置要点:**
- JWT密钥至少32字符长度
- Redis连接用于限流和缓存
- Consul用于服务发现(生产环境)
- 限流参数根据业务需求调整
- 熔断阈值配置要考虑服务特性

**安全配置:**
- 生产环境CORS限制具体域名
- JWT过期时间生产环境建议1小时
- 限流策略针对不同API设置
- 监控告警集成Prometheus+Grafana
