package com.crm.invoice.repository;

import com.crm.invoice.entity.Quote;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuoteRepository extends JpaRepository<Quote, Long>, JpaSpecificationExecutor<Quote> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quote q left join fetch q.lines where q.id=:id")
    Optional<Quote> lockWithLines(@Param("id") Long id);

    @EntityGraph(attributePaths = "lines")
    Optional<Quote> findWithLinesById(Long id);

    @EntityGraph(attributePaths = "lines")
    List<Quote> findWithLinesByIdIn(Collection<Long> ids);
}
