-- Flyway V1 migration: initial schema for TruePulseAI

-- Users
CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  user_name VARCHAR(100) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  country_code VARCHAR(10),
  mobile_number VARCHAR(30),
  user_type VARCHAR(50),
  first_name VARCHAR(100),
  last_name VARCHAR(100),
  gender VARCHAR(50),
  date_of_birth DATE,
  is_verified BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE
);

-- Roles
CREATE TABLE IF NOT EXISTS roles (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE,
  description TEXT
);

-- user_roles join table (prevent duplicates)
CREATE TABLE IF NOT EXISTS user_roles (
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
  PRIMARY KEY (user_id, role_id)
);

-- Retros
CREATE TABLE IF NOT EXISTS retros (
  id BIGSERIAL PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_retro_user_id ON retros(user_id);

-- Feedback points (retro_id REQUIRED: feedback point created only after retro exists)
CREATE TABLE IF NOT EXISTS feedback_points (
  id BIGSERIAL PRIMARY KEY,
  type VARCHAR(50) NOT NULL,
  description TEXT NOT NULL,
  retro_id BIGINT NOT NULL REFERENCES retros(id) ON DELETE CASCADE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_feedback_retro_id ON feedback_points(retro_id);

-- Discussions (feedback_point required, user required)
CREATE TABLE IF NOT EXISTS discussions (
  id BIGSERIAL PRIMARY KEY,
  note TEXT NOT NULL,
  feedback_point_id BIGINT NOT NULL REFERENCES feedback_points(id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_discussion_feedback_point_id ON discussions(feedback_point_id);

-- Action items (retro REQUIRED, assigned_user optional)
CREATE TABLE IF NOT EXISTS action_items (
  id BIGSERIAL PRIMARY KEY,
  description TEXT NOT NULL,
  due_date DATE NOT NULL,
  completed BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(50) NOT NULL,
  retro_id BIGINT NOT NULL REFERENCES retros(id) ON DELETE CASCADE,
  assigned_user_id BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
  assigned_user_name VARCHAR(100),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE,
  completed_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_actionitem_retro_id ON action_items(retro_id);
CREATE INDEX IF NOT EXISTS idx_actionitem_assigned_user_id ON action_items(assigned_user_id);

-- Questions (user REQUIRED)
CREATE TABLE IF NOT EXISTS questions (
  id BIGSERIAL PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_question_user_id ON questions(user_id);

-- Answers (question REQUIRED, user REQUIRED)
CREATE TABLE IF NOT EXISTS answers (
  id BIGSERIAL PRIMARY KEY,
  content TEXT NOT NULL,
  question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_answer_question_id ON answers(question_id);
CREATE INDEX IF NOT EXISTS idx_answer_user_id ON answers(user_id);

-- Add additional indexes for roles and users names
CREATE INDEX IF NOT EXISTS idx_role_name ON roles(name);
CREATE INDEX IF NOT EXISTS idx_user_username ON users(user_name);
CREATE INDEX IF NOT EXISTS idx_user_email ON users(email);

