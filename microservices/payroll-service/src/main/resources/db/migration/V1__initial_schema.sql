-- Initial schema baseline generated from historical DDL; schema only, no data.
CREATE TABLE payrolls (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` varchar(255) NOT NULL,
  `base_salary` decimal(12,2) NOT NULL,
  `bonuses` decimal(12,2) DEFAULT NULL,
  `deductions` decimal(12,2) DEFAULT NULL,
  `tax_amount` decimal(12,2) DEFAULT NULL,
  `net_salary` decimal(12,2) NOT NULL,
  `pay_period` varchar(255) NOT NULL,
  `payment_date` date DEFAULT NULL,
  `currency` varchar(255) DEFAULT NULL,
  `status` varchar(255) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



