package com.crm.hr.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity @Table(name="employee_documents", indexes=@Index(name="idx_employee_documents_employee", columnList="employee_id,created_at")) @Data @NoArgsConstructor
public class EmployeeDocument {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="employee_id",nullable=false,length=50) private String employeeId;
    @Column(nullable=false,length=255) private String name;
    @Column(name="document_type",nullable=false,length=30) private String type;
    @Column(name="media_type",nullable=false,length=100) private String mediaType;
    @Column(name="byte_size",nullable=false) private long size;
    @JsonIgnore @Column(name="storage_key",nullable=false,length=80,unique=true) private String storageKey;
    @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    @PrePersist void create(){createdAt=LocalDateTime.now();}
}
