CREATE TABLE customer_interactions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 customer_id BIGINT NOT NULL,
 employee_id BIGINT NULL,
 interaction_type VARCHAR(20) NOT NULL,
 occurred_at TIMESTAMP NOT NULL,
 notes TEXT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 INDEX idx_interactions_contact_date (customer_id, occurred_at)
);
CREATE TABLE sales_opportunities (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 customer_id BIGINT NOT NULL,
 employee_id BIGINT NULL,
 title VARCHAR(200) NOT NULL,
 description VARCHAR(1000) NULL,
 estimated_amount DECIMAL(12,2) NULL,
 stage VARCHAR(30) NOT NULL DEFAULT 'NEW',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 INDEX idx_opportunity_stage_owner (stage, employee_id)
);
