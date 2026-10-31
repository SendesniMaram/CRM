package com.crm.invoice.entity;
import jakarta.persistence.*; import lombok.Data;
@Entity @Table(name="invoice_number_sequence") @Data
public class InvoiceNumberSequence { @Id private Integer id; @Column(name="next_value", nullable=false) private Long nextValue; }
