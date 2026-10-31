package com.crm.invoice.repository;
import com.crm.invoice.entity.InvoicePayment; import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.Query; import org.springframework.data.repository.query.Param; import java.math.BigDecimal; import java.util.List;
public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment,Long> {
 List<InvoicePayment> findByInvoiceIdOrderByPaidAtDescIdDesc(Long invoiceId);
 @Query("select coalesce(sum(p.amount),0) from InvoicePayment p where p.invoiceId=:invoiceId") BigDecimal sumForInvoice(@Param("invoiceId") Long invoiceId);
}
