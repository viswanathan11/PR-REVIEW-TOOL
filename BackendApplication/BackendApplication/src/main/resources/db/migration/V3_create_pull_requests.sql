CREATE TABLE pull_requests(
    id BIGSERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL REFERENCES repositories(id) ON DELETE CASACDE,
    pr_number INTEGER NOT NULL,
    title TEXT,
    author VARCHAR(100),
    base_branch VARCHAR(100),
    head_branch VARCHAR(100),
    head_sha    VARCHAR(100),
    state       VARCHAR(20),
    github_url  TEXT,
    opened_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    unique(repository_id,pr_number)
);

CREATE INDEX idx_pull_requests_repository_id ON pull_requests(repository_id);
