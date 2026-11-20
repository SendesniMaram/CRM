package com.crm.invoice.entity;
import jakarta.persistence.*; import java.math.BigDecimal; import lombok.Data;
@Entity @Table(name="quote_lines") @Data
public class QuoteLine {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="quote_id",insertable=false,updatable=false) private Long quoteId;
 @Column(nullable=false,length=255) private String description;
 @Column(nullable=false,precision=12,scale=3) private BigDecimal quantity;
 @Column(name="unit_price",nullable=false,precision=12,scale=2) private BigDecimal unitPrice;
 @Column(name="line_total",nullable=false,precision=12,scale=2) private BigDecimal lineTotal;
}
