package com.crm.hr.repository;
import com.crm.hr.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface JobApplicationRepository extends JpaRepository<JobApplication,Long>, JpaSpecificationExecutor<JobApplication> {}
