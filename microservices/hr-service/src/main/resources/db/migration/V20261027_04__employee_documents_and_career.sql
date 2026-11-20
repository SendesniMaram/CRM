CREATE TABLE employee_documents (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 employee_id VARCHAR(50) NOT NULL,
 name VARCHAR(255) NOT NULL,
 document_type VARCHAR(30) NOT NULL,
 media_type VARCHAR(100) NOT NULL,
 byte_size BIGINT NOT NULL,
 storage_key VARCHAR(80) NOT NULL UNIQUE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 INDEX idx_employee_documents_employee (employee_id, created_at)
);
CREATE TABLE employee_career_events (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 employee_id VARCHAR(50) NOT NULL,
 effective_date DATE NOT NULL,
 type VARCHAR(30) NOT NULL,
 previous_value VARCHAR(200) NULL,
 new_value VARCHAR(200) NOT NULL,
 comment VARCHAR(2000) NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 INDEX idx_career_employee_effective (employee_id, effective_date)
);
