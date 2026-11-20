package com.crm.fees.repository;

import com.crm.fees.entity.Fee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface FeeRepository extends JpaRepository<Fee, Long>, JpaSpecificationExecutor<Fee> {

    interface Totals { long getCount(); java.math.BigDecimal getAmount(); Double getHours(); }
    interface StatusCount { String getStatus(); long getCount(); }
    @org.springframework.data.jpa.repository.Query("select count(f) as count, sum(f.workedHours) as hours from Fee f")
    Totals aggregateTotals();
    @org.springframework.data.jpa.repository.Query("select f.invoiceStatus as status, count(f) as count from Fee f group by f.invoiceStatus")
    java.util.List<StatusCount> countByInvoiceStatus();
    @org.springframework.data.jpa.repository.Query("select f.paymentStatus as status, count(f) as count from Fee f group by f.paymentStatus")
    java.util.List<StatusCount> countByPaymentStatus();

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select f from Fee f where f.id=:id")
    java.util.Optional<Fee> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    boolean existsByEmployeeId(String employeeId);
}
