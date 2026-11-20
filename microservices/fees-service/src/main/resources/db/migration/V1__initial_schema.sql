-- Initial schema baseline generated from historical DDL; schema only, no data.
CREATE TABLE fees (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` varchar(255) NOT NULL,
  `customer_id` varchar(255) DEFAULT NULL,
  `service_name` varchar(255) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `hourly_rate` decimal(12,2) NOT NULL,
  `worked_hours` double NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `invoice_status` varchar(255) DEFAULT NULL,
  `payment_status` varchar(255) DEFAULT NULL,
  `work_date` date DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



