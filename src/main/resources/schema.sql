-- ═══════════════════════════════════════════════════════════════
-- TrendPulse Database Schema
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS raw_signals (
    id              BIGSERIAL PRIMARY KEY,
    source          VARCHAR(50)     NOT NULL DEFAULT 'reddit',
    subreddit       VARCHAR(100),
    post_id         VARCHAR(50),
    post_title      TEXT,
    comment_body    TEXT,
    author          VARCHAR(100),
    keyword_matched VARCHAR(100),
    product_mentioned VARCHAR(255),
    collected_at    TIMESTAMP       NOT NULL DEFAULT NOW(),
    processed       BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS trends (
    id                      BIGSERIAL PRIMARY KEY,
    product_name            VARCHAR(255)    NOT NULL,
    category                VARCHAR(100),
    trend_score             DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    velocity                DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    velocity_label          VARCHAR(50),
    confidence              DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    ai_explanation          TEXT,
    mention_count_this_week INTEGER         NOT NULL DEFAULT 0,
    mention_count_last_week INTEGER         NOT NULL DEFAULT 0,
    audience_tag            VARCHAR(50)     DEFAULT 'Gen-Z',
    image_url               TEXT,
    brand_mention           VARCHAR(255),
    price_point             VARCHAR(50),
    gender                  VARCHAR(20),
    india_relevant          BOOLEAN         DEFAULT FALSE,
    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS product_links (
    id              BIGSERIAL PRIMARY KEY,
    trend_id        BIGINT          NOT NULL REFERENCES trends(id) ON DELETE CASCADE,
    platform        VARCHAR(50)     NOT NULL,
    affiliate_url   TEXT            NOT NULL,
    price_range     VARCHAR(50),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- ── R5: Session-based personalisation ──
CREATE TABLE IF NOT EXISTS user_preferences (
    id              BIGSERIAL PRIMARY KEY,
    session_id      VARCHAR(255)    NOT NULL UNIQUE,
    liked_categories TEXT[],
    viewed_trend_ids BIGINT[],
    clicked_buy_ids  BIGINT[],
    audience_tag    VARCHAR(50),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_raw_signals_processed ON raw_signals(processed);
CREATE INDEX IF NOT EXISTS idx_raw_signals_collected_at ON raw_signals(collected_at);
CREATE INDEX IF NOT EXISTS idx_trends_category ON trends(category);
CREATE INDEX IF NOT EXISTS idx_trends_velocity_label ON trends(velocity_label);
CREATE INDEX IF NOT EXISTS idx_trends_audience_tag ON trends(audience_tag);
CREATE INDEX IF NOT EXISTS idx_trends_trend_score ON trends(trend_score DESC);
CREATE INDEX IF NOT EXISTS idx_trends_india_relevant ON trends(india_relevant);
CREATE INDEX IF NOT EXISTS idx_trends_gender ON trends(gender);
CREATE INDEX IF NOT EXISTS idx_trends_price_point ON trends(price_point);
CREATE INDEX IF NOT EXISTS idx_product_links_trend_id ON product_links(trend_id);
CREATE INDEX IF NOT EXISTS idx_user_preferences_session ON user_preferences(session_id);

-- ═══════════════════════════════════════════════════════════════
-- Migration SQL (run if tables already exist):
-- ═══════════════════════════════════════════════════════════════
-- ALTER TABLE trends ADD COLUMN IF NOT EXISTS brand_mention VARCHAR(255);
-- ALTER TABLE trends ADD COLUMN IF NOT EXISTS price_point VARCHAR(50);
-- ALTER TABLE trends ADD COLUMN IF NOT EXISTS gender VARCHAR(20);
-- ALTER TABLE trends ADD COLUMN IF NOT EXISTS india_relevant BOOLEAN DEFAULT FALSE;
