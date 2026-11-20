package com.crm.customer.repository;
import com.crm.customer.entity.CustomerAssociation;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CustomerAssociationRepository extends JpaRepository<CustomerAssociation,Long> {
 @org.springframework.data.jpa.repository.Modifying
 @org.springframework.data.jpa.repository.Query(value="INSERT INTO customer_user_associations (user_id, customer_id) VALUES (:userId, :customerId)", nativeQuery=true)
 void insertNew(@org.springframework.data.repository.query.Param("userId") Long userId, @org.springframework.data.repository.query.Param("customerId") Long customerId);
 boolean existsByCustomerId(Long customerId);
}
