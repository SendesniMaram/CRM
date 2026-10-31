CREATE TABLE invoice_payments (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  invoice_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  paid_at DATE NOT NULL,
  method VARCHAR(50) NULL,
  reference VARCHAR(100) NULL,
  notes VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_payment_invoice_date (invoice_id, paid_at)
);
CREATE TABLE invoice_reminders (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  invoice_id BIGINT NOT NULL,
  recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  channel VARCHAR(30) NOT NULL,
  notes VARCHAR(500) NULL,
  recorded_by VARCHAR(120) NULL,
  INDEX idx_reminder_invoice_date (invoice_id, recorded_at)
);
