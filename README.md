# MiniChat - 轻量级实时通讯系统

MiniChat 是一款基于 Spring Boot 3 + Vue 3 + WebSocket 构建的轻量级实时通讯系统。项目采用前后端分离架构，通过 Nginx 进行统一反向代理，支持私聊、群聊、朋友圈、文件传输、敏感词过滤等核心功能。

## 🚀 项目特性

- **实时通讯**：基于 WebSocket + STOMP 协议实现低延迟消息推送。
- **消息队列**：使用 RabbitMQ 处理消息异步解耦及死信队列，保证消息可靠性。
- **全文检索**：集成 Elasticsearch 实现海量聊天记录的高效搜索。
- **高效缓存**：Redis 负责在线状态管理、Feeds 流推拉模式及系统预热。
- **对象存储**：对接阿里云 OSS，支持图片、视频及大文件的断点续传。
- **内容安全**：内置前缀树（Trie）算法实现的敏感词过滤系统。
- **统一代理**：使用 Nginx 统一管理前端静态资源与后端 API 转发，解决跨域并支持负载均衡。
- **快速部署**：支持 Docker Compose 一键启动所有中间件环境。

## 🛠️ 技术选型

### 后端
- **核心框架**：Spring Boot 3.5
- **持久层**：MyBatis Plus 3.5
- **数据库**：MySQL 8.0
- **缓存**：Redis 7.0
- **消息中间件**：RabbitMQ 3.12
- **搜索引擎**：Elasticsearch 8.11
- **安全认证**：Spring Security + JWT

### 前端
- **框架**：Vue 3.5
- **状态管理**：Pinia
- **UI 组件库**：Element Plus
- **实时协议**：SockJS + StompJS

### 运维部署
- **反向代理**：Nginx
- **容器化**：Docker / Docker Compose

## 📦 快速开始

### 1. 克隆项目
```bash
git clone https://github.com/your-username/minichat-project.git
cd minichat-project
```

### 2. 环境配置
复制环境变量模板并根据实际情况修改（特别是第三方服务 Key）：
```bash
cp .env.example .env
```

### 3. 启动中间件 (Docker)
项目已配置 MySQL 自动初始化。确保本地已安装 Docker，并停止占用端口的服务：
```bash
docker-compose up -d
```
> **注**：`mysql-init/init.sql` 会在容器首次启动时自动导入数据库结构。

### 4. 后端启动
1. 修改 `minichat/src/main/resources/application-local.yml` 中的敏感配置（如 OSS Key）。
2. 运行 `MinichatApplication.java` 启动后端服务。

### 5. 前端部署与 Nginx 配置
1. 编译前端项目：
   ```bash
   cd minichat-frontend
   npm install
   npm run build
   ```
2. 配置 Nginx：
   - 将 `minichat-frontend/dist` 目录配置为 Nginx 静态资源根目录。
   - 使用根目录下的 `nginx.conf` 作为参考，配置 API 转发与 WebSocket 代理。
3. 启动 Nginx 服务。

## 📂 项目结构
```text
minichat-project/
├── minichat/              # 后端 Java 工程
├── minichat-frontend/     # 前端 Vue 工程
├── mysql-init/            # MySQL 数据库初始化脚本
│   └── init.sql
├── es-docker/             # ES 相关配置
├── docker-compose.yml     # Docker 容器编排脚本
├── nginx.conf             # Nginx 反向代理配置参考
└── .env.example           # 环境变量模板
```

