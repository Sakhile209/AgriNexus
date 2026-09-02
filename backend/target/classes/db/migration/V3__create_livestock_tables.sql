CREATE TABLE animal (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    internal_id VARCHAR(80) NOT NULL,
    ear_tag_number VARCHAR(100),
    species VARCHAR(80) NOT NULL,
    breed VARCHAR(100),
    sex VARCHAR(20),
    date_of_birth DATE,
    approximate_age_months INTEGER,
    colour VARCHAR(80),
    identifying_markings VARCHAR(300),
    weight_kg NUMERIC(10, 2),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    notes VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_animal_farm FOREIGN KEY (farm_id) REFERENCES farm (id) ON DELETE RESTRICT,
    CONSTRAINT uq_animal_farm_internal_id UNIQUE (farm_id, internal_id),
    CONSTRAINT ck_animal_age CHECK (approximate_age_months IS NULL OR approximate_age_months >= 0),
    CONSTRAINT ck_animal_weight CHECK (weight_kg IS NULL OR weight_kg > 0),
    CONSTRAINT ck_animal_status CHECK (status IN ('ACTIVE', 'SOLD', 'DECEASED', 'LOST'))
);
CREATE INDEX idx_animal_farm_status ON animal (farm_id, status);
CREATE INDEX idx_animal_farm_species ON animal (farm_id, species);

CREATE TABLE animal_event (
    id UUID PRIMARY KEY,
    animal_id UUID NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    event_date DATE NOT NULL,
    weight_kg NUMERIC(10, 2),
    from_location VARCHAR(150),
    to_location VARCHAR(150),
    notes VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_animal_event_animal FOREIGN KEY (animal_id) REFERENCES animal (id) ON DELETE RESTRICT,
    CONSTRAINT ck_animal_event_weight CHECK (weight_kg IS NULL OR weight_kg > 0)
);
CREATE INDEX idx_animal_event_animal_date ON animal_event (animal_id, event_date DESC);

CREATE TABLE animal_health_record (
    id UUID PRIMARY KEY,
    animal_id UUID NOT NULL,
    observed_symptoms VARCHAR(1500) NOT NULL,
    symptoms_started_on DATE,
    veterinarian_contacted BOOLEAN NOT NULL DEFAULT FALSE,
    veterinarian_visit_on DATE,
    follow_up_on DATE,
    notes VARCHAR(1500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_health_record_animal FOREIGN KEY (animal_id) REFERENCES animal (id) ON DELETE RESTRICT
);
CREATE INDEX idx_health_record_animal_date ON animal_health_record (animal_id, created_at DESC);
CREATE INDEX idx_health_record_follow_up ON animal_health_record (follow_up_on) WHERE follow_up_on IS NOT NULL;

CREATE TABLE treatment (
    id UUID PRIMARY KEY,
    health_record_id UUID NOT NULL,
    treatment VARCHAR(500) NOT NULL,
    medication VARCHAR(300),
    treatment_date DATE NOT NULL,
    administered_by VARCHAR(200),
    notes VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_treatment_health_record FOREIGN KEY (health_record_id) REFERENCES animal_health_record (id) ON DELETE RESTRICT
);
CREATE INDEX idx_treatment_health_record_date ON treatment (health_record_id, treatment_date DESC);

CREATE TABLE vaccination (
    id UUID PRIMARY KEY,
    animal_id UUID NOT NULL,
    vaccine_name VARCHAR(200) NOT NULL,
    administered_on DATE NOT NULL,
    next_due_on DATE,
    administered_by VARCHAR(200),
    notes VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vaccination_animal FOREIGN KEY (animal_id) REFERENCES animal (id) ON DELETE RESTRICT,
    CONSTRAINT ck_vaccination_dates CHECK (next_due_on IS NULL OR next_due_on >= administered_on)
);
CREATE INDEX idx_vaccination_animal_date ON vaccination (animal_id, administered_on DESC);
CREATE INDEX idx_vaccination_next_due ON vaccination (next_due_on) WHERE next_due_on IS NOT NULL;

