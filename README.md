# Hper - AI智能体平台

<div align="center">

一款基于 Spring AI + Vue 3 的全栈 AI 对话应用，支持多轮对话记忆、智能体（Agent）管理、工具调用、RAG 知识库问答、文件解析与自主规划等核心能力。

---

## 📸 效果预览

### 聊天主页

<!-- 在下方插入聊天页面截图 -->
<p align="center">
  <img src="./screenshots/chat-home.png" alt="聊天主页" width="800" />
</p>

### 智能体管理

<!-- 在下方插入智能体页面截图 -->
<p align="center">
  <img src="./screenshots/agent-page.png" alt="智能体页面" width="800" />
</p>

---

## ✨ 核心功能

| 功能 | 说明 |
|------|------|
| **多轮对话** | 基于 Spring AI ChatMemory 接口的数据库持久化方案，完整保留对话历史，支持上下文窗口滑动管理 |
| **流式输出** | SSE (Server-Sent Events) 实时推送 AI 回复，支持打字机效果与深度思考模式双流程 |
| **智能体 (ChatBot)** | 用户可自定义创建角色型智能体，设定名称和系统提示词，快速构建专属 AI 助手 |
| **RAG 智能体** | 基于 PgVector 向量数据库的知识库检索增强生成（RAG），支持文档上传、分块、向量化与语义搜索 |
| **文件上传 & 解析** | 支持 PDF / TXT / Markdown 文件上传，服务端通过 PDFBox / Jsoup 解析后作为对话上下文 |
| **联网搜索** | 集成百度 AI 搜索工具，一键开启联网能力，获取实时信息辅助回答 |
| **深度思考** | 双阶段推理模式——先输出分析思考过程，再基于思考结果生成最终回答 |
| **用户系统** | 注册 / 登录、个人资料编辑（头像上传）、深色/浅色主题切换 |
| **消息分页加载** | 游标分页（Cursor Pagination），向上滚动时懒加载历史消息 |

---

## 🏗 技术架构

### 整体架构

```
┌─────────────────────────────────────────────────────┐
│                   Browser (Vue 3)                    │
│  ┌──────────┐  ┌──────────┐  ┌────────────────────┐ │
│  │ Pinia    │  │ Vue      │  │ SSE Stream Reader  │ │
│  │ Store    │  │ Router   │  │ Markdown Renderer  │ │
│  └────┬─────┘  └────┬─────┘  └────────┬───────────┘ │
│       └──────────────┼─────────────────┘             │
│                      ▼                               │
│              HTTP API / SSE                          │
└──────────────────────┬───────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│              Spring Boot Backend                     │
│  ┌──────────────────────────────────────────────┐   │
│  │              Controller Layer                │   │
│  │  ChatConversationController                  │   │
│  │  AiController / UserController               │   │
│  └──────────────────────┬───────────────────────┘   │
│                         ▼                           │
│  ┌──────────────────────────────────────────────┐   │
│  │              Service Layer                   │   │
│  │  ChatApp (SSE Streaming)                     │   │
│  │  ChatMessageService / ConversationService    │   │
│  │  RagQueryService / FileParseService          │   │
│  └──────────────────────┬───────────────────────┘   │
│                         ▼                           │
│  ┌──────────────────────────────────────────────┐   │
│  │           Memory & Agent Layer               │   │
│  │  DatabaseChatMemory (Spring AI ChatMemory)   │   │
│  │  BaseAgent / ReActAgent / ToolCallAgent      │   │
│  │  MessageChatMemoryAdvisor                    │   │
│  └──────────────────────┬───────────────────────┘   │
│                         ▼                           │
│  ┌──────────┬──────────┴──────────┬──────────────┐  │
│  │PostgreSQL │   PgVector         │ DashScope    │  │
│  │(会话/消息) │ (向量存储/RAG)     │ (LLM 模型)   │  │
│  └──────────┴─────────────────────┴──────────────┘  │
└─────────────────────────────────────────────────────┘
```

### 技术栈

**前端**

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | ^3.5 | 核心框架 (Composition API) |
| TypeScript | ~6.0 | 类型安全 |
| Vite | ^8.0 | 构建工具 |
| Pinia | ^3.0 | 状态管理 |
| Vue Router | ^5.0 | 路由管理 |
| Ant Design Vue | ^4.2 | UI 组件库 |
| Axios | ^1.9 | HTTP 请求 |
| Marked + Highlight.js | - | Markdown 渲染与代码高亮 |

**后端**

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 运行环境 |
| Spring Boot | 3.5 | 基础框架 |
| Spring AI Alibaba | 1.1.2 | AI 能力集成（DashScope 模型、Agent 框架） |
| MyBatis-Plus | 3.5 | ORM 框架 |
| PostgreSQL | - | 关系数据库（会话、消息、用户） |
| PgVector | - | 向量数据库（RAG 文档嵌入） |
| Knife4j | 4.4 | API 文档 (Swagger 增强) |
| Aliyun OSS | 3.18 | 对象存储（头像、附件） |
| PDFBox | 3.0 | PDF 文件解析 |
| Jsoup | 1.22 | HTML/Markdown 解析 |

---

## 🔑 核心设计

### 对话记忆持久化

实现 Spring AI `ChatMemory` 接口，提供两种存储策略：

