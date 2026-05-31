-- ============================================================
-- huan-ai-helper 数据库建表脚本
-- 数据库: PostgreSQL
-- ============================================================

-- 1. 用户表
CREATE TABLE IF NOT EXISTS "user" (
    id              BIGSERIAL       PRIMARY KEY,
    user_name       VARCHAR(64)     NOT NULL,
    avatar_url      VARCHAR(512)    DEFAULT NULL,
    email           VARCHAR(128)    DEFAULT NULL,
    phone           VARCHAR(20)     DEFAULT NULL,
    password        VARCHAR(256)    NOT NULL,
    status          SMALLINT        NOT NULL DEFAULT 0,
    last_login_time TIMESTAMP       DEFAULT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  "user"                    IS '用户表';
COMMENT ON COLUMN "user".id                 IS '用户ID';
COMMENT ON COLUMN "user".user_name          IS '用户名';
COMMENT ON COLUMN "user".avatar_url         IS '头像地址';
COMMENT ON COLUMN "user".email              IS '邮箱';
COMMENT ON COLUMN "user".phone              IS '手机号';
COMMENT ON COLUMN "user".password           IS '密码（加密存储）';
COMMENT ON COLUMN "user".status             IS '状态：0-正常 1-禁用';
COMMENT ON COLUMN "user".last_login_time    IS '最后登录时间';
COMMENT ON COLUMN "user".created_at         IS '创建时间';
COMMENT ON COLUMN "user".updated_at         IS '更新时间';
COMMENT ON COLUMN "user".is_deleted         IS '逻辑删除：0-未删除 1-已删除';

CREATE UNIQUE INDEX uk_user_user_name ON "user" (user_name) WHERE is_deleted = 0;
CREATE INDEX idx_user_phone ON "user" (phone) WHERE is_deleted = 0;
CREATE INDEX idx_user_email ON "user" (email) WHERE is_deleted = 0;


-- 2. AI 会话表
CREATE TABLE IF NOT EXISTS chat_conversation (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL,
    title               VARCHAR(256)    DEFAULT NULL,
    system_prompt       TEXT            DEFAULT NULL,
    model_name          VARCHAR(64)     NOT NULL DEFAULT 'qwen-plus',
    conversation_type   VARCHAR(32)     NOT NULL DEFAULT 'NORMAL',
    context_window_size INTEGER         NOT NULL DEFAULT 4096,
    context_strategy    VARCHAR(32)     NOT NULL DEFAULT 'SLIDING_WINDOW',
    temperature         DECIMAL(3,2)    DEFAULT 0.7,
    top_p               DECIMAL(3,2)    DEFAULT NULL,
    status              SMALLINT        NOT NULL DEFAULT 0,
    total_tokens        BIGINT          NOT NULL DEFAULT 0,
    total_messages      INTEGER         NOT NULL DEFAULT 0,
    last_message_time   TIMESTAMP       DEFAULT NULL,
    agent_id            VARCHAR(64)     DEFAULT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  chat_conversation                     IS 'AI会话表';
COMMENT ON COLUMN chat_conversation.id                  IS '会话ID';
COMMENT ON COLUMN chat_conversation.user_id             IS '用户ID';
COMMENT ON COLUMN chat_conversation.title               IS '会话标题';
COMMENT ON COLUMN chat_conversation.system_prompt       IS '系统提示词';
COMMENT ON COLUMN chat_conversation.model_name          IS '使用的模型名称';
COMMENT ON COLUMN chat_conversation.conversation_type   IS '会话类型：NORMAL-普通问答 CHATBOT-智能体/角色 AGENT-Autonomous Agent RAG_AGENT-RAG智能体';
COMMENT ON COLUMN chat_conversation.context_window_size IS '上下文窗口大小（token数）';
COMMENT ON COLUMN chat_conversation.context_strategy    IS '上下文策略：SLIDING_WINDOW-滑动窗口 SUMMARY-摘要压缩 HYBRID-混合';
COMMENT ON COLUMN chat_conversation.temperature         IS '温度参数';
COMMENT ON COLUMN chat_conversation.top_p               IS 'Top-P采样参数';
COMMENT ON COLUMN chat_conversation.status              IS '状态：0-活跃 1-归档 2-已结束';
COMMENT ON COLUMN chat_conversation.total_tokens        IS '累计消耗token数';
COMMENT ON COLUMN chat_conversation.total_messages      IS '累计消息数';
COMMENT ON COLUMN chat_conversation.last_message_time   IS '最后消息时间';
COMMENT ON COLUMN chat_conversation.agent_id            IS '关联的智能体ID（对应 agent_config.agent_id）';
COMMENT ON COLUMN chat_conversation.created_at          IS '创建时间';
COMMENT ON COLUMN chat_conversation.updated_at          IS '更新时间';
COMMENT ON COLUMN chat_conversation.is_deleted          IS '逻辑删除：0-未删除 1-已删除';

CREATE INDEX idx_conversation_user_id ON chat_conversation (user_id, is_deleted, last_message_time DESC);
CREATE INDEX idx_conversation_type ON chat_conversation (user_id, conversation_type, is_deleted);
CREATE INDEX idx_conversation_agent_id ON chat_conversation (agent_id) WHERE is_deleted = 0;


-- 3. 聊天消息表
CREATE TABLE IF NOT EXISTS chat_message (
    id                  BIGSERIAL       PRIMARY KEY,
    conversation_id     BIGINT          NOT NULL,
    parent_message_id   BIGINT          DEFAULT NULL,
    role                VARCHAR(16)     NOT NULL,
    content             TEXT            NOT NULL DEFAULT '',
    model_name          VARCHAR(64)     DEFAULT NULL,
    input_tokens        INTEGER         DEFAULT 0,
    output_tokens       INTEGER         DEFAULT 0,
    total_tokens        INTEGER         DEFAULT 0,
    finish_reason       VARCHAR(32)     DEFAULT NULL,
    tool_calls          JSONB           DEFAULT NULL,
    tool_call_id        VARCHAR(64)     DEFAULT NULL,
    context_included    BOOLEAN         DEFAULT TRUE,
    seq_no              INTEGER         NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  chat_message                        IS '聊天消息表';
COMMENT ON COLUMN chat_message.id                     IS '消息ID';
COMMENT ON COLUMN chat_message.conversation_id        IS '会话ID';
COMMENT ON COLUMN chat_message.parent_message_id      IS '父消息ID（用于消息分支/编辑重发）';
COMMENT ON COLUMN chat_message.role                   IS '角色：user-用户 assistant-助手 system-系统 tool-工具';
COMMENT ON COLUMN chat_message.content                IS '消息内容';
COMMENT ON COLUMN chat_message.model_name             IS '生成该消息的模型名称（仅assistant角色）';
COMMENT ON COLUMN chat_message.input_tokens           IS '输入token数';
COMMENT ON COLUMN chat_message.output_tokens          IS '输出token数';
COMMENT ON COLUMN chat_message.total_tokens           IS '总token数';
COMMENT ON COLUMN chat_message.finish_reason          IS '结束原因：stop-正常 tool_calls-调用工具 length-超长';
COMMENT ON COLUMN chat_message.tool_calls             IS '工具调用信息（JSON数组）';
COMMENT ON COLUMN chat_message.tool_call_id           IS '工具调用ID（仅tool角色）';
COMMENT ON COLUMN chat_message.context_included       IS '是否在当前上下文窗口内';
COMMENT ON COLUMN chat_message.seq_no                 IS '消息序号（会话内递增）';
COMMENT ON COLUMN chat_message.created_at             IS '创建时间';
COMMENT ON COLUMN chat_message.is_deleted             IS '逻辑删除：0-未删除 1-已删除';

CREATE INDEX idx_message_conversation_id ON chat_message (conversation_id, seq_no ASC);
CREATE INDEX idx_message_context ON chat_message (conversation_id, context_included, seq_no ASC) WHERE is_deleted = 0;


-- 4. 上下文摘要表（用于 SUMMARY / HYBRID 策略）
CREATE TABLE IF NOT EXISTS chat_context_summary (
    id                  BIGSERIAL       PRIMARY KEY,
    conversation_id     BIGINT          NOT NULL,
    summary_content     TEXT            NOT NULL,
    summarized_msg_ids  BIGINT[]        NOT NULL,
    token_count         INTEGER         NOT NULL DEFAULT 0,
    seq_start           INTEGER         NOT NULL,
    seq_end             INTEGER         NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  chat_context_summary                     IS '上下文摘要表';
COMMENT ON COLUMN chat_context_summary.id                  IS '摘要ID';
COMMENT ON COLUMN chat_context_summary.conversation_id     IS '会话ID';
COMMENT ON COLUMN chat_context_summary.summary_content     IS '摘要内容';
COMMENT ON COLUMN chat_context_summary.summarized_msg_ids  IS '被摘要的消息ID数组';
COMMENT ON COLUMN chat_context_summary.token_count         IS '摘要的token数';
COMMENT ON COLUMN chat_context_summary.seq_start           IS '摘要覆盖的起始消息序号';
COMMENT ON COLUMN chat_context_summary.seq_end             IS '摘要覆盖的结束消息序号';
COMMENT ON COLUMN chat_context_summary.created_at          IS '创建时间';
COMMENT ON COLUMN chat_context_summary.is_deleted          IS '逻辑删除：0-未删除 1-已删除';

CREATE INDEX idx_summary_conversation_id ON chat_context_summary (conversation_id, seq_end DESC) WHERE is_deleted = 0;


-- 5. 知识库配置表
CREATE TABLE IF NOT EXISTS knowledge_base (
    id                  BIGSERIAL       PRIMARY KEY,
    kb_id               VARCHAR(64)     NOT NULL UNIQUE,
    kb_name             VARCHAR(128)    NOT NULL,
    description         TEXT            DEFAULT NULL,
    document_count      INTEGER         NOT NULL DEFAULT 0,
    chunk_count         INTEGER         NOT NULL DEFAULT 0,
    status              SMALLINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  knowledge_base                  IS '知识库配置表';
COMMENT ON COLUMN knowledge_base.id               IS '主键ID';
COMMENT ON COLUMN knowledge_base.kb_id            IS '知识库唯一标识';
COMMENT ON COLUMN knowledge_base.kb_name          IS '知识库名称';
COMMENT ON COLUMN knowledge_base.description      IS '知识库描述';
COMMENT ON COLUMN knowledge_base.document_count   IS '文档数量';
COMMENT ON COLUMN knowledge_base.chunk_count      IS '分块总数量';
COMMENT ON COLUMN knowledge_base.status           IS '状态：0-正常 1-禁用';
COMMENT ON COLUMN knowledge_base.created_at       IS '创建时间';
COMMENT ON COLUMN knowledge_base.updated_at       IS '更新时间';
COMMENT ON COLUMN knowledge_base.is_deleted       IS '逻辑删除：0-未删除 1-已删除';

CREATE INDEX idx_knowledge_base_kb_id ON knowledge_base (kb_id) WHERE is_deleted = 0;


-- 6. 知识库文档表
CREATE TABLE IF NOT EXISTS knowledge_document (
    id                  BIGSERIAL       PRIMARY KEY,
    kb_id               VARCHAR(64)     NOT NULL,
    doc_id              VARCHAR(64)     NOT NULL UNIQUE,
    file_name           VARCHAR(256)    NOT NULL,
    file_type           VARCHAR(32)     NOT NULL,
    chunk_count         INTEGER         NOT NULL DEFAULT 0,
    status              VARCHAR(16)     NOT NULL DEFAULT 'PROCESSING',
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  knowledge_document                IS '知识库文档表';
COMMENT ON COLUMN knowledge_document.id             IS '主键ID';
COMMENT ON COLUMN knowledge_document.kb_id          IS '所属知识库ID';
COMMENT ON COLUMN knowledge_document.doc_id         IS '文档唯一标识';
COMMENT ON COLUMN knowledge_document.file_name      IS '原始文件名';
COMMENT ON COLUMN knowledge_document.file_type      IS '文件类型：md/txt';
COMMENT ON COLUMN knowledge_document.chunk_count    IS '该文档的分块数量';
COMMENT ON COLUMN knowledge_document.status         IS '处理状态：PROCESSING-处理中 COMPLETED-已完成 FAILED-失败';
COMMENT ON COLUMN knowledge_document.created_at     IS '上传时间';
COMMENT ON COLUMN knowledge_document.is_deleted     IS '逻辑删除：0-未删除 1-已删除';

CREATE INDEX idx_knowledge_document_kb_id ON knowledge_document (kb_id) WHERE is_deleted = 0;


-- 7. 智能体配置表
CREATE TABLE IF NOT EXISTS agent_config (
    id                  BIGSERIAL       PRIMARY KEY,
    agent_id            VARCHAR(64)     NOT NULL UNIQUE,
    agent_name          VARCHAR(128)    NOT NULL,
    description         TEXT            DEFAULT NULL,
    avatar_url          VARCHAR(512)    DEFAULT NULL,
    system_prompt       TEXT            NOT NULL,
    knowledge_base_id   VARCHAR(64)     DEFAULT NULL,
    max_steps           INTEGER         NOT NULL DEFAULT 10,
    status              SMALLINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  agent_config                    IS '智能体配置表';
COMMENT ON COLUMN agent_config.id                 IS '主键ID';
COMMENT ON COLUMN agent_config.agent_id           IS '智能体唯一标识';
COMMENT ON COLUMN agent_config.agent_name         IS '智能体名称（前端展示）';
COMMENT ON COLUMN agent_config.description        IS '智能体描述';
COMMENT ON COLUMN agent_config.avatar_url         IS '智能体头像URL';
COMMENT ON COLUMN agent_config.system_prompt      IS '系统提示词';
COMMENT ON COLUMN agent_config.knowledge_base_id  IS '关联的知识库ID';
COMMENT ON COLUMN agent_config.max_steps          IS '最大执行步数';
COMMENT ON COLUMN agent_config.status             IS '状态：0-正常 1-禁用';
COMMENT ON COLUMN agent_config.created_at         IS '创建时间';
COMMENT ON COLUMN agent_config.updated_at         IS '更新时间';
COMMENT ON COLUMN agent_config.is_deleted         IS '逻辑删除：0-未删除 1-已删除';

CREATE INDEX idx_agent_config_agent_id ON agent_config (agent_id) WHERE is_deleted = 0;
CREATE INDEX idx_agent_config_kb_id ON agent_config (knowledge_base_id) WHERE is_deleted = 0;


-- 8. 向量表 metadata 列类型转换（json → jsonb）+ 索引优化
-- 注意：vector_store 表由 Spring AI PgVectorStore 自动创建，metadata 列可能为 json 类型
-- 需要转为 jsonb 才能创建 GIN 索引
ALTER TABLE vector_store ALTER COLUMN metadata TYPE jsonb USING metadata::jsonb;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_vector_store_kb_id'
    ) THEN
        CREATE INDEX idx_vector_store_kb_id ON vector_store USING GIN (metadata jsonb_path_ops);
    END IF;
END $$;
