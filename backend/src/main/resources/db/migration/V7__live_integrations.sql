-- Additive only: retain all existing farmer and marketplace records.
CREATE TABLE external_snapshot (
 cache_key VARCHAR(220) PRIMARY KEY, payload TEXT NOT NULL,
 retrieved_at TIMESTAMPTZ NOT NULL, expires_at TIMESTAMPTZ NOT NULL
);
ALTER TABLE market_price ADD COLUMN published_on DATE;
ALTER TABLE market_price ADD COLUMN source_url TEXT;
ALTER TABLE market_price ADD COLUMN verified BOOLEAN NOT NULL DEFAULT FALSE;
CREATE UNIQUE INDEX market_price_publication ON market_price(commodity, market, province, unit, source, published_on);
ALTER TABLE marketplace_listing ADD COLUMN province VARCHAR(80);
ALTER TABLE marketplace_listing ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE marketplace_listing ADD COLUMN photo BYTEA;
ALTER TABLE marketplace_listing ADD COLUMN photo_type VARCHAR(30);
CREATE INDEX marketplace_province ON marketplace_listing(province, active);
