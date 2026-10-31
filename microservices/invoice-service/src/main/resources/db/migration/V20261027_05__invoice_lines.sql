CREATE TABLE invoice_lines (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 invoice_id BIGINT NOT NULL,
 description VARCHAR(255) NOT NULL,
 quantity DECIMAL(12,3) NOT NULL,
 unit_price DECIMAL(12,2) NOT NULL,
 line_total DECIMAL(12,2) NOT NULL,
 INDEX idx_invoice_lines_invoice (invoice_id)
);
