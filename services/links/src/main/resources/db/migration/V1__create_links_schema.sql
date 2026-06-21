CREATE TABLE tb_links (
    id UUID PRIMARY KEY,
    short_code VARCHAR(12) NOT NULL,
    original_url VARCHAR(2048) NOT NULL,
    owner_id UUID NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    click_count BIGINT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_link_short_code UNIQUE (short_code),
    CONSTRAINT ck_link_click_count CHECK (click_count >= 0)
);

CREATE INDEX idx_link_owner_created_at ON tb_links (owner_id, created_at DESC);
