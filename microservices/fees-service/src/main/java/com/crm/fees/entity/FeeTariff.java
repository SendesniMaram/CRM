package com.crm.fees.entity;import jakarta.persistence.*;import lombok.Data;import java.math.BigDecimal;import java.time.LocalDate;
@Entity @Table(name="fee_tariffs") @Data public class FeeTariff {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="offering_id",nullable=false,updatable=false) private Long offeringId;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal rate;
 @Column(name="valid_from",nullable=false) private LocalDate validFrom;
 @Column(name="valid_to") private LocalDate validTo;
 @Column(nullable=false) private boolean active=true;
}