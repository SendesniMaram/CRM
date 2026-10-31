package com.crm.customer.repository;
import com.crm.customer.entity.SalesOpportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SalesOpportunityRepository extends JpaRepository<SalesOpportunity, Long>,
        JpaSpecificationExecutor<SalesOpportunity> { }
