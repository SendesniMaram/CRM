package com.crm.invoice.entity;import jakarta.persistence.*;import lombok.Data;
@Entity @Table(name="invoice_fee_claims") @Data public class InvoiceFeeClaim {@Id @Column(name="fee_id") private Long feeId;@Column(name="job_id",length=36) private String jobId;@Column(name="invoice_id") private Long invoiceId;@Column(name="line_id") private Long lineId;}
