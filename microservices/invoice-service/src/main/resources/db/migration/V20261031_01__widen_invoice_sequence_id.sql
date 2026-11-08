-- The entity uses a Java Integer, which maps to SQL INT rather than TINYINT.
ALTER TABLE invoice_number_sequence MODIFY COLUMN id INT NOT NULL;
