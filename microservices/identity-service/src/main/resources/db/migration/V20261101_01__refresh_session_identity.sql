-- Prepared for Lot 1B. Review/backup and apply explicitly in an authorized integration phase.
-- NULL legacy sessions remain stored but cannot authenticate: users must log in again.
ALTER TABLE refresh_tokens ADD COLUMN session_id VARCHAR(36) NULL;
CREATE INDEX idx_refresh_session_state ON refresh_tokens (user_id, session_id, revoked, expiry_date);
