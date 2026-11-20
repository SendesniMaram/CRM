CREATE TABLE employee_evaluations (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    criteria TEXT NULL,
    comments TEXT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_employee_evaluations_employee (employee_id, period_start)
);

CREATE TABLE job_offers (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE job_applications (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    offer_id BIGINT NOT NULL,
    candidate_name VARCHAR(200) NOT NULL,
    candidate_email VARCHAR(254) NOT NULL,
    notes TEXT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_application_offer FOREIGN KEY (offer_id) REFERENCES job_offers(id),
    INDEX idx_job_applications_offer (offer_id, status)
);
