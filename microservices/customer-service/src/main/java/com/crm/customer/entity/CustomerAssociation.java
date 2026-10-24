package com.crm.customer.entity;
import jakarta.persistence.*;
@Entity @Table(name="customer_user_associations", uniqueConstraints=@UniqueConstraint(name="uk_customer_association_customer", columnNames="customer_id"))
public class CustomerAssociation {
 @Id @Column(name="user_id") private Long userId;
 @Column(name="customer_id", nullable=false) private Long customerId;
 protected CustomerAssociation() {}
 public CustomerAssociation(Long userId, Long customerId) { this.userId=userId; this.customerId=customerId; }
 public Long getUserId() { return userId; }
 public Long getCustomerId() { return customerId; }
}
