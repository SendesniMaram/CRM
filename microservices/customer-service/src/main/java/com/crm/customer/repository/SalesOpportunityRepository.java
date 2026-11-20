package com.crm.customer.repository;
import com.crm.customer.entity.SalesOpportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;

public interface SalesOpportunityRepository extends JpaRepository<SalesOpportunity, Long>,
        JpaSpecificationExecutor<SalesOpportunity> {
    interface StageSummary { String getStage(); long getCount(); BigDecimal getAmount(); }
    @Query("select o.stage as stage, count(o) as count, sum(coalesce(o.estimatedAmount, 0)) as amount from SalesOpportunity o group by o.stage")
    List<StageSummary> summarizeStages();
}
