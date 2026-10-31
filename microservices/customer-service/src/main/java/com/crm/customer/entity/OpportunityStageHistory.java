package com.crm.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "opportunity_stage_history")
@Data
public class OpportunityStageHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "opportunity_id", nullable = false)
    private Long opportunityId;

    @Column(name = "previous_stage", length = 30)
    private String previousStage;

    @Column(name = "new_stage", nullable = false, length = 30)
    private String newStage;

    @Column(name = "changed_at", insertable = false, updatable = false)
    private LocalDateTime changedAt;
}
