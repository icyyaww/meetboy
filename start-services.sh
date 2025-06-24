#!/bin/bash

# Turms 项目启动脚本
echo "=== 启动 Turms 项目 ==="

# 设置工作目录
BASE_DIR="/home/icyyaww/program/meetboy"
cd "$BASE_DIR"

# 检查Java版本
echo "检查Java环境..."
java -version
if [ $? -ne 0 ]; then
    echo "错误: 未找到Java环境"
    exit 1
fi

# 定义端口配置
TURMS_SERVICE_PORT=8510
TURMS_GATEWAY_PORT=9510
TURMS_INTERACTION_PORT=8530
TURMS_ADMIN_PORT=6510
MYSQL_PORT=3306
MONGO_PORT=27017
REDIS_PORT=6379

echo "=== 步骤1: 检查基础服务状态 ==="

# 检查MySQL
echo "检查MySQL服务..."
if netstat -tln | grep -q ":$MYSQL_PORT "; then
    echo "✓ MySQL正在运行 (端口 $MYSQL_PORT)"
else
    echo "❌ MySQL未运行，尝试启动..."
    # 如果有Docker Compose文件，尝试启动
    if [ -f "docker-compose.mysql.yml" ]; then
        echo "使用Docker启动MySQL..."
        docker-compose -f docker-compose.mysql.yml up -d mysql
    else
        echo "请手动启动MySQL服务"
    fi
fi

# 检查MongoDB
echo "检查MongoDB服务..."
if netstat -tln | grep -q ":$MONGO_PORT "; then
    echo "✓ MongoDB正在运行 (端口 $MONGO_PORT)"
else
    echo "❌ MongoDB未运行"
    echo "建议: 请手动启动MongoDB或使用Docker"
fi

# 检查Redis
echo "检查Redis服务..."
if netstat -tln | grep -q ":$REDIS_PORT "; then
    echo "✓ Redis正在运行 (端口 $REDIS_PORT)"
else
    echo "❌ Redis未运行"
    echo "建议: 请手动启动Redis或使用Docker"
fi

echo "=== 步骤2: 编译项目 ==="

# 检查是否需要编译
if [ ! -d "turms-service/target" ] || [ ! -d "turms-interaction-service/target" ]; then
    echo "需要编译项目..."
    echo "开始编译核心模块..."
    
    # 只编译必要的模块
    mvn clean compile -pl turms-server-common,turms-service,turms-interaction-service -am -DskipTests -q
    
    if [ $? -ne 0 ]; then
        echo "❌ 编译失败，尝试单独编译..."
        
        # 尝试编译server-common
        echo "编译 turms-server-common..."
        cd turms-server-common && mvn clean compile -DskipTests -q && cd ..
        
        # 尝试编译turms-service
        echo "编译 turms-service..."
        cd turms-service && mvn clean compile -DskipTests -q && cd ..
        
        # 尝试编译turms-interaction-service
        echo "编译 turms-interaction-service..."
        cd turms-interaction-service && mvn clean compile -DskipTests -q && cd ..
    fi
else
    echo "✓ 发现已编译的目标文件"
fi

echo "=== 步骤3: 启动服务 ==="

# 函数：启动Java服务
start_java_service() {
    local service_name=$1
    local service_dir=$2
    local main_class=$3
    local port=$4
    local additional_args=$5
    
    echo "启动 $service_name..."
    
    cd "$service_dir"
    
    # 检查端口是否被占用
    if netstat -tln | grep -q ":$port "; then
        echo "⚠️  端口 $port 已被占用，$service_name 可能已在运行"
        cd "$BASE_DIR"
        return 0
    fi
    
    # 设置classpath
    CLASSPATH="target/classes:$(find target/lib -name "*.jar" 2>/dev/null | tr '\n' ':' | sed 's/:$//')"
    
    # 启动服务
    nohup java -cp "$CLASSPATH" $additional_args "$main_class" > "../logs/${service_name,,}.log" 2>&1 &
    
    local pid=$!
    echo "$service_name 已启动，PID: $pid"
    
    # 等待服务启动
    echo "等待 $service_name 启动..."
    sleep 10
    
    # 检查服务是否启动成功
    if netstat -tln | grep -q ":$port "; then
        echo "✓ $service_name 启动成功 (端口 $port)"
    else
        echo "❌ $service_name 启动失败，请检查日志"
        cat "../logs/${service_name,,}.log" | tail -20
    fi
    
    cd "$BASE_DIR"
}

# 创建日志目录
mkdir -p logs

