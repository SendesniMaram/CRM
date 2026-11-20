-- Initial schema baseline generated from historical DDL; schema only, no data.
CREATE TABLE invoices (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `invoice_number` varchar(255) NOT NULL,
  `customer_id` varchar(255) NOT NULL,
  `issue_date` date DEFAULT NULL,
  `due_date` date DEFAULT NULL,
  `subtotal` decimal(12,2) NOT NULL,
  `tax` decimal(12,2) DEFAULT NULL,
  `total_amount` decimal(12,2) NOT NULL,
  `currency` varchar(255) DEFAULT NULL,
  `status` varchar(255) NOT NULL,
  `notes` text,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invoices_invoice_number` (`invoice_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



