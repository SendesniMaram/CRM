-- Additive only. Historical DOUBLE hours, amounts and declarative statuses are not rewritten.
ALTER TABLE fees ADD COLUMN exact_hours DECIMAL(8,2) NULL, ADD COLUMN currency VARCHAR(3) NULL,
 ADD COLUMN offering_id BIGINT NULL, ADD COLUMN source_time_id BIGINT NULL,
 ADD CONSTRAINT uk_fee_source_time UNIQUE (source_time_id), ADD INDEX idx_fee_report_period (work_date,currency);
CREATE TABLE fee_offerings (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,title VARCHAR(255) NOT NULL,employee_id VARCHAR(50) NOT NULL,customer_id VARCHAR(50) NULL,currency VARCHAR(3) NOT NULL,active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE fee_tariffs (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,offering_id BIGINT NOT NULL,rate DECIMAL(12,2) NOT NULL,valid_from DATE NOT NULL,valid_to DATE NULL,active BOOLEAN NOT NULL DEFAULT TRUE,
 CONSTRAINT fk_fee_tariff_offering FOREIGN KEY (offering_id) REFERENCES fee_offerings(id),
 CONSTRAINT ck_fee_tariff_rate CHECK (rate>0),CONSTRAINT ck_fee_tariff_period CHECK (valid_to IS NULL OR valid_to>=valid_from),
 INDEX idx_fee_tariff_validity (offering_id,active,valid_from,valid_to)
);
CREATE TABLE fee_time_entries (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,offering_id BIGINT NOT NULL,request_id VARCHAR(36) NOT NULL UNIQUE,work_date DATE NOT NULL,hours DECIMAL(8,2) NOT NULL,description VARCHAR(500) NOT NULL,
 tariff_id BIGINT NOT NULL,applied_rate DECIMAL(12,2) NOT NULL,amount DECIMAL(12,2) NOT NULL,fee_id BIGINT NULL UNIQUE,
 cancelled BOOLEAN NOT NULL DEFAULT FALSE,cancel_reason VARCHAR(500) NULL,active_signature VARCHAR(64) NULL UNIQUE,
 created_by BIGINT NOT NULL,created_at DATETIME(6) NOT NULL,updated_by BIGINT NOT NULL,updated_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_fee_time_offering FOREIGN KEY (offering_id) REFERENCES fee_offerings(id),
 CONSTRAINT fk_fee_time_tariff FOREIGN KEY (tariff_id) REFERENCES fee_tariffs(id),
 CONSTRAINT fk_fee_time_fee FOREIGN KEY (fee_id) REFERENCES fees(id),
 CONSTRAINT ck_fee_time_hours CHECK (hours>0),CONSTRAINT ck_fee_time_amount CHECK (amount>0 AND applied_rate>0),
 INDEX idx_fee_time_period (offering_id,work_date),INDEX idx_fee_time_reporting (work_date,cancelled,fee_id)
);
ALTER TABLE fees ADD CONSTRAINT fk_fee_offering FOREIGN KEY (offering_id) REFERENCES fee_offerings(id),
 ADD CONSTRAINT fk_fee_source_time FOREIGN KEY (source_time_id) REFERENCES fee_time_entries(id);
