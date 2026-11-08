package com.crm.customer.repository;import com.crm.customer.entity.ClientCaseEvent;import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.data.domain.*;
public interface ClientCaseEventRepository extends JpaRepository<ClientCaseEvent,Long>{Page<ClientCaseEvent> findByCaseId(long caseId,Pageable page);}
