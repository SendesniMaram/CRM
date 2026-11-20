-- Prepared only. Existing documents remain private to administrators.
ALTER TABLE employee_documents ADD COLUMN employee_visible BOOLEAN NOT NULL DEFAULT FALSE;
