package com.crm.invoice.repository;

import com.crm.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select i from Invoice i where i.id=:id")
    java.util.Optional<Invoice> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    org.springframework.data.domain.Page<Invoice> findByCustomerId(String customerId, org.springframework.data.domain.Pageable page);
    java.util.Optional<Invoice> findByIdAndCustomerId(Long id, String customerId);

    boolean existsByInvoiceNumber(String invoiceNumber);
}
