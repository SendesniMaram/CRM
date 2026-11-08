package com.crm.fees.entity;import jakarta.persistence.*;import lombok.Data;import java.math.BigDecimal;import java.time.*;
@Entity @Table(name="fee_time_entries") @Data public class FeeTimeEntry {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="offering_id",nullable=false,updatable=false) private Long offeringId;
 @Column(name="request_id",nullable=false,length=36,unique=true,updatable=false) private String requestId;
 @Column(name="work_date",nullable=false) private LocalDate workDate;
 @Column(nullable=false,precision=8,scale=2) private BigDecimal hours;
 @Column(nullable=false,length=500) private String description;
 @Column(name="tariff_id",nullable=false) private Long tariffId;
 @Column(name="applied_rate",nullable=false,precision=12,scale=2) private BigDecimal appliedRate;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal amount;
 @Column(name="fee_id",unique=true) private Long feeId;
 @Column(nullable=false) private boolean cancelled;
 @Column(name="cancel_reason",length=500) private String cancelReason;
 @Column(name="active_signature",length=64,unique=true) private String activeSignature;
 @Column(name="created_by",nullable=false,updatable=false) private Long createdBy;
 @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
 @Column(name="updated_by",nullable=false) private Long updatedBy;
 @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
 @PrePersist void created(){createdAt=LocalDateTime.now();updatedAt=createdAt;}
 @PreUpdate void updated(){updatedAt=LocalDateTime.now();}
}