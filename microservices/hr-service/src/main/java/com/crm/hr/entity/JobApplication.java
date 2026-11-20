package com.crm.hr.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity @Table(name="job_applications") @Data @NoArgsConstructor
public class JobApplication {
    public enum Status { RECEIVED, IN_REVIEW, ACCEPTED, REJECTED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="offer_id",nullable=false) private Long offerId;
    @Column(name="candidate_name",nullable=false,length=200) private String candidateName;
    @Column(name="candidate_email",nullable=false,length=254) private String candidateEmail;
    @Column(columnDefinition="TEXT") private String notes;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status=Status.RECEIVED;
    @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
    @PrePersist void create(){createdAt=updatedAt=LocalDateTime.now();}
    @PreUpdate void update(){updatedAt=LocalDateTime.now();}
}
