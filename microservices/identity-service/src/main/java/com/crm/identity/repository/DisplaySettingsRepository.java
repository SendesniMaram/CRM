package com.crm.identity.repository;
import com.crm.identity.entity.DisplaySettings;import org.springframework.data.jpa.repository.*;import jakarta.persistence.LockModeType;import java.util.Optional;
public interface DisplaySettingsRepository extends JpaRepository<DisplaySettings,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from DisplaySettings s where s.id=1") Optional<DisplaySettings> lock();
}
