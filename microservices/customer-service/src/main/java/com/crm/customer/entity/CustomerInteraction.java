package com.crm.customer.entity;
import jakarta.persistence.*; import java.time.LocalDateTime; import lombok.Data;
@Entity @Table(name="customer_interactions") @Data
public class CustomerInteraction {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="customer_id",nullable=false) private Long customerId;
 @Column(name="employee_id") private Long employeeId;
 @Column(name="interaction_type",nullable=false,length=20) private String interactionType;
 @Column(name="occurred_at",nullable=false) private LocalDateTime occurredAt;
 @Column(columnDefinition="TEXT") private String notes;
 @Column(name="created_at",insertable=false,updatable=false) private LocalDateTime createdAt;
}
