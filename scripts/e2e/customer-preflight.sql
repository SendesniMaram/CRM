-- READ ONLY. Run only on the explicitly approved Customer database.
-- MySQL >= 8.0.16 is required to enforce CHECK constraints.
SELECT DATABASE() AS selected_database, VERSION() AS mysql_version;
SELECT @@default_storage_engine AS default_engine, @@foreign_key_checks AS foreign_key_checks;
SELECT TABLE_NAME, ENGINE FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('customers', 'customer_user_associations');
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_KEY
 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
 AND (TABLE_NAME = 'customer_user_associations' OR (TABLE_NAME = 'customers' AND COLUMN_NAME = 'id'));
SELECT TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE
 FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE()
 AND TABLE_NAME IN ('customers', 'customer_user_associations');
SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
 FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE()
 AND TABLE_NAME = 'customer_user_associations';
