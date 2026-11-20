CREATE TABLE employee_contracts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    contract_type VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    status VARCHAR(20) NOT NULL,
    parent_contract_id BIGINT NULL,
    notes VARCHAR(2000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_employee_contract_parent FOREIGN KEY (parent_contract_id)
        REFERENCES employee_contracts(id),
    INDEX idx_employee_contracts_employee (employee_id, start_date),
    INDEX idx_employee_contracts_parent (parent_contract_id)
);
