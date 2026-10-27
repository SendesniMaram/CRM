CREATE TABLE leave_quotas (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 employee_id VARCHAR(50) NOT NULL,
 period_start DATE NOT NULL,
 period_end DATE NOT NULL,
 leave_type VARCHAR(50) NOT NULL,
 granted_days DECIMAL(7,2) NOT NULL,
 CONSTRAINT uk_leave_quota_employee_period_type UNIQUE (employee_id, period_start, period_end, leave_type),
 INDEX idx_leave_quota_employee_period (employee_id, period_start, period_end)
);
