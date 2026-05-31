-- ============================================================
-- RAG智能体相关表增量脚本
-- ============================================================

-- 1. 知识库配置表
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

-- 2. 知识库文档表
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

-- 3. 智能体配置表
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

-- 3.1 已有 agent_config 表增加 avatar_url 字段
ALTER TABLE agent_config ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(512) DEFAULT NULL;
COMMENT ON COLUMN agent_config.avatar_url IS '智能体头像URL';

-- 4. 会话表增加 agent_id 字段
ALTER TABLE chat_conversation
ADD COLUMN IF NOT EXISTS agent_id VARCHAR(64) DEFAULT NULL;

COMMENT ON COLUMN chat_conversation.agent_id IS '关联的智能体ID（对应 agent_config.agent_id）';

CREATE INDEX IF NOT EXISTS idx_conversation_agent_id ON chat_conversation (agent_id) WHERE is_deleted = 0;

-- 5. 会话类型注释更新（新增 RAG_AGENT 类型）
COMMENT ON COLUMN chat_conversation.conversation_type IS '会话类型：NORMAL-普通问答 CHATBOT-智能体/角色 AGENT-Autonomous Agent RAG_AGENT-RAG智能体';

-- 6. 向量表 metadata 列类型转换（json → jsonb）+ 索引优化
ALTER TABLE vector_store ALTER COLUMN metadata TYPE jsonb USING metadata::jsonb;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_vector_store_kb_id'
    ) THEN
        CREATE INDEX idx_vector_store_kb_id ON vector_store USING GIN (metadata jsonb_path_ops);
    END IF;
END $$;

-- 7. 消息表增加附件字段
ALTER TABLE chat_message ADD COLUMN IF NOT EXISTS attachments TEXT DEFAULT NULL;
COMMENT ON COLUMN chat_message.attachments IS '消息附件JSON数组，格式：[{"name":"file.pdf","type":"pdf","url":"...","size":1024}]';
