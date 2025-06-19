#!/bin/bash

echo "=== Turms API Gateway 项目结构验证 ==="
echo

echo "1. 检查Maven配置文件..."
if [ -f "pom.xml" ]; then
    echo "✓ pom.xml 存在"
else
    echo "✗ pom.xml 不存在"
    exit 1
fi

echo "2. 检查主应用类..."
if [ -f "src/main/java/im/turms/apigateway/TurmsApiGatewayApplication.java" ]; then
    echo "✓ 主应用类存在"
else
    echo "✗ 主应用类不存在"
    exit 1
fi

echo "3. 检查配置文件..."
if [ -f "src/main/resources/application.yml" ]; then
    echo "✓ 配置文件存在"
else
    echo "✗ 配置文件不存在"
    exit 1
fi

echo "4. 检查核心组件..."
components=(
    "src/main/java/im/turms/apigateway/config/GatewayConfig.java"
    "src/main/java/im/turms/apigateway/security/JwtUtil.java"
    "src/main/java/im/turms/apigateway/filter/AuthenticationGatewayFilterFactory.java"
    "src/main/java/im/turms/apigateway/filter/LoggingGlobalFilter.java"
    "src/main/java/im/turms/apigateway/exception/FallbackController.java"
    "src/main/java/im/turms/apigateway/util/ResponseUtil.java"
)

for component in "${components[@]}"; do
    if [ -f "$component" ]; then
        echo "✓ $(basename $component) 存在"
    else
        echo "✗ $(basename $component) 不存在"
    fi
done

echo "5. 检查Docker配置..."
if [ -f "docker/Dockerfile" ]; then
    echo "✓ Dockerfile 存在"
else
    echo "✗ Dockerfile 不存在"
fi

if [ -f "docker/docker-compose.yml" ]; then
    echo "✓ docker-compose.yml 存在"
else
    echo "✗ docker-compose.yml 不存在"
fi

echo "6. 检查文档..."
if [ -f "README.md" ]; then
    echo "✓ README.md 存在"
else
    echo "✗ README.md 不存在"
fi

echo
echo "=== 项目结构检查完成 ==="

# 检查target目录
if [ -d "target/classes" ]; then
    echo "✓ target/classes 目录存在"
    if [ -f "target/classes/application.yml" ]; then
        echo "✓ 配置文件已复制到target目录"
    else
        echo "✗ 配置文件未复制到target目录"
    fi
else
    echo "✗ target/classes 目录不存在"
fi

echo
echo "构建建议："
echo "1. 确保有良好的网络连接"
echo "2. 运行: mvn clean compile -pl turms-api-gateway"
echo "3. 如果网络问题，可以稍后重试依赖下载"
echo "4. 验证父项目pom.xml中包含了turms-api-gateway模块"