# 启动顺序：turms-service -> turms-interaction-service -> turms-gateway -> turms-admin

# 1. 启动 turms-service (核心服务)
start_java_service "Turms-Service" "turms-service" \
    "im.turms.service.TurmsServiceApplication" \
    "$TURMS_SERVICE_PORT" \
    "-Dspring.profiles.active=dev -Xmx1g"

# 2. 启动 turms-interaction-service (互动服务)
start_java_service "Turms-Interaction-Service" "turms-interaction-service" \
    "im.turms.interaction.InteractionServiceApplication" \
    "$TURMS_INTERACTION_PORT" \
    "-Dspring.profiles.active=dev -Xmx512m"

# 3. 启动 turms-gateway (网关服务)
if [ -d "turms-gateway/target" ]; then
    start_java_service "Turms-Gateway" "turms-gateway" \
        "im.turms.gateway.TurmsGatewayApplication" \
        "$TURMS_GATEWAY_PORT" \
        "-Dspring.profiles.active=dev -Xmx1g"
else
    echo "⚠️  turms-gateway 未编译，跳过启动"
fi

echo "=== 步骤4: 启动 Admin 管理后台 ==="

if [ -d "turms-admin" ]; then
    cd turms-admin
    
    # 检查是否安装了依赖
    if [ ! -d "node_modules" ]; then
        echo "安装Admin依赖..."
        npm install
    fi
    
    # 检查端口
    if netstat -tln | grep -q ":$TURMS_ADMIN_PORT "; then
        echo "⚠️  端口 $TURMS_ADMIN_PORT 已被占用，Admin可能已在运行"
    else
        echo "启动Admin管理后台..."
        # 构建前端
        npm run build
        
        # 启动后端服务器
        nohup npm run start > "../logs/turms-admin.log" 2>&1 &
        
        echo "Admin管理后台已启动"
        echo "访问地址: http://localhost:$TURMS_ADMIN_PORT"
    fi
    
    cd "$BASE_DIR"
else
    echo "⚠️  turms-admin 目录未找到，跳过启动"
fi

echo "=== 步骤5: 服务状态总览 ==="

echo ""
echo "服务启动完成！以下是服务状态："
echo ""
echo "基础服务:"
echo "  MySQL:     $(netstat -tln | grep -q ":$MYSQL_PORT " && echo "✓ 运行中" || echo "❌ 未运行") (端口 $MYSQL_PORT)"
echo "  MongoDB:   $(netstat -tln | grep -q ":$MONGO_PORT " && echo "✓ 运行中" || echo "❌ 未运行") (端口 $MONGO_PORT)"
echo "  Redis:     $(netstat -tln | grep -q ":$REDIS_PORT " && echo "✓ 运行中" || echo "❌ 未运行") (端口 $REDIS_PORT)"
echo ""
echo "Turms服务:"
echo "  Core Service:       $(netstat -tln | grep -q ":$TURMS_SERVICE_PORT " && echo "✓ 运行中" || echo "❌ 未运行") (端口 $TURMS_SERVICE_PORT)"
echo "  Interaction Service: $(netstat -tln | grep -q ":$TURMS_INTERACTION_PORT " && echo "✓ 运行中" || echo "❌ 未运行") (端口 $TURMS_INTERACTION_PORT)"
echo "  Gateway Service:    $(netstat -tln | grep -q ":$TURMS_GATEWAY_PORT " && echo "✓ 运行中" || echo "❌ 未运行") (端口 $TURMS_GATEWAY_PORT)"
echo "  Admin Panel:        $(netstat -tln | grep -q ":$TURMS_ADMIN_PORT " && echo "✓ 运行中" || echo "❌ 未运行") (端口 $TURMS_ADMIN_PORT)"
echo ""

if netstat -tln | grep -q ":$TURMS_ADMIN_PORT "; then
    echo "🎉 项目启动成功！"
    echo ""
    echo "访问链接:"
    echo "  管理后台: http://localhost:$TURMS_ADMIN_PORT"
    echo "  API文档:  http://localhost:$TURMS_SERVICE_PORT/swagger-ui.html"
    echo ""
    echo "日志文件位置:"
    echo "  Core Service: $BASE_DIR/logs/turms-service.log"
    echo "  Interaction:  $BASE_DIR/logs/turms-interaction-service.log"
    echo "  Gateway:      $BASE_DIR/logs/turms-gateway.log"
    echo "  Admin:        $BASE_DIR/logs/turms-admin.log"
else
    echo "⚠️  部分服务启动失败，请检查日志文件"
fi

echo ""
echo "=== 启动脚本执行完成 ==="