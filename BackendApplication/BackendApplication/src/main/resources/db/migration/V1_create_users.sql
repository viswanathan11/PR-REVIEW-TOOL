CREATE TABLE users{
    -- BIGSERIAL auto-incrmenting id
    id  BIGSERIAL PRIMARY KEY,
    github_id  VARCHAR(50) UNIQUE NOT NULL,
    github_login VARCHAR(100) NOT NULL,
    avatar_url   TEXT,
    access_token TEXT NOT NULL,
    -- Timestamp with timezone  ~TZ = ~TIMEZONE
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
};
--  This ensure that the look up is done based on the github_id
CREATE INDEX idx_users_github_id ON users(github_id);