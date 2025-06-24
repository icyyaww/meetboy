#!/bin/bash

echo "启动Turms项目 - 包含Nginx反向代理"

# 检查nginx是否已安装
if ! command -v nginx &> /dev/null; then
    echo "正在安装nginx..."
    sudo apt update
    sudo apt install -y nginx
fi

# 停止可能运行的nginx服务
sudo systemctl stop nginx 2>/dev/null || true

# 备份原nginx配置
if [ -f /etc/nginx/nginx.conf ]; then
    sudo cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.backup.$(date +%Y%m%d_%H%M%S)
fi

# 复制我们的nginx配置
sudo cp nginx.conf /etc/nginx/nginx.conf

# 测试nginx配置
sudo nginx -t

if [ $? -eq 0 ]; then
    echo "Nginx配置验证成功"
    
    # 启动所有服务
    echo "启动MySQL..."
    sudo systemctl start mysql || echo "MySQL可能已经在运行"
    
    echo "启动MongoDB..."
    sudo systemctl start mongod || echo "MongoDB可能已经在运行"
    
    echo "启动Redis..."
    sudo systemctl start redis-server || echo "Redis可能已经在运行"
    
    # 启动Turms服务
    echo "启动Turms交互服务 (端口8531)..."
    cd turms-interaction-service
    nohup mvn spring-boot:run -Dspring-boot.run.profiles=dev > ../logs/turms-interaction-service.log 2>&1 &
    cd ..
    
    # 等待交互服务启动
    sleep 10
    
    echo "启动Turms主服务 (端口8510)..."
    cd turms-service  
    nohup mvn spring-boot:run -Dspring-boot.run.profiles=dev > ../logs/turms-service.log 2>&1 &
    cd ..
    
    # 等待主服务启动
    sleep 10
    
    echo "启动Turms网关 (端口9510)..."
    cd turms-gateway
    nohup mvn spring-boot:run -Dspring-boot.run.profiles=dev > ../logs/turms-gateway.log 2>&1 &
    cd ..
    
    echo "启动Turms管理界面 (端口6510)..."
    cd turms-admin
    nohup npm run dev > ../logs/turms-admin.log 2>&1 &
    cd ..
    
    # 等待所有服务启动
    sleep 15
    
    echo "启动Nginx反向代理..."
    sudo nginx
    
    echo ""
    echo "🎉 所有服务启动完成！"
    echo ""
    echo "访问地址："
    echo "- 管理界面: http://localhost (通过Nginx代理)"
    echo "- 直接访问管理界面: http://localhost:6510"
    echo "- API端点: http://localhost:8510"
    echo "- 交互服务: http://localhost:8531"
    echo ""
    echo "测试交互API："
    echo "curl -X GET \"http://localhost/interaction/likes/page?page=0&size=20\""
    echo "curl -X GET \"http://localhost:8510/interaction/likes/page?page=0&size=20\""
    echo ""
    echo "查看日志："
    echo "tail -f logs/turms-interaction-service.log"
    echo "tail -f logs/turms-service.log"
    echo "tail -f logs/turms-admin.log"
    
else
    echo "❌ Nginx配置验证失败，请检查配置文件"
    exit 1
fi