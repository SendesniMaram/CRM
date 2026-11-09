package com.crm.identity.repository;
import com.crm.identity.entity.AdministrationEvent;
import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.Repository;import org.springframework.data.domain.*;import org.springframework.data.repository.query.Param;import java.time.Instant;import java.util.Optional;
public interface AdministrationEventRepository extends Repository<AdministrationEvent,Long> {
 AdministrationEvent save(AdministrationEvent event);
 Optional<AdministrationEvent> findById(Long id);
 @Query("select e from AdministrationEvent e where e.occurredAt>=:from and e.occurredAt<:to and (:action is null or e.action=:action) and (:target is null or e.targetId=:target) and (:actor is null or e.actorId=:actor)")
 Page<AdministrationEvent> search(@Param("from") Instant from,@Param("to") Instant to,@Param("action") String action,@Param("target") Long target,@Param("actor") Long actor,Pageable page);
}
