package com.crm.invoice.repository;
import com.crm.invoice.entity.InvoicePayment; import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.Query; import org.springframework.data.repository.query.Param; import java.math.BigDecimal; import java.util.List;
public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment,Long> {
 interface InvoicePaidSummary { Long getInvoiceId(); BigDecimal getAmount(); }
 @Query("select p.invoiceId as invoiceId, coalesce(sum(p.amount),0) as amount from InvoicePayment p group by p.invoiceId") List<InvoicePaidSummary> summarizePaidByInvoice();
 interface CurrencyPaidSummary { String getCurrency(); BigDecimal getAmount(); }
 @Query("select i.currency as currency, coalesce(sum(p.amount),0) as amount from InvoicePayment p join Invoice i on i.id=p.invoiceId group by i.currency") List<CurrencyPaidSummary> summarizePaidByCurrency();
 @Query("select i.currency as currency, coalesce(sum(p.amount),0) as amount from InvoicePayment p join Invoice i on i.id=p.invoiceId where i.customerId=:customerId group by i.currency") List<CurrencyPaidSummary> summarizePaidByCustomerCurrency(@Param("customerId") String customerId);
 List<InvoicePayment> findByInvoiceIdOrderByPaidAtDescIdDesc(Long invoiceId);
 @Query("select coalesce(sum(p.amount),0) from InvoicePayment p where p.invoiceId=:invoiceId") BigDecimal sumForInvoice(@Param("invoiceId") Long invoiceId);
}
