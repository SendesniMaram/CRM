package com.crm.fees.repository;import com.crm.fees.entity.FeeTimeEntry;import java.util.*;import java.time.LocalDate;import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.data.domain.*;
public interface FeeTimeRepository extends JpaRepository<FeeTimeEntry,Long> {
 @org.springframework.data.jpa.repository.Query("select distinct t.tariffId from FeeTimeEntry t where t.tariffId in :ids") List<Long> usedTariffs(@org.springframework.data.repository.query.Param("ids") List<Long> ids);
 Optional<FeeTimeEntry> findByRequestId(String requestId);boolean existsByTariffId(Long tariffId);boolean existsByActiveSignatureAndIdNot(String signature,Long id);
 Page<FeeTimeEntry> findByOfferingIdAndWorkDateBetween(Long offeringId,LocalDate from,LocalDate to,Pageable page);
}