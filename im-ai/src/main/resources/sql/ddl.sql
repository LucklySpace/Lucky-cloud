CREATE
EXTENSION IF NOT EXISTS vector;
CREATE
EXTENSION IF NOT EXISTS hstore;
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS vector_store
(
    id
    uuid
    DEFAULT
    uuid_generate_v4
(
) PRIMARY KEY,
    content text,
    metadata json,
    embedding vector
(
    1536
)
    );

CREATE INDEX ON vector_store USING HNSW (embedding vector_cosine_ops);



CREATE TABLE chat_session
(
    id              VARCHAR(64) PRIMARY KEY,
    user_id         VARCHAR(64) NOT NULL,
    title           VARCHAR(255),
    prompt_id       VARCHAR(64),
    summary         TEXT,
    last_message_at TIMESTAMP,
    message_count   INTEGER     DEFAULT 0,
    status          VARCHAR(50) DEFAULT 'active',
    category        VARCHAR(100),
    created_at      TIMESTAMP,
    version         INTEGER     DEFAULT 0
);

COMMENT
ON TABLE chat_session IS '聊天会话表';

COMMENT
ON COLUMN chat_session.id IS '会话 ID，通常为 UUID';
COMMENT
ON COLUMN chat_session.user_id IS '用户ID';
COMMENT
ON COLUMN chat_session.title IS '会话标题';
COMMENT
ON COLUMN chat_session.prompt_id IS '默认系统提示词ID';
COMMENT
ON COLUMN chat_session.summary IS '摘要，用于快速预览会话内容';
COMMENT
ON COLUMN chat_session.last_message_at IS '最近一条消息的时间';
COMMENT
ON COLUMN chat_session.message_count IS '消息数量';
COMMENT
ON COLUMN chat_session.status IS '会话状态，如 active / archived / deleted';
COMMENT
ON COLUMN chat_session.category IS '分类标签';
COMMENT
ON COLUMN chat_session.created_at IS '创建时间';
COMMENT
ON COLUMN chat_session.version IS '乐观锁版本号';

-- 可以考虑为 user_id 建立索引，加速查询：
CREATE INDEX idx_chat_session_user_id ON chat_session (user_id);


CREATE TABLE chat_prompt
(
    id          VARCHAR(64) PRIMARY KEY,
    name        VARCHAR(255),
    prompt      TEXT,
    description VARCHAR(500),
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    version     INTEGER DEFAULT 0
);

COMMENT
ON TABLE chat_prompt IS 'Prompt 配置实体，用于保存系统提示词或输入模板';

COMMENT
ON COLUMN chat_prompt.id IS 'Prompt 唯一标识 ID，通常为 UUID';
COMMENT
ON COLUMN chat_prompt.name IS 'Prompt 名称，用于界面展示或搜索';
COMMENT
ON COLUMN chat_prompt.prompt IS 'Prompt 内容，可为系统提示语、用户输入模板等';
COMMENT
ON COLUMN chat_prompt.description IS 'Prompt 描述，用于说明用途、适用场景等';
COMMENT
ON COLUMN chat_prompt.created_at IS '创建时间，由系统自动设置';
COMMENT
ON COLUMN chat_prompt.updated_at IS '更新时间，由系统自动维护';
COMMENT
ON COLUMN chat_prompt.version IS '版本号，用于乐观锁控制';



CREATE TABLE chat_message
(
    id         VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64),
    type       VARCHAR(50),
    content    TEXT,
    prompt_id  VARCHAR(64),
    streamed   BOOLEAN,
    parent_id  VARCHAR(64),
    version    INTEGER DEFAULT 0,
    created_at TIMESTAMP
);

COMMENT
ON TABLE chat_message IS '聊天消息实体，记录用户提问、AI 回复、系统提示等内容';

COMMENT
ON COLUMN chat_message.id IS '消息主键 ID，通常使用 UUID';
COMMENT
ON COLUMN chat_message.session_id IS '所属的对话会话 ID';
COMMENT
ON COLUMN chat_message.type IS '消息类型，如 user / assistant / system';
COMMENT
ON COLUMN chat_message.content IS '消息正文内容，如提问文本、AI 回复等';
COMMENT
ON COLUMN chat_message.prompt_id IS '关联的 Prompt ID，表示该消息使用的提示模板，可为空';
COMMENT
ON COLUMN chat_message.streamed IS '是否为流式生成（如 SSE 等），true 表示是';
COMMENT
ON COLUMN chat_message.parent_id IS '父消息 ID，用于多轮对话上下文引用，null 表示对话起点';
COMMENT
ON COLUMN chat_message.version IS '乐观锁版本号';
COMMENT
ON COLUMN chat_message.created_at IS '消息创建时间';

-- 为 session_id 建索引，提升查询性能
CREATE INDEX idx_chat_message_session_id ON chat_message (session_id);
