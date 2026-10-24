-- Fresh isolated demo database only. Never use as a migration for an existing database.
USE customer_db;
CREATE TABLE customers (
 id BIGINT NOT NULL AUTO_INCREMENT,
 name VARCHAR(255) NOT NULL,
 email VARCHAR(255),
 phone VARCHAR(255),
 company VARCHAR(255),
 address VARCHAR(500),
 created_at DATETIME(6),
 updated_at DATETIME(6),
 PRIMARY KEY (id),
 UNIQUE KEY uk_demo_customer_email (email),
 UNIQUE KEY uk_demo_customer_phone (phone)
) ENGINE=InnoDB;
