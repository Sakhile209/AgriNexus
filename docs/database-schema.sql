-- AgriNexus PostgreSQL schema

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE roles (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name VARCHAR(100) NOT NULL UNIQUE,
  description TEXT
);

CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(255) NOT NULL,
  phone VARCHAR(50),
  role_id UUID REFERENCES roles(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE farmers (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES users(id) UNIQUE,
  gender VARCHAR(20),
  location VARCHAR(255),
  province VARCHAR(100),
  farm_size_ha NUMERIC(10,2),
  preferred_language VARCHAR(50)
);

CREATE TABLE farms (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  farmer_id UUID REFERENCES farmers(id),
  name VARCHAR(255) NOT NULL,
  location VARCHAR(255),
  province VARCHAR(100),
  farm_type VARCHAR(100),
  size_ha NUMERIC(10,2)
);

CREATE TABLE livestock (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  farm_id UUID REFERENCES farms(id),
  type VARCHAR(100) NOT NULL,
  breed VARCHAR(100),
  gender VARCHAR(20),
  birth_date DATE,
  status VARCHAR(50),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE vaccinations (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  livestock_id UUID REFERENCES livestock(id),
  vaccine_name VARCHAR(200) NOT NULL,
  scheduled_date DATE,
  administered_date DATE,
  notes TEXT
);

CREATE TABLE animal_health_records (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  livestock_id UUID REFERENCES livestock(id),
  record_date DATE NOT NULL,
  condition VARCHAR(200),
  symptoms TEXT,
  treatment TEXT,
  severity VARCHAR(50)
);

CREATE TABLE crops (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  farm_id UUID REFERENCES farms(id),
  crop_name VARCHAR(150) NOT NULL,
  variety VARCHAR(150),
  planting_date DATE,
  expected_harvest_date DATE,
  status VARCHAR(50)
);

CREATE TABLE crop_activities (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  crop_id UUID REFERENCES crops(id),
  activity_type VARCHAR(100) NOT NULL,
  activity_date DATE NOT NULL,
  notes TEXT
);

CREATE TABLE weather_data (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  region VARCHAR(150) NOT NULL,
  weather_date DATE NOT NULL,
  rainfall_mm NUMERIC(8,2),
  temperature_c NUMERIC(5,2),
  forecast TEXT
);

CREATE TABLE market_prices (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  commodity VARCHAR(150) NOT NULL,
  region VARCHAR(150),
  price_per_unit NUMERIC(10,2) NOT NULL,
  currency CHAR(3) DEFAULT 'ZAR',
  recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE suppliers (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES users(id) UNIQUE,
  company_name VARCHAR(255) NOT NULL,
  category VARCHAR(100),
  location VARCHAR(255)
);

CREATE TABLE products (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  supplier_id UUID REFERENCES suppliers(id),
  name VARCHAR(255) NOT NULL,
  category VARCHAR(100) NOT NULL,
  unit_price NUMERIC(10,2),
  stock_quantity NUMERIC(10,2),
  description TEXT
);

CREATE TABLE buyers (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES users(id) UNIQUE,
  company_name VARCHAR(255),
  location VARCHAR(255),
  buyer_type VARCHAR(100)
);

CREATE TABLE orders (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  buyer_id UUID REFERENCES buyers(id),
  farmer_id UUID REFERENCES farmers(id),
  product_id UUID REFERENCES products(id),
  quantity NUMERIC(10,2),
  total_amount NUMERIC(10,2),
  status VARCHAR(50),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transactions (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  order_id UUID REFERENCES orders(id),
  amount NUMERIC(10,2) NOT NULL,
  payment_status VARCHAR(50),
  payment_method VARCHAR(100),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notifications (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES users(id),
  title VARCHAR(255) NOT NULL,
  body TEXT,
  is_read BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reports (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  report_type VARCHAR(100) NOT NULL,
  owner_id UUID,
  generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  summary TEXT
);

CREATE INDEX idx_farms_farmer_id ON farms(farmer_id);
CREATE INDEX idx_livestock_farm_id ON livestock(farm_id);
CREATE INDEX idx_vaccinations_livestock_id ON vaccinations(livestock_id);
CREATE INDEX idx_animal_health_livestock_id ON animal_health_records(livestock_id);
CREATE INDEX idx_crops_farm_id ON crops(farm_id);
CREATE INDEX idx_crop_activities_crop_id ON crop_activities(crop_id);
CREATE INDEX idx_weather_region_date ON weather_data(region, weather_date);
CREATE INDEX idx_market_prices_commodity_region ON market_prices(commodity, region);
CREATE INDEX idx_products_supplier_id ON products(supplier_id);
CREATE INDEX idx_orders_buyer_id ON orders(buyer_id);
CREATE INDEX idx_orders_farmer_id ON orders(farmer_id);
CREATE INDEX idx_orders_status ON orders(status);
