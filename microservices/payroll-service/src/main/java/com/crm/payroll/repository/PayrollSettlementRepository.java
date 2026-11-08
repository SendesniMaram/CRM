package com.crm.payroll.repository;
import com.crm.payroll.entity.PayrollSettlement;
import java.util.*;
import org.springframework.data.repository.Repository;
public interface PayrollSettlementRepository extends Repository<PayrollSettlement,Long> {
 PayrollSettlement saveAndFlush(PayrollSettlement entry);
 List<PayrollSettlement> findByPayrollIdOrderByCreatedAtAscIdAsc(Long id);
 Optional<PayrollSettlement> findByRequestId(String requestId);
 Optional<PayrollSettlement> findById(Long id);
 boolean existsByPayrollId(Long id);
 boolean existsByReversesId(Long id);
 boolean existsByPayrollIdAndTransactionReference(Long id,String reference);
}
