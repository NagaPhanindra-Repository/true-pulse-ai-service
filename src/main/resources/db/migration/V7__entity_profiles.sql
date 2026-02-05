CREATE TABLE IF NOT EXISTS entity_profiles (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_entity_profiles_user FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_entity_profiles_type ON entity_profiles(type);
CREATE INDEX IF NOT EXISTS idx_entity_profiles_created_by ON entity_profiles(created_by_user_id);

CREATE TABLE IF NOT EXISTS business_profiles (
    entity_id BIGINT PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    description TEXT,
    business_type VARCHAR(255) NOT NULL,
    mobile_number VARCHAR(50) NOT NULL,
    country_code VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL,
    contact_hours VARCHAR(255) NOT NULL,
    CONSTRAINT fk_business_profiles_entity FOREIGN KEY (entity_id) REFERENCES entity_profiles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS business_leader_profiles (
    entity_id BIGINT PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    company VARCHAR(255) NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    project_description TEXT,
    mobile_number VARCHAR(50) NOT NULL,
    country_code VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL,
    contact_hours VARCHAR(255) NOT NULL,
    CONSTRAINT fk_business_leader_profiles_entity FOREIGN KEY (entity_id) REFERENCES entity_profiles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS politician_profiles (
    entity_id BIGINT PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    party_name VARCHAR(255) NOT NULL,
    segment_address VARCHAR(255) NOT NULL,
    contesting_to VARCHAR(255) NOT NULL,
    description TEXT,
    mobile_number VARCHAR(50) NOT NULL,
    country_code VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL,
    contact_hours VARCHAR(255) NOT NULL,
    CONSTRAINT fk_politician_profiles_entity FOREIGN KEY (entity_id) REFERENCES entity_profiles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS celebrity_profiles (
    entity_id BIGINT PRIMARY KEY,
    real_name VARCHAR(255) NOT NULL,
    artist_name VARCHAR(255) NOT NULL,
    artist_type VARCHAR(255) NOT NULL,
    description TEXT,
    mobile_number VARCHAR(50) NOT NULL,
    country_code VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL,
    contact_hours VARCHAR(255) NOT NULL,
    CONSTRAINT fk_celebrity_profiles_entity FOREIGN KEY (entity_id) REFERENCES entity_profiles(id) ON DELETE CASCADE
);
