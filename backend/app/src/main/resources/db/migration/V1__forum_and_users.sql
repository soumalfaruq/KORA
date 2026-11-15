CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    username VARCHAR(32) NOT NULL UNIQUE,
    email VARCHAR(254) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(16) NOT NULL DEFAULT 'MEMBER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT app_users_username_format CHECK (username ~ '^[a-zA-Z0-9_]{3,32}$'),
    CONSTRAINT app_users_role CHECK (role IN ('MEMBER', 'MODERATOR', 'ADMIN'))
);
CREATE UNIQUE INDEX app_users_username_lower_idx ON app_users (lower(username));
CREATE UNIQUE INDEX app_users_email_lower_idx ON app_users (lower(email));

CREATE TABLE questions (
    id UUID PRIMARY KEY,
    title VARCHAR(180) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    author_id UUID NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT questions_status CHECK (status IN ('OPEN', 'RESOLVED', 'CLOSED'))
);

CREATE TABLE answers (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE RESTRICT,
    author_id UUID NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE questions
    ADD COLUMN accepted_answer_id UUID UNIQUE REFERENCES answers(id) ON DELETE SET NULL;

CREATE TABLE question_tags (
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    tag VARCHAR(40) NOT NULL,
    PRIMARY KEY (question_id, tag)
);

CREATE INDEX questions_created_at_idx ON questions (created_at DESC);
CREATE INDEX questions_status_created_at_idx ON questions (status, created_at DESC);
CREATE INDEX answers_question_id_idx ON answers (question_id, created_at);
CREATE INDEX question_tags_tag_idx ON question_tags (tag);
