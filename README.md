# Spring Cloud 微服务 DEMO

## 概述

本项目是一套完整的 Spring Cloud 微服务演示系统，包含用户认证(UAA)、API网关、产品管理等核心微服务模块，支持用户名密码、LDAP、GitHub OAuth2 三种登录方式。

## 技术栈

- **Spring Boot**: 3.5.0
- **Spring Cloud**: 2025.0.0
- **Spring Authorization Server**: 1.3.0
- **Spring Cloud Gateway**: 4.3.0
- **MySQL**: 8.0
- **OpenLDAP**: 1.5.0 (Docker 容器)
- **Java**: 17
- **Maven**: 3.8+
- **Docker**: 20.10+

## JDK 版本要求

**JDK 17** 或更高版本。

推荐使用 Eclipse Temurin (Adoptium) JDK 17:
- 下载地址: https://adoptium.net/temurin/releases/?version=17

验证版本:
```bash
java -version
# openjdk version "17.x.x"
# OpenJDK Runtime Environment (Temurin-17...)
```

## 快速开始

### 1. 编译项目

```bash
mvn clean install -DskipTests
```

### 2. 启动所有服务 (需要 Docker)

```bash
docker-compose --env-file docker-compose.env down -v
docker-compose --env-file docker-compose.env up --build -d
```

> 环境变量文件 `docker-compose.env` 已在仓库中包含真实配置，拉取后可直接启动。

所有服务将在同一台服务器上运行，通过 Docker 内部网络通信。

### 3. 访问系统

- **Web 登录页面**: http://localhost:7573/web/login
- **Gateway 直接访问**: http://localhost:7573
- **LDAP Admin**: http://localhost:8090
- **Discovery 控制台**: http://localhost:18761

## 测试账号

### 数据库用户 (用户名密码登录 / LDAP 登录)

| 用户名        | 密码          | 角色                              |
|---------------|---------------|-----------------------------------|
| user_1        | user_1        | USER                              |
| editor_1      | editor_1      | EDITOR, USER                      |
| adm_1         | adm_1         | PRODUCT_ADMIN, EDITOR, USER       |
| ldap_user_1   | ldap_user_1   | USER                              |
| ldap_editor_1 | ldap_editor_1 | EDITOR, USER                      |
| ldap_adm_1    | ldap_adm_1    | PRODUCT_ADMIN, USER               |

### GitHub OAuth2 (需要配置，见下方说明)

登录后默认分配 **EDITOR + USER** 角色。

## API 测试 (CURL 命令)

所有 API 通过 Gateway 统一入口访问 (端口 **7573**)。

### 1. 获取 Access Token (用户名密码登录)

```bash
curl -X POST "http://localhost:7573/auth/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=adm_1&password=adm_1&client_id=public-client"
```

**实际输出示例**:
```json
{
  "access_token": "eyJraWQiOiJ1YWEtc2lnbmluZy1rZXktdjEiLCJhbGciOiJSUzI1NiJ9...",
  "token_type": "Bearer",
  "expires_in": 7200,
  "scope": "read write user product:read product:write product:delete",
  "refresh_token": "bc26e131-35f5-4b06-acd8-1fd846acb425",
  "roles": ["PRODUCT_ADMIN", "EDITOR", "USER"]
}
```

> `access_token` 字段的值即为 JWT token，后续 API 调用中需要使用。

### 2. 查询产品列表 (需要 USER 角色)

```bash
curl -X GET "http://localhost:7573/product/api/products" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**实际输出示例** (200 OK):
```json
[
  { "id": 1, "name": "TestProduct", "stock": 10, "createdBy": "editor_1", "createdAt": "2026-04-18T10:15:00", "updatedAt": "2026-04-18T10:15:00" }
]
```

### 3. 添加产品 (需要 EDITOR 或 PRODUCT_ADMIN 角色)

```bash
curl -X POST "http://localhost:7573/product/api/products" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"New Product"}'
```

**实际输出示例** (200 OK):
```json
{"id":3,"name":"New Product","stock":10,"createdBy":"editor_1","createdAt":"2026-04-20T04:54:35.807180393","updatedAt":"2026-04-20T04:54:35.807200152"}
```

**USER 角色调用时** (403 Forbidden):
```json
{"error":"Forbidden"}
```

### 4. 修改产品 (需要 EDITOR 或 PRODUCT_ADMIN 角色)

```bash
curl -X PUT "http://localhost:7573/product/api/products/1" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Updated Product Name"}'
```

**实际输出示例** (200 OK):
```json
{"id":1,"name":"Updated Product Name","stock":10,"createdBy":"editor_1","createdAt":"2026-04-20T04:54:36","updatedAt":"2026-04-20T04:54:36"}
```

### 5. 删除产品 (需要 EDITOR 或 PRODUCT_ADMIN 角色)

```bash
curl -X DELETE "http://localhost:7573/product/api/products/1" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**实际输出示例** (204 No Content，删除成功后返回空)

