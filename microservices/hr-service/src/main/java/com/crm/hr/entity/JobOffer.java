package com.crm.hr.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity @Table(name="job_offers") @Data @NoArgsConstructor
public class JobOffer {
    public enum Status { OPEN, CLOSED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=200) private String title;
    @Column(columnDefinition="TEXT") private String description;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status=Status.OPEN;
    @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
    @PrePersist void create(){createdAt=updatedAt=LocalDateTime.now();}
    @PreUpdate void update(){updatedAt=LocalDateTime.now();}
}
