package com.crm.fees.entity;import jakarta.persistence.*;import lombok.Data;
@Entity @Table(name="fee_offerings") @Data public class FeeOffering {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=255) private String title;
 @Column(name="employee_id",nullable=false,length=50,updatable=false) private String employeeId;
 @Column(name="customer_id",length=50,updatable=false) private String customerId;
 @Column(nullable=false,length=3,updatable=false) private String currency;
 @Column(nullable=false) private boolean active=true;
}