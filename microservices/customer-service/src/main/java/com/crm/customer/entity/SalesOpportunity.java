package com.crm.customer.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
@Entity @Table(name="sales_opportunities") @Data
public class SalesOpportunity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="customer_id",nullable=false) private Long customerId;
 @Column(name="employee_id") private Long employeeId;
 @Column(nullable=false,length=200) private String title;
 @Column(length=1000) private String description;
 @Column(name="estimated_amount",precision=12,scale=2) private BigDecimal estimatedAmount;
 @Column(nullable=false,length=30) private String stage="NEW";
 @Column(name="expected_close_date") private LocalDate expectedCloseDate;
 @Column(name="created_at",insertable=false,updatable=false) private LocalDateTime createdAt;
 @Column(name="updated_at",insertable=false,updatable=false) private LocalDateTime updatedAt;
}
