ALTER TABLE sales_opportunities
    ADD COLUMN expected_close_date DATE NULL;

CREATE TABLE opportunity_stage_history (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    opportunity_id BIGINT NOT NULL,
    previous_stage VARCHAR(30) NULL,
    new_stage VARCHAR(30) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_opportunity_stage_history_opportunity
        FOREIGN KEY (opportunity_id) REFERENCES sales_opportunities(id) ON DELETE CASCADE,
    INDEX idx_opportunity_stage_history (opportunity_id, changed_at)
);
