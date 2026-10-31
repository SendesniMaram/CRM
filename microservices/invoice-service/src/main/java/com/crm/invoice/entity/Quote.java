package com.crm.invoice.entity;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalDateTime; import java.util.ArrayList; import java.util.List; import lombok.Data;
@Entity @Table(name="quotes") @Data
public class Quote {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="quote_number",nullable=false,unique=true,length=50) private String quoteNumber;
 @Column(name="customer_id",nullable=false,length=50) private String customerId;
 @Column(name="issue_date") private LocalDate issueDate;
 @Column(name="expiry_date") private LocalDate expiryDate;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal subtotal;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal tax=BigDecimal.ZERO;
 @Column(name="total_amount",nullable=false,precision=12,scale=2) private BigDecimal totalAmount;
 @Column(length=10) private String currency;
 @Column(nullable=false,length=20) private String status="DRAFT";
 @Column(length=500) private String notes;
 @Column(name="converted_invoice_id",unique=true) private Long convertedInvoiceId;
 @OneToMany(cascade=CascadeType.ALL,orphanRemoval=true) @JoinColumn(name="quote_id") private List<QuoteLine> lines=new ArrayList<>();
 @Column(name="created_at",insertable=false,updatable=false) private LocalDateTime createdAt;
 @Column(name="updated_at",insertable=false,updatable=false) private LocalDateTime updatedAt;
}
