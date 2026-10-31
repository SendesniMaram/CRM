package com.crm.customer.repository;
import com.crm.customer.entity.CustomerInteraction; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface CustomerInteractionRepository extends JpaRepository<CustomerInteraction,Long> { List<CustomerInteraction> findByCustomerIdOrderByOccurredAtDesc(Long customerId); }
