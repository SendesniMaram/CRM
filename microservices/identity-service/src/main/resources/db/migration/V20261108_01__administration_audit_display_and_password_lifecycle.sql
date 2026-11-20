-- Additive only. Existing accounts retain their current password policy.
ALTER TABLE users ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE administration_events (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,occurred_at TIMESTAMP(6) NOT NULL,
 actor_id BIGINT NULL,target_id BIGINT NOT NULL,action VARCHAR(40) NOT NULL,outcome VARCHAR(20) NOT NULL,
 INDEX idx_admin_event_time(occurred_at,id),INDEX idx_admin_event_target(target_id,id)
);
-- Identifiers are retained as audit references, without a destructive cascade or personal metadata.
CREATE TABLE crm_display_settings (
 id BIGINT NOT NULL PRIMARY KEY,version BIGINT NOT NULL DEFAULT 0,
 company_name VARCHAR(100) NOT NULL,support_email VARCHAR(100) NOT NULL,welcome_message VARCHAR(500) NOT NULL
);
INSERT INTO crm_display_settings(id,version,company_name,support_email,welcome_message) VALUES(1,0,'CRM Workspace','','');