- **DatabaseChatMemory**（生产使用）：每条消息持久化至 `chat_message` 表，携带递增序号 `seq_no` 和上下文标记 `context_included`。当消息数超过会话配置的窗口大小时，自动执行滑动窗口策略——将最早的消息标记为移出上下文，而非物理删除，完整保留对话历史。
- **FileBasedChatMemory**（开发调试）：基于 Kryo 序列化将消息保存为本地 `.kryo` 文件。

通过 `MessageChatMemoryAdvisor` 将 ChatMemory 无缝注入 ChatClient 调用链，每次对话自动完成「加载历史 → 调用 LLM → 持久化新消息」的全流程。

### 上下文管理策略

| 策略 | 说明 | 状态 |
|------|------|------|
| Sliding Window | 固定窗口大小，滑动淘汰最早消息 | ✅ 已实现 |
| Summary | 对历史消息做摘要压缩 | 🔧 预留扩展 |
| Hybrid | 滑动窗口 + 摘要混合 | 🔧 预留扩展 |

### Agent 智能体体系

```
BaseAgent (抽象基类)
├── 状态管理: IDLE → RUNNING → FINISHED / ERROR
├── 步骤控制: currentStep / maxStep
├── ReActAgent (ReAct 推理循环)
├── ToolCallAgent (工具调用型)
└── HuanAgent (定制实现)
```

支持两种会话类型：
- **CHATBOT**：用户自定义角色提示词的轻量智能体
- **RAG_AGENT**：关联知识库的 RAG 增强智能体，通过向量相似度检索相关文档作为上下文

---

## 🚀 快速开始

### 环境要求

- JDK 21+
- Node.js 20+ (推荐 >=22.12)
- PostgreSQL 14+ (需安装 pgvector 扩展)
- Maven 3.8+

### 1. 初始化数据库

```sql
-- 执行 SQL 初始化脚本
\i sql/init.sql
\i sql/migration_agent_rag.sql
```

### 2. 配置后端

编辑 `huan-ai-helper-backend/src/main/resources/application.yml` 及对应 profile 配置：

```yaml
# 必须配置项
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/your_db
    username: your_username
    password: your_password

  ai:
    dashscope:
      api-key: your_dashscope_api_key        # 通义千问 API Key
      chat:
        options:
          model: qwen-plus                     # 可选模型

baidu:
  ai-search:
    api-key: your_baidu_search_api_key        # 百度搜索 API Key（联网搜索）

aliyun:
  oss:
    endpoint: your_oss_endpoint
    bucket-name: your_bucket
    access-key-id: your_access_key
    access-key-secret: your_secret
```

### 3. 启动后端

```bash
cd huan-ai-helper-backend
mvn spring-boot:run
```

后端启动于 `http://localhost:8848`，API 文档访问 `http://localhost:8848/api/swagger-ui.html`

### 4. 启动前端

```bash
cd huan-ai-helper-frontend
npm install
npm run dev
```

前端开发服务器启动于 `http://localhost:5173`，已自动代理 `/api` 请求至后端。

---

## 📁 项目结构

```
huan-ai-helper/
├── huan-ai-helper-backend/          # Spring Boot 后端
│   ├── src/main/java/com/huan/aihelper/
│   │   ├── agent/                   # Agent 智能体框架
│   │   │   ├── BaseAgent.java       #   抽象基类（状态机 + 步骤控制）
│   │   │   ├── ReActAgent.java      #   ReAct 推理循环
│   │   │   └── model/              #     AgentState 枚举
│   │   ├── app/
│   │   │   └── ChatApp.java         #   对话核心逻辑（SSE 流式输出）
│   │   ├── chatmemory/              # 对话记忆持久化
│   │   │   ├── DatabaseChatMemory.java  #   数据库实现（生产）
│   │   │   └── FileBasedChatMemory.java # 文件实现（开发）
│   │   ├── config/                  # 配置类（CORS, MyBatis, OSS, VectorStore）
│   │   ├── controller/              # REST API 层
│   │   ├── mapper/                  # MyBatis Mapper
│   │   ├── model/entity/            # 数据实体（Conversation, Message, Summary...）
│   │   ├── service/                 # 业务逻辑层
│   │   ├── rag/                     # RAG 相关（文档加载、向量化、查询）
│   │   ├── tools/                   # AI 工具（时间查询、网页搜索、网页抓取）
│   │   └── advisor/                 # ChatClient Advisor（日志、重读）
│   └── sql/                         # 数据库初始化脚本
│
├── huan-ai-helper-frontend/         # Vue 3 前端
│   └── src/
│       ├── api/                     # API 请求封装
│       ├── assets/images/           # 静态资源（Logo 等）
│       ├── components/              # 组件
│       │   └── ChatPanel.vue        #   聊天面板（消息列表 + 输入框 + 智能体选择）
│       ├── router/                  # 路由配置
│       ├── stores/                  # Pinia 状态管理
│       │   ├── conversation.ts      #   会话与消息状态
│       │   ├── user.ts              #   用户状态
│       │   └── theme.ts             #   主题状态
│       ├── utils/                   # 工具函数（Markdown 渲染）
│       └── views/                   # 页面视图
│           ├── chat/ChatLayout.vue  #   聊天主布局（侧栏 + 主区域）
│           └── user/                #   登录 / 注册 / 个人资料
```

---

## 📝 License

MIT
