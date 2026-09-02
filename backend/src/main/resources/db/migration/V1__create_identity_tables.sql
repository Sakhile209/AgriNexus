CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone_number VARCHAR(30) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'FARMER',
    account_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_app_user_email UNIQUE (email),
    CONSTRAINT ck_app_user_role CHECK (role IN ('FARMER')),
    CONSTRAINT ck_app_user_status CHECK (account_status IN ('ACTIVE', 'LOCKED', 'DISABLED'))
);

CREATE TABLE farmer_profile (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    preferred_name VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_farmer_profile_user UNIQUE (user_id),
    CONSTRAINT fk_farmer_profile_user
        FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE RESTRICT
);

CREATE INDEX idx_app_user_account_status ON app_user (account_status);

