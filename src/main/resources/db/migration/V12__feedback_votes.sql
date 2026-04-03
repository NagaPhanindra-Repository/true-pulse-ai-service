-- Feedback Voting Table Migration
-- This migration creates the feedback_votes table for voting on feedback points

CREATE TABLE IF NOT EXISTS feedback_votes (
    id BIGSERIAL PRIMARY KEY,
    feedback_point_id BIGINT NOT NULL REFERENCES feedback_points(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vote_type VARCHAR(10) NOT NULL CHECK (vote_type IN ('LIKE', 'DISLIKE')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(feedback_point_id, user_id)
);

CREATE INDEX idx_feedback_votes_feedback_point_id ON feedback_votes(feedback_point_id);
CREATE INDEX idx_feedback_votes_user_id ON feedback_votes(user_id);
