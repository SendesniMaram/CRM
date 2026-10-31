ALTER TABLE customers ADD COLUMN contact_type VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER';
ALTER TABLE customers ADD COLUMN category VARCHAR(100) NULL;
ALTER TABLE customers ADD COLUMN segment VARCHAR(100) NULL;
ALTER TABLE customers ADD COLUMN assigned_employee_id BIGINT NULL;

CREATE INDEX idx_customers_contact_type ON customers(contact_type);
CREATE INDEX idx_customers_category_segment ON customers(category, segment);
CREATE INDEX idx_customers_assigned_employee ON customers(assigned_employee_id);
