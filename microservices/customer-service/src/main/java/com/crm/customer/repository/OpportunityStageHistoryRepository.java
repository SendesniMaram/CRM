package com.crm.customer.repository;

import com.crm.customer.entity.OpportunityStageHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpportunityStageHistoryRepository extends JpaRepository<OpportunityStageHistory, Long> {
    List<OpportunityStageHistory> findByOpportunityIdOrderByChangedAtDescIdDesc(Long opportunityId);
    void deleteByOpportunityId(Long opportunityId);
}
