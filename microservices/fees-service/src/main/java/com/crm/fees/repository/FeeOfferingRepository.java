package com.crm.fees.repository;import com.crm.fees.entity.FeeOffering;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import java.util.*;
public interface FeeOfferingRepository extends JpaRepository<FeeOffering,Long> {
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select o from FeeOffering o where o.id=:id") Optional<FeeOffering> lock(@Param("id") Long id);
 org.springframework.data.domain.Page<FeeOffering> findByTitleContainingIgnoreCase(String title,org.springframework.data.domain.Pageable page);
}