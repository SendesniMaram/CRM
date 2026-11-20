-- Prepared only; no historical amount/status is rewritten.
ALTER TABLE payrolls
 ADD COLUMN overtime_hours DECIMAL(8,2) NULL,
 ADD COLUMN overtime_hourly_rate DECIMAL(12,2) NULL,
 ADD COLUMN overtime_amount DECIMAL(12,2) NULL,
 ADD COLUMN cancelled BOOLEAN NOT NULL DEFAULT FALSE,
 ADD COLUMN replacement_payroll_id BIGINT NULL,
 ADD COLUMN correction_of_id BIGINT NULL,
 ADD COLUMN correction_reason VARCHAR(500) NULL,
 ADD COLUMN corrected_by BIGINT NULL,
 ADD COLUMN corrected_at DATETIME(6) NULL,
 ADD CONSTRAINT fk_payroll_replacement FOREIGN KEY (replacement_payroll_id) REFERENCES payrolls(id),
 ADD CONSTRAINT fk_payroll_correction FOREIGN KEY (correction_of_id) REFERENCES payrolls(id);
CREATE TABLE payroll_settlements (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 payroll_id BIGINT NOT NULL,
 internal_reference VARCHAR(36) NOT NULL UNIQUE,
 request_id VARCHAR(36) NOT NULL UNIQUE,
 kind VARCHAR(10) NOT NULL,
 amount DECIMAL(12,2) NOT NULL,
 paid_at DATE NOT NULL,
 method VARCHAR(50) NULL,
 transaction_reference VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL,
 reverses_id BIGINT NULL UNIQUE,
 reason VARCHAR(500) NULL,
 created_by BIGINT NOT NULL,
 created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_settlement_payroll FOREIGN KEY (payroll_id) REFERENCES payrolls(id),
 CONSTRAINT fk_settlement_reversal FOREIGN KEY (reverses_id) REFERENCES payroll_settlements(id),
 CONSTRAINT ck_settlement_amount CHECK (amount > 0),
 CONSTRAINT ck_settlement_kind CHECK (kind IN ('PAYMENT','REVERSAL')),
 CONSTRAINT uk_payroll_transaction UNIQUE (payroll_id, transaction_reference),
 INDEX idx_payroll_settlement_history (payroll_id,created_at,id)
);
