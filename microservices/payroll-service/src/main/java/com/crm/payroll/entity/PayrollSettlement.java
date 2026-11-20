package com.crm.payroll.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.Data;
@Entity @Table(name="payroll_settlements") @Data
public class PayrollSettlement {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="payroll_id",nullable=false,updatable=false) private Long payrollId;
 @Column(name="internal_reference",nullable=false,unique=true,length=36,updatable=false) private String internalReference;
 @Column(name="request_id",nullable=false,unique=true,length=36,updatable=false) private String requestId;
 @Column(nullable=false,length=10,updatable=false) private String kind;
 @Column(nullable=false,precision=12,scale=2,updatable=false) private BigDecimal amount;
 @Column(name="paid_at",nullable=false,updatable=false) private LocalDate paidAt;
 @Column(length=50,updatable=false) private String method;
 @Column(name="transaction_reference",length=100,updatable=false) private String transactionReference;
 @Column(name="reverses_id",unique=true,updatable=false) private Long reversesId;
 @Column(length=500,updatable=false) private String reason;
 @Column(name="created_by",nullable=false,updatable=false) private Long createdBy;
 @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
 @PrePersist void created(){if(createdAt==null)createdAt=LocalDateTime.now();}
}
