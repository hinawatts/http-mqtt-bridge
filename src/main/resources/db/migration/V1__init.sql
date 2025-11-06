-- src/main/resources/db/migration/V1__initial_schema.sql
-- Create users table
CREATE TABLE brokers (
                         id BIGSERIAL PRIMARY KEY,
                         host_name TEXT NOT NULL UNIQUE,
                         port INTEGER NOT NULL CHECK (port BETWEEN 1 AND 65535),
                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_brokers_hostname ON brokers(host_name);