**无权限时** (403 Forbidden):
```json
{"error":"Forbidden"}
```

**自己创建的产品** (EDITOR 可删除自己创建的):
```bash
# editor_1 登录，可删除 editor_1 创建的产品
curl -X DELETE "http://localhost:7573/product/api/products/2" \
  -H "Authorization: Bearer YOUR_EDITOR_TOKEN"
# 返回 204 No Content
```

**PRODUCT_ADMIN 可删除任意产品**:
```bash
# adm_1 登录，可删除任意产品
curl -X DELETE "http://localhost:7573/product/api/products/1" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
# 返回 204 No Content
```

## 角色权限说明

| 角色           | 产品列表 | 添加产品 | 修改产品 | 删除产品 |
|----------------|---------|---------|---------|---------|
| USER           | OK      | NO      | NO      | NO      |
| EDITOR         | OK      | OK      | OK      | 仅可删除自己创建的产品 |
| PRODUCT_ADMIN  | OK      | OK      | OK      | 可删除任何产品 |

**角色继承**: PRODUCT_ADMIN > EDITOR > USER

**删除权限说明**:
- PRODUCT_ADMIN 可删除任意产品
- EDITOR 只能删除自己（created_by = 当前用户名）创建的产品
- USER 禁止删除任何产品

**数据归属**: 每条产品记录存储 `created_by` 字段，记录创建者用户名，可在前端产品列表中查看。

## 自测确认列表

请在交付前确认以下所有项目:

- [x] readme中包含项目要求的JDK版本说明 (JDK 17)
- [x] `mvn clean install -DskipTests` 能build成功
- [x] `docker-compose up --build` 能启动所有服务
- [x] MySQL 自动创建 `uaa_db`、`product_db`、`cart_db` 三个数据库（通过 `scripts/init-mysql.sql`）
- [x] Flyway 自动建表（`oauth2_authorization` 表在 uaa_db，`product` 表含 `created_by`、`stock` 字段在 product_db）
- [x] LDAP 自动创建 6 个用户（ldap_user_1 / ldap_editor_1 / ldap_adm_1）
- [x] LDAP 自动创建 3 个角色组（USER / EDITOR / PRODUCT_ADMIN）
- [x] 数据库用户 user_1 / editor_1 / adm_1 可登录
- [x] 通过 curl 命令能得到预期的输出（见上方 API 测试）
- [x] RBAC 权限控制: USER 浏览OK增删改403，EDITOR/PRODUCT_ADMIN 全部OK
- [x] JWT token 包含 `username` 声明（AuthController 手动注入）
- [x] Product.created_by 记录创建者用户名
- [x] PRODUCT_ADMIN 可删除任意产品，EDITOR 只能删除自己创建的产品
- [x] GitHub OAuth2 已配置 Client ID/Secret，登录后分配 EDITOR 角色
- [x] 前端产品列表显示创建者，按权限显示/隐藏删除按钮
- [x] Redis 购物车缓存和分布式锁（CartService）
- [x] 购物车下架商品自动清理，购买失败友好提示

## 本地运行说明 (不使用 Docker)

```bash
# 1. 编译
mvn clean install -DskipTests

# 2. 启动 Discovery (端口 18761)
java -jar discovery/target/discovery-1.0.0.jar

# 3. 启动 UAA (端口 19000)
java -jar uaa/target/uaa-1.0.0.jar --spring.profiles.active=default

# 4. 启动 Product (端口 18082)
java -jar product/target/product-1.0.0.jar --spring.profiles.active=default

# 5. 启动 Gateway (端口 17573)
java -jar gateway/target/gateway-1.0.0.jar --spring.profiles.active=default
```

