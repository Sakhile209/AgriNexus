CREATE TABLE field (
 id UUID PRIMARY KEY, farm_id UUID NOT NULL, name VARCHAR(150) NOT NULL,
 size_value NUMERIC(12,2), size_unit VARCHAR(20), soil_type VARCHAR(100), notes VARCHAR(1000),
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_field_farm FOREIGN KEY(farm_id) REFERENCES farm(id) ON DELETE RESTRICT,
 CONSTRAINT uq_field_farm_name UNIQUE(farm_id,name),
 CONSTRAINT ck_field_size CHECK(size_value IS NULL OR size_value > 0)
);
CREATE INDEX idx_field_farm ON field(farm_id);

CREATE TABLE crop (
 id UUID PRIMARY KEY, field_id UUID NOT NULL, crop_type VARCHAR(120) NOT NULL, variety VARCHAR(120),
 planting_date DATE NOT NULL, expected_harvest_date DATE, actual_harvest_date DATE,
 status VARCHAR(30) NOT NULL DEFAULT 'PLANNED', notes VARCHAR(1000),
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_crop_field FOREIGN KEY(field_id) REFERENCES field(id) ON DELETE RESTRICT,
 CONSTRAINT ck_crop_status CHECK(status IN('PLANNED','PLANTED','GROWING','HARVESTED','FAILED')),
 CONSTRAINT ck_crop_expected_date CHECK(expected_harvest_date IS NULL OR expected_harvest_date >= planting_date),
 CONSTRAINT ck_crop_actual_date CHECK(actual_harvest_date IS NULL OR actual_harvest_date >= planting_date)
);
CREATE INDEX idx_crop_field_status ON crop(field_id,status);

CREATE TABLE crop_activity (
 id UUID PRIMARY KEY, crop_id UUID NOT NULL, activity_type VARCHAR(50) NOT NULL,
 activity_date DATE NOT NULL, details VARCHAR(1000), notes VARCHAR(1000),
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_crop_activity_crop FOREIGN KEY(crop_id) REFERENCES crop(id) ON DELETE RESTRICT
);
CREATE INDEX idx_crop_activity_crop_date ON crop_activity(crop_id,activity_date DESC);

CREATE TABLE soil_record (
 id UUID PRIMARY KEY, field_id UUID NOT NULL, recorded_on DATE NOT NULL, soil_type VARCHAR(100),
 moisture VARCHAR(20), ph NUMERIC(4,2), nitrogen NUMERIC(12,3), phosphorus NUMERIC(12,3),
 potassium NUMERIC(12,3), electrical_conductivity NUMERIC(12,3), laboratory_name VARCHAR(200),
 notes VARCHAR(1000), created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_soil_record_field FOREIGN KEY(field_id) REFERENCES field(id) ON DELETE RESTRICT,
 CONSTRAINT ck_soil_moisture CHECK(moisture IS NULL OR moisture IN('DRY','MOIST','WET')),
 CONSTRAINT ck_soil_ph CHECK(ph IS NULL OR ph BETWEEN 0 AND 14)
);
CREATE INDEX idx_soil_record_field_date ON soil_record(field_id,recorded_on DESC);

