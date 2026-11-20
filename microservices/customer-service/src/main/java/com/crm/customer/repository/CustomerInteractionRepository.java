package com.crm.customer.repository;
import com.crm.customer.entity.CustomerInteraction; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface CustomerInteractionRepository extends JpaRepository<CustomerInteraction,Long> { org.springframework.data.domain.Page<CustomerInteraction> findByCustomerIdAndEmployeeIdAndEmployeeVisibleTrue(Long customerId,Long employeeId,org.springframework.data.domain.Pageable page); List<CustomerInteraction> findByCustomerIdOrderByOccurredAtDesc(Long customerId); }
