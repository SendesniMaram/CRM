package com.crm.invoice.repository;
import com.crm.invoice.entity.InvoiceNumberSequence; import jakarta.persistence.LockModeType; import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.Lock; import org.springframework.data.jpa.repository.Query;
public interface InvoiceNumberSequenceRepository extends JpaRepository<InvoiceNumberSequence,Integer> { @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from InvoiceNumberSequence s where s.id=1") InvoiceNumberSequence lockSequence(); }
