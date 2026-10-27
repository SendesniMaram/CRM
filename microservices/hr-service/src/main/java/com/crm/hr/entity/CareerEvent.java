package com.crm.hr.entity;
import jakarta.persistence.*; import lombok.Data; import lombok.NoArgsConstructor; import java.time.LocalDate; import java.time.LocalDateTime;
@Entity @Table(name="employee_career_events",indexes=@Index(name="idx_career_employee_effective",columnList="employee_id,effective_date")) @Data @NoArgsConstructor
public class CareerEvent {
 public enum Type { POSITION_CHANGE, DEPARTMENT_CHANGE, PROMOTION }
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="employee_id",nullable=false,length=50) private String employeeId;
 @Column(name="effective_date",nullable=false) private LocalDate effectiveDate;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Type type;
 @Column(name="previous_value",length=200) private String previousValue;
 @Column(name="new_value",nullable=false,length=200) private String newValue;
 @Column(length=2000) private String comment;
 @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
 @PrePersist void create(){createdAt=LocalDateTime.now();}
}
