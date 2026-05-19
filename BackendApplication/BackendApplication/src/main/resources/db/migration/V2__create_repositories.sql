CREATE TABLE repositories (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    github_repo_id VARCHAR(50) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    description TEXT,
    private BOOLEAN DEFAULT FALSE,
    webhook_id VARCHAR(50),
    webhook_active BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id,github_repo_id)
);

CREATE INDEX idx_repository_user_id ON repositories(user_id);

-- REFERENCES users(id) = forign key - this column must match an id in the user table
-- ON DEFAULT CASACDE = if a user is deleted, delete their repos too
-- UNIQUE(user_id,github_repo_id) = composite unique constraint (i.e. same user cannot have the same repo twice)