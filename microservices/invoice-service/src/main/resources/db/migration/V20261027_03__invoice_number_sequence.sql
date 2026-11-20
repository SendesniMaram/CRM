CREATE TABLE invoice_number_sequence (
  id TINYINT NOT NULL PRIMARY KEY,
  next_value BIGINT NOT NULL
);
INSERT INTO invoice_number_sequence (id, next_value) VALUES (1, 1);
