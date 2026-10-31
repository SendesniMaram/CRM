package com.crm.invoice.repository;
import com.crm.invoice.entity.InvoiceReminder; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface InvoiceReminderRepository extends JpaRepository<InvoiceReminder,Long> { List<InvoiceReminder> findByInvoiceIdOrderByRecordedAtDesc(Long invoiceId); }
