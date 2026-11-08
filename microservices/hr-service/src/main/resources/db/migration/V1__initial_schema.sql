-- Initial schema baseline generated from historical DDL; schema only, no data.
CREATE TABLE attendances (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_in` time(6) DEFAULT NULL,
  `check_out` time(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `date` date NOT NULL,
  `employee_id` varchar(255) NOT NULL,
  `notes` varchar(500) DEFAULT NULL,
  `status` enum('ABSENT','HALF_DAY','LATE','LEAVE','PRESENT') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE hr_records (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contract_type` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `employee_id` varchar(255) NOT NULL,
  `employment_status` varchar(255) DEFAULT NULL,
  `hire_date` datetime(6) DEFAULT NULL,
  `job_title` varchar(255) DEFAULT NULL,
  `manager` varchar(255) DEFAULT NULL,
  `salary_grade` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `work_location` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK9gfcx56bi607k05psjns2oqqm` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE leave_employee_associations (
  `username` varchar(100) NOT NULL,
  `employee_id` varchar(50) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`username`),
  UNIQUE KEY `UK99cbvmgqw7uy7seu2jggserc3` (`employee_id`),
  UNIQUE KEY `UKm5e8t3o77mcyvdujd0cj55oi4` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE leave_requests (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `employee_id` varchar(255) NOT NULL,
  `end_date` date NOT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `start_date` date NOT NULL,
  `status` enum('APPROVED','PENDING','REJECTED') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `validator_username` varchar(100) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE trainings (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `certificate_obtained` bit(1) NOT NULL,
  `cost` double NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `employee_id` varchar(255) NOT NULL,
  `end_date` date NOT NULL,
  `provider` varchar(200) NOT NULL,
  `start_date` date NOT NULL,
  `status` enum('CANCELLED','COMPLETED','IN_PROGRESS','PLANNED') NOT NULL,
  `title` varchar(200) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



