CREATE TYPE review_status AS ENUM('PENDING','PROCESSING','DONE','FAILED');


CREATE TABLE reviews(
    id          BIGSERIAL PRIMARY KEY,
    pull_request_id     BIGINT NOT NULL REFERENCES pull_requests(id) ON DELETE CASCADE,
    status              review_status NOT NULL DEFAULT 'PENDING',
    model_used          VARCHAR(100),
    review_summary      TEXT,
    overall_score       INTEGER,
    issues_found        INTEGER DEFAULT 0,
    raw_response        JSONB,
    posted_to_github    BOOLEAN DEFAULT FALSE,
    error_message       TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(), 
    completed_at        TIMESTAMPTZ 
);

CREATE TABLE review_comments(
    id          BIGSERIAL PRIMARY KEY,
    review_id   BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    file_path   TEXT NOT NULL,
    line_number INTEGER,
    severity    VARCHAR(20) NOT NULL,
    comment    TEXT NOT NULL,
    suggestion  TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reviws_pull_request_id ON reviews(pull_request_id);
CREATE INDEX idx_review_comments_review_id ON review_comments(review_id);


-- key learnings:
-- CREATE TYPE ... AS ENUM = PostgreSql custome type (resitrics value to only those 4)
-- JSONB = store JSON data in a binary formate (fast to query). This Stores the raw AI respone
-- two tbale in one migration file 