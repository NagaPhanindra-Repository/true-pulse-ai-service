-- Flyway V2 migration: User Follower System
-- Creates user_followers table for managing follower relationships
-- Designed to scale to millions of followers per user

-- User Followers table
CREATE TABLE IF NOT EXISTS user_followers (
    id BIGSERIAL PRIMARY KEY,
    user_username VARCHAR(100) NOT NULL,
    follower_username VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Unique constraint to prevent duplicate follow relationships
    CONSTRAINT uk_user_follower UNIQUE (user_username, follower_username),

    -- Check constraint to prevent self-following
    CONSTRAINT chk_no_self_follow CHECK (user_username <> follower_username)
);

-- Indexes for performance at scale
-- Index for finding all followers of a user (most common query)
CREATE INDEX IF NOT EXISTS idx_user_followers_user_username
    ON user_followers(user_username);

-- Index for finding all users that a user is following
CREATE INDEX IF NOT EXISTS idx_user_followers_follower_username
    ON user_followers(follower_username);

-- Index for sorting by created date (recent followers)
CREATE INDEX IF NOT EXISTS idx_user_followers_created_at
    ON user_followers(created_at DESC);

-- Composite index for active followers query optimization
CREATE INDEX IF NOT EXISTS idx_user_followers_user_active
    ON user_followers(user_username, is_active, created_at DESC);

-- Comments for documentation
COMMENT ON TABLE user_followers IS 'Stores follower relationships between users. One user can have millions of followers.';
COMMENT ON COLUMN user_followers.user_username IS 'Username of the user being followed';
COMMENT ON COLUMN user_followers.follower_username IS 'Username of the follower';
COMMENT ON COLUMN user_followers.created_at IS 'When the follow relationship was created';
COMMENT ON COLUMN user_followers.is_active IS 'Whether the follower relationship is active (for soft deletes or blocking)';

