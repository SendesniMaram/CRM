package com.crm.hr.repository;

import com.crm.hr.entity.Training;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingRepository extends JpaRepository<Training, Long>, JpaSpecificationExecutor<Training> {

    List<Training> findByEmployeeId(String employeeId);

    List<Training> findByStatus(Training.TrainingStatus status);
}
