-- Feature Memory System Tables
-- This migration creates the core tables for the TruePulse AI Memory System

-- Jira Integration Table
CREATE TABLE IF NOT EXISTS jira_integrations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    jira_url VARCHAR(500) NOT NULL,
    jira_email VARCHAR(255) NOT NULL,
    encrypted_api_token TEXT NOT NULL,
    project_keys TEXT[] DEFAULT ARRAY[]::TEXT[],
    is_active BOOLEAN DEFAULT true,
    last_sync_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, jira_url)
);

CREATE INDEX idx_jira_integrations_user ON jira_integrations(user_id);
CREATE INDEX idx_jira_integrations_active ON jira_integrations(is_active);

-- Feature Memory (Core Entity)
CREATE TABLE IF NOT EXISTS feature_memories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    jira_integration_id UUID REFERENCES jira_integrations(id),
    jira_story_id VARCHAR(100),
    jira_story_key VARCHAR(50) NOT NULL,
    jira_story_title TEXT,
    jira_story_description TEXT,
    jira_story_type VARCHAR(50),
    jira_assignee VARCHAR(255),
    jira_status VARCHAR(100),
    initial_description TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    UNIQUE(jira_integration_id, jira_story_key)
);

CREATE INDEX idx_feature_memories_user ON feature_memories(user_id);
CREATE INDEX idx_feature_memories_story_key ON feature_memories(jira_story_key);
CREATE INDEX idx_feature_memories_status ON feature_memories(status);

-- Memory Discussions (Timeline Entries)
CREATE TABLE IF NOT EXISTS memory_discussions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feature_memory_id UUID NOT NULL REFERENCES feature_memories(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    discussion_text TEXT NOT NULL,
    decision_type VARCHAR(50) NOT NULL,
    tags TEXT[] DEFAULT ARRAY[]::TEXT[],
    meeting_date DATE,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_discussions_memory ON memory_discussions(feature_memory_id);
CREATE INDEX idx_discussions_type ON memory_discussions(decision_type);
CREATE INDEX idx_discussions_recorded ON memory_discussions(recorded_at);
CREATE INDEX idx_discussions_user ON memory_discussions(user_id);

-- Memory Attachments
CREATE TABLE IF NOT EXISTS memory_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feature_memory_id UUID REFERENCES feature_memories(id) ON DELETE CASCADE,
    discussion_id UUID REFERENCES memory_discussions(id) ON DELETE CASCADE,
    file_name VARCHAR(255),
    file_url TEXT,
    file_type VARCHAR(100),
    file_size_bytes BIGINT,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_memory_attachments_feature ON memory_attachments(feature_memory_id);
CREATE INDEX idx_memory_attachments_discussion ON memory_attachments(discussion_id);

-- Git Branch Mappings
CREATE TABLE IF NOT EXISTS git_branch_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feature_memory_id UUID NOT NULL REFERENCES feature_memories(id) ON DELETE CASCADE,
    branch_name VARCHAR(255) NOT NULL,
    repository_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(feature_memory_id, branch_name)
);

CREATE INDEX idx_branch_name ON git_branch_mappings(branch_name);
CREATE INDEX idx_branch_feature_memory ON git_branch_mappings(feature_memory_id);

