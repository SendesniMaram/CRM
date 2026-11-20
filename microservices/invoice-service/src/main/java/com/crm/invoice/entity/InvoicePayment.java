package com.crm.invoice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Entity @Table(name="invoice_payments") @Data
public class InvoicePayment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="invoice_id", nullable=false) private Long invoiceId;
 @Column(nullable=false, precision=12, scale=2) private BigDecimal amount;
 @Column(name="paid_at", nullable=false) private LocalDate paidAt;
 @Column(length=50) private String method;
 @Column(length=100) private String reference;
 @Column(length=500) private String notes;
 @Column(name="created_at", insertable=false, updatable=false) private LocalDateTime createdAt;
}
