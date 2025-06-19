# Turms API Gateway Dockerfile
FROM openjdk:21-jre-slim

LABEL maintainer="Turms Project"
LABEL description="Turms API Gateway - Unified entry point for all Turms services"

# 设置工作目录
WORKDIR /app

# 安装必要的工具
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 复制应用文件
COPY target/turms-api-gateway-*.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 环境变量
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:+UseStringDeduplication -XX:MaxGCPauseMillis=100"
ENV SPRING_PROFILES_ACTIVE=prod

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=$SPRING_PROFILES_ACTIVE"]