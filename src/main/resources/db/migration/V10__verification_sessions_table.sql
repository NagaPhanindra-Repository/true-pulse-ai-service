-- V10__verification_sessions_table.sql
-- Create verification_sessions table for persistent session storage
-- Fixes production issue: "Invalid Session ID not found" with load balancing

CREATE TABLE IF NOT EXISTS verification_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(36) NOT NULL UNIQUE,
    requested_user_name VARCHAR(255) NOT NULL,
    requested_email VARCHAR(255),
    country_code VARCHAR(10) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMP,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP NOT NULL,
    created_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for faster lookups
CREATE INDEX idx_verification_sessions_session_id ON verification_sessions(session_id);
CREATE INDEX idx_verification_sessions_expires_at ON verification_sessions(expires_at);
CREATE INDEX idx_verification_sessions_status ON verification_sessions(status);

-- Comment describing the table
COMMENT ON TABLE verification_sessions IS 'Stores verification sessions for user ID verification flow. Sessions auto-expire after 1 hour.';
COMMENT ON COLUMN verification_sessions.session_id IS 'Unique session identifier sent to frontend';
COMMENT ON COLUMN verification_sessions.status IS 'Session status: PENDING, VERIFIED, EXPIRED, REJECTED';
COMMENT ON COLUMN verification_sessions.consumed IS 'Flag to prevent reusing same verification session';
COMMENT ON COLUMN verification_sessions.expires_at IS 'Session expiration timestamp (auto-cleaned up)';

