CREATE TABLE IF NOT EXISTS knowledge_document
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    title
    VARCHAR
(
    255
) NOT NULL,
    original_filename VARCHAR
(
    255
),
    storage_path VARCHAR
(
    255
),
    content_type VARCHAR
(
    100
),
    size BIGINT,
    status INT DEFAULT 0,
    version INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    creator VARCHAR
(
    100
),
    permission VARCHAR
(
    500
),
    group_id BIGINT
    );

CREATE TABLE IF NOT EXISTS document_version
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    document_id
    BIGINT
    NOT
    NULL,
    version_number
    INT
    NOT
    NULL,
    storage_path
    VARCHAR
(
    255
),
    size BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    creator VARCHAR
(
    100
)
    );

CREATE INDEX idx_knowledge_document_creator ON knowledge_document (creator);
CREATE INDEX idx_knowledge_document_group ON knowledge_document (group_id);
CREATE INDEX idx_document_version_doc_id ON document_version (document_id);

CREATE TABLE IF NOT EXISTS audit_log
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    document_id
    BIGINT,
    action
    VARCHAR
(
    50
),
    operator VARCHAR
(
    100
),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    details TEXT
    );

CREATE TABLE IF NOT EXISTS doc_group
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    name
    VARCHAR
(
    255
) NOT NULL,
    owner VARCHAR
(
    100
),
    description VARCHAR
(
    500
),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE UNIQUE INDEX idx_doc_group_owner_name ON doc_group (owner, name);
