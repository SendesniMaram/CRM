package com.crm.invoice.entity;
import jakarta.persistence.*; import java.time.LocalDateTime; import lombok.Data;
@Entity @Table(name="invoice_reminders") @Data
public class InvoiceReminder {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="invoice_id", nullable=false) private Long invoiceId;
 @Column(name="recorded_at", insertable=false, updatable=false) private LocalDateTime recordedAt;
 @Column(nullable=false, length=30) private String channel;
 @Column(length=500) private String notes;
 @Column(name="recorded_by", length=120) private String recordedBy;
}
