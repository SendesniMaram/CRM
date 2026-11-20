package com.crm.customer.repository;

import com.crm.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Customer c where c.id=:id")
    java.util.Optional<Customer> lockById(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByName(String name);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
    long countByContactType(String contactType);
}

