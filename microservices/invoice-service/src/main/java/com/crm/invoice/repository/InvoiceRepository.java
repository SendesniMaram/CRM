package com.crm.invoice.repository;

import com.crm.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {
    org.springframework.data.domain.Page<Invoice> findByCustomerId(String customerId, org.springframework.data.domain.Pageable page);
    java.util.Optional<Invoice> findByIdAndCustomerId(Long id, String customerId);

    boolean existsByInvoiceNumber(String invoiceNumber);
}
