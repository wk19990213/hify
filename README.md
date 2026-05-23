# Hify

一个简版 AI Agent 开发平台（参考 Dify），可本地部署，面向团队内部小规模使用（20~50 人同时在线）。提供模型提供商管理、Agent 配置、对话引擎、RAG 知识库、工作流编排和 MCP 工具接入。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2 + Java 17 + MyBatis-Plus |
| 前端 | Vue 3 + TypeScript + Element Plus + Vite |
| 数据库 | MySQL 8.0 + Redis 7 |
| 向量检索 | pgvector（预留） |
| 熔断限流 | Resilience4j |
| 容器化 | Docker + Docker Compose |

---

## 功能特性

### 已支持

- **模型提供商管理**：支持 OpenAI、Anthropic、Gemini、Ollama 及 OpenAI 兼容协议（如 DeepSeek、硅基流动）
- **Agent 创建与配置**：绑定模型、设置 System Prompt、Temperature、对话轮次上限
- **MCP 工具接入**：Agent 可绑定外部 MCP 服务（独立 Spring Boot 应用，HTTP JSON-RPC 通信），LLM 自主决定调用哪个工具
- **对话引擎**：流式响应（SSE）、多轮对话、上下文管理
- **简版工作流编排**：顺序执行、条件分支
- **管理控制台**：Web 前端界面，统一管理 Provider、Agent、工作流

### 暂不支持（ roadmap ）

- 可视化工作流拖拽编排
- 多租户 / 权限体系
- 插件市场、计费系统
- RAG 知识库完整链路（文档上传、分块、向量化、检索）

---

## 项目结构

```
hify/
├── hify-app/              # 启动模块（Spring Boot Application）
├── hify-common/           # 公共模块（工具类、常量、异常、DTO 基类）
├── hify-provider/         # 模型提供商管理
├── hify-agent/            # Agent 管理与配置
├── hify-chat/             # 对话引擎
├── hify-mcp/              # MCP 工具管理与调用
├── hify-workflow/         # 工作流编排与执行
├── hify-knowledge/        # 知识库与 RAG（骨架实现）
├── hify-web/              # Vue 3 前端
├── hify-mcp-services/     # 独立 MCP 服务示例（如 order-service）
├── deploy/                # Docker + K8s 部署配置
├── docker-compose.yml     # 本地依赖启动（MySQL + Redis）
├── start.sh               # 一键启动脚本（构建 + 启后端 + 启前端）
└── pom.xml                # Maven 父 POM
```

### 模块依赖关系

```
hify-chat → hify-agent, hify-provider, hify-workflow, hify-knowledge
hify-agent → hify-mcp
所有业务模块 → hify-common
hify-app → 所有业务模块（启动入口）
```

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.9+（已含 wrapper：`./mvnw`）
- Node.js 18+ + npm
- MySQL 8.0
- Redis 7

### 1. 启动基础依赖

```bash
# 使用 Docker Compose 启动 MySQL 和 Redis
docker-compose up -d
```

或自行安装并配置 MySQL（端口 3306）和 Redis（端口 6379）。

### 2. 初始化数据库

创建数据库 `hify`，执行项目提供的初始化 SQL（如有）。

### 3. 启动后端

```bash
# 方式一：使用 Maven Wrapper（推荐）
cd hify-app && ../mvnw spring-boot:run

# 方式二：先打包再启动
cd hify-app && ../mvnw clean package -DskipTests
cd target && java -jar hify-app-*.jar

# 方式三：一键脚本（Windows Git Bash）
./start.sh
```

后端默认端口：`8080`
健康检查：`http://localhost:8080/api/v1/health`

### 4. 启动前端

```bash
cd hify-web
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

### 5. 启动 MCP 服务（可选）

如需使用 MCP 工具，需独立启动 MCP 服务：

```bash
cd hify-mcp-services/order-service
java -jar target/order-mcp-service-1.0.0.jar
```

MCP 服务端口：`8090`

---

## 核心概念

### Provider（提供商）

LLM 服务方配置，包含 API 地址、密钥、超时参数等。支持多种类型：OPENAI、ANTHROPIC、OLLAMA、OPENAI_COMPATIBLE。

### ModelConfig（模型配置）

抽象模型定义，不绑定具体提供商。Agent 绑定时选择 ModelConfig，系统通过**上下文解析链**自动路由到可用的 Provider：

```
Agent → ModelConfig → ProviderModel → Provider → ProviderAdapter → LLM API
```

### Agent

用户创建的 AI 配置单元。绑定一个 ModelConfig，设置 System Prompt、Temperature、对话轮次上限，可关联多个 MCP 工具。

### MCP 工具

通过 MCP 协议接入的外部工具。MCP 服务为**独立 Spring Boot 应用**，与主应用完全分离，通过 HTTP JSON-RPC 通信。Agent 绑定整个 MCP 服务（非逐个工具），LLM 自主决定调用哪个工具。

---

## 接口规范

RESTful 风格：`/api/v1/{资源复数名}`

```
GET    /api/v1/providers              # 列表（分页）
POST   /api/v1/providers              # 创建
GET    /api/v1/providers/{id}         # 详情
PUT    /api/v1/providers/{id}         # 更新
DELETE /api/v1/providers/{id}         # 删除
POST   /api/v1/providers/{id}/test-connection  # 连通性测试
```

---

## 部署

### 本地开发

```bash
# 启动依赖
docker-compose up -d

# 启动后端 + 前端
./start.sh
```

### Docker 生产部署（预留）

见 `deploy/` 目录，含 Dockerfile 和 K8s 配置模板。

---

## 开发规范

- 后端遵循阿里巴巴 Java 编码规范（详见 `.claude/CLAUDE.md`）
- Controller 只做参数校验和调用 Service
- 跨模块调用走 Service 接口，不直接引用其他模块的 Mapper 或 Entity
- 统一使用 SLF4J 日志，禁止 `System.out/err`
- 自定义异常继承 `RuntimeException`

---

## License

MIT
