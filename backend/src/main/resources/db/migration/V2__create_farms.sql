CREATE TABLE farm (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    farm_type VARCHAR(80) NOT NULL,
    province VARCHAR(80) NOT NULL,
    municipality VARCHAR(150),
    latitude NUMERIC(9, 6),
    longitude NUMERIC(9, 6),
    size_value NUMERIC(12, 2),
    size_unit VARCHAR(20),
    main_activities VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_farm_owner FOREIGN KEY (owner_id) REFERENCES app_user (id) ON DELETE RESTRICT,
    CONSTRAINT ck_farm_size CHECK (size_value IS NULL OR size_value > 0),
    CONSTRAINT ck_farm_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_farm_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);

CREATE INDEX idx_farm_owner ON farm (owner_id);