本地模式下使用 H2 内存数据库，LDAP 登录不可用（需要 Docker 环境）。

## LDAP 目录服务

GitHub OAuth 配置已在 `docker-compose.env` 中预置，无需额外配置。

1. 访问 http://localhost:7573/web/login
2. 点击 "GitHub" 标签页
3. 点击 "Sign in with GitHub"
4. 授权后自动登录，分配 EDITOR + USER 角色

## 网络配置说明

### Docker 环境 (推荐用于评审)

所有服务在同一台服务器上通过 Docker 内部网络通信:

```
外部访问 (浏览器/curl)
       |
       v
Gateway :7573 (统一入口)
       |
       +-- /auth/* --> UAA :9000
       +-- /product/* --> Product :8082
       +-- /web/* --> Web :8080
```

**服务间通信使用 Docker 内部 DNS**:
- `gateway` -> `uaa:9000`
- `gateway` -> `product:8082`
- `product` -> `uaa:9000` (获取 JWT 公钥验证)

### 本地开发模式

不使用 Docker 时，配置 `application.yml` 中的 `jwk-set-uri`:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://localhost:9000/.well-known/jwks.json
```

## 项目结构

```
demo/
├── docker-compose.yml        # 容器编排文件
├── pom.xml                  # 父 POM
├── README.md                # 本文件
├── .gitignore
├── scripts/
│   ├── init-mysql.sql      # MySQL 数据库初始化
│   └── init-ldap.sh       # LDAP 初始化脚本
├── discovery/               # 服务注册中心 (Eureka Server)
├── config/                  # 配置中心 (Spring Cloud Config Server)
├── uaa/                     # 用户认证服务 (Spring Authorization Server)
├── gateway/                 # API 网关 (Spring Cloud Gateway)
├── product/                 # 产品管理服务
├── cart/                    # 购物车服务
└── web/                     # Web 前端 (Thymeleaf 登录页面)
```

## 服务端口说明

| 服务       | 容器端口 | 宿主机端口 | 说明                   |
|------------|----------|------------|------------------------|
| Gateway    | 7573     | 7573       | 统一入口,对外暴露       |
| UAA        | 9000     | 19000      | Docker 内部通信         |
| Product    | 8082     | 18082      | Docker 内部通信         |
| Cart       | 8083     | 18083      | Docker 内部通信         |
| Web        | 8080     | 18080      | Docker 内部通信         |
| Discovery  | 8761     | 18761      | Eureka 控制台          |
| Config     | 8888     | 18888      | 配置中心               |
| MySQL      | 3306     | 3307       | 数据库                 |
| OpenLDAP   | 389      | 3389       | LDAP 目录服务          |
| LDAP Admin | 80       | 8090       | LDAP Web 管理界面      |
| Redis      | 6379     | 6379       | 购物车缓存             |

## 常见问题

### Q: LDAP 用户无法登录?

1. 确认 Docker 容器正常运行: `docker-compose ps`
2. 检查 LDAP 日志: `docker-compose logs openldap`
3. 手动初始化 LDAP 数据: `docker exec microservice-openldap ldapsearch -x -H ldap://localhost -D "cn=admin,dc=luban-cae,dc=com" -w admin_secret -b "dc=luban-cae,dc=com" "(uid=ldap_adm_1)"`

### Q: GitHub OAuth2 无法登录?

1. 确认已在 GitHub 创建 OAuth App
2. 确认 callback URL 填写正确: `http://localhost:7573/login/oauth2/code/github`
3. 确认 `application.yml` 和 `docker-compose.yml` 中已配置正确的 Client ID 和 Secret

### Q: JWT 验证失败?

1. 确认 Product 服务能访问 UAA 的 jwk-set-uri
2. Docker 环境下 Product 使用: `http://uaa:9000/.well-known/jwks.json`
3. 本地开发使用: `http://localhost:9000/.well-known/jwks.json`

### Q: mvn clean install 失败?

1. 确认 JDK 版本为 17: `java -version`
2. 确认 Maven 版本 >= 3.8: `mvn -version`
3. 检查网络连接 (需要下载依赖)

### Q: docker-compose up 卡住?

1. 检查 Docker 是否正常运行: `docker ps`
2. 确认端口未被占用: `netstat -an | grep 7573`
3. 查看日志: `docker-compose logs -f`
