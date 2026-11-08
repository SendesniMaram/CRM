package com.crm.invoice.repository;import com.crm.invoice.entity.ReminderPolicy;import org.springframework.data.jpa.repository.*;import java.util.Optional;
public interface ReminderPolicyRepository extends JpaRepository<ReminderPolicy,Long>{@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select p from ReminderPolicy p where p.id=1") Optional<ReminderPolicy> lock();}
