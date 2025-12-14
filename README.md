# 文件资料管理系统

## 项目简介

基于Vue3 + SpringBoot的文件资料管理系统，支持文件上传、资料管理、借阅管理等功能。

## 技术栈

### 前端
- Vue 3.4
- Vite 5.0
- TypeScript 5.3
- Naive UI 2.38
- Pinia 2.1
- Axios 1.6
- Vue Router 4.2

### 后端
- Spring Boot 3.2
- MyBatis-Plus 3.5
- MySQL 8.0
- Sa-Token 1.37
- Redis（可选）
- WebSocket
- Knife4j（API文档）
- Flowable 7.0.1（工作流引擎）

## 项目结构

```
DocumentManagementSystem/
├── dms-web/             # Web应用启动模块
└── frontend/            # 前端项目
```

## 环境要求

- **Java**: JDK 17 或更高版本（Spring Boot 3.x 要求）
- **Maven**: 3.6+ 
- **MySQL**: 8.0+
- **Redis**: 可选，用于缓存和 Sa-Token 会话存储
- **Node.js**: 16+ （前端开发）

> ⚠️ **重要提示**：如果系统使用的是 Java 8，需要升级到 Java 17。详细安装配置请参考 [JAVA_SETUP.md](JAVA_SETUP.md)

## 快速开始

### 后端启动

1. **确保 Java 17 已安装并配置**
   ```bash
   java -version  # 应显示 17 或更高版本
   ```

2. 创建MySQL数据库
```sql
CREATE DATABASE dms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 修改配置文件 `dms-web/src/main/resources/application.yml`
   - 修改数据库连接信息
   - 修改Redis连接信息（如使用）
   - 修改文件上传路径

3. 执行数据库初始化脚本（需要手动创建表结构）

4. 启动项目
```bash
cd dms-web
mvn spring-boot:run
```

### 前端启动

1. 安装依赖
```bash
cd frontend
npm install
```

2. 启动开发服务器
```bash
npm run dev
```

3. 访问 http://localhost:3000

## 核心功能

1. **用户管理**：用户CRUD、状态管理
2. **权限管理**：基于Sa-Token的权限控制
3. **角色管理**：管理员、普通用户等角色
4. **资料管理**：支持图片、文字、文件上传
5. **大厅展示**：公开资料浏览
6. **借阅管理**：申请、审批、状态流转
7. **首页统计**：数据统计仪表盘
8. **工作流管理**：基于Flowable的审批流程配置和管理

## API文档

启动后端服务后，访问：http://localhost:8080/api/doc.html

## 文档

- [Flowable 集成配置文档](docs/Flowable集成配置文档.md) - Flowable 工作流引擎的技术集成配置说明
- [Flowable 系统使用文档](docs/Flowable系统使用文档.md) - Flowable 工作流功能的用户使用指南

## 数据库表结构

需要创建以下表：
- sys_user（用户表）
- sys_role（角色表）
- sys_permission（权限表）
- file_document（资料表）
- file_borrow（借阅表）
- process_definition（流程定义表）
- process_node（流程节点配置表）

**Flowable 系统表**：Flowable 会自动创建（前缀 `act_`），无需手动创建。

详细表结构请参考 `database/flowable_tables.sql`。

## 注意事项

1. 文件上传路径需要在配置文件中设置
2. 如需使用OSS存储，需要配置OSS相关参数
3. Redis为可选，如不使用需要修改Sa-Token配置
4. 首次运行需要创建数据库表结构
5. Flowable 表会自动创建，但业务表（process_definition、process_node）需要手动创建
6. 使用 Flowable 功能前，请先阅读 [Flowable 集成配置文档](docs/Flowable集成配置文档.md)

