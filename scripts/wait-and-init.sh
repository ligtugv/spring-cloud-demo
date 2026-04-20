#!/bin/bash
set -e

echo "========================================="
echo "微服务 DEMO - 启动脚本"
echo "========================================="

# Wait for MySQL
echo "等待 MySQL 启动..."
until mysql -h localhost -u root -proot_password -e "SELECT 1" > /dev/null 2>&1; do
    echo "MySQL not ready, waiting..."
    sleep 2
done
echo "MySQL ready!"

# Wait for OpenLDAP
echo "等待 OpenLDAP 启动..."
until ldapwhoami -H ldap://localhost -x -D "cn=admin,dc=luban-cae,dc=com" -w admin_secret > /dev/null 2>&1; do
    echo "OpenLDAP not ready, waiting..."
    sleep 2
done
echo "OpenLDAP ready!"

# Init LDAP
echo "初始化 LDAP 数据..."
chmod +x /scripts/init-ldap.sh
/scripts/init-ldap.sh

echo "========================================="
echo "所有服务启动完成!"
echo "========================================="
echo "访问地址:"
echo "  - 网关 (Gateway):  http://localhost:7573"
echo "  - Web 登录页面:    http://localhost:7573/web/"
echo "  - Eureka 控制台:   http://localhost:7573/eureka/web"
echo "  - LDAP Admin:      http://localhost:8090"
echo "========================================="
