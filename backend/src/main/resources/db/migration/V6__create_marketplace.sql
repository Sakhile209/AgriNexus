CREATE TABLE market_price (
  id UUID PRIMARY KEY,
  commodity VARCHAR(100) NOT NULL,
  market VARCHAR(120) NOT NULL,
  province VARCHAR(80) NOT NULL,
  unit VARCHAR(40) NOT NULL,
  price_zar NUMERIC(12,2) NOT NULL CHECK (price_zar >= 0),
  source VARCHAR(160) NOT NULL,
  captured_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE marketplace_listing (
  id UUID PRIMARY KEY,
  seller_email VARCHAR(320) NOT NULL,
  title VARCHAR(150) NOT NULL,
  category VARCHAR(80) NOT NULL,
  quantity NUMERIC(12,2) NOT NULL CHECK (quantity > 0),
  unit VARCHAR(40) NOT NULL,
  price_zar NUMERIC(12,2) NOT NULL CHECK (price_zar >= 0),
  location VARCHAR(150) NOT NULL,
  contact VARCHAR(120) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_market_price_captured ON market_price(captured_at DESC);
CREATE INDEX idx_marketplace_active_created ON marketplace_listing(active, created_at DESC);

INSERT INTO market_price(id, commodity, market, province, unit, price_zar, source)
VALUES
  ('10000000-0000-0000-0000-000000000001', 'White maize', 'Indicative national market', 'National', 'ton', 4350.00, 'AgriNexus verified contributor'),
  ('10000000-0000-0000-0000-000000000002', 'Yellow maize', 'Indicative national market', 'National', 'ton', 4210.00, 'AgriNexus verified contributor'),
  ('10000000-0000-0000-0000-000000000003', 'Sunflower seed', 'Indicative national market', 'National', 'ton', 8925.00, 'AgriNexus verified contributor');

INSERT INTO marketplace_listing(id, seller_email, title, category, quantity, unit, price_zar, location, contact)
VALUES
  ('20000000-0000-0000-0000-000000000001', 'market@agrinexus.local', 'Fresh white maize', 'Crops', 10, '50 kg bag', 245.00, 'Gauteng', 'Contact seller through AgriNexus'),
  ('20000000-0000-0000-0000-000000000002', 'market@agrinexus.local', 'Livestock feed', 'Inputs', 25, '25 kg bag', 180.00, 'Free State', 'Contact seller through AgriNexus');
