package com.crm.customer.repository;
import com.crm.customer.entity.CustomerAssociation;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CustomerAssociationRepository extends JpaRepository<CustomerAssociation,Long> {
 boolean existsByCustomerId(Long customerId);
}
