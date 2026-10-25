package com.crm.identity.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.crm.identity.entity.RefreshToken;
import com.crm.identity.entity.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    // Serialize consumption of the same refresh token; a rotation is single-use.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshToken token where token.token = :value")
    Optional<RefreshToken> findByTokenForUpdate(@Param("value") String token);

    @Modifying(flushAutomatically = true)
    @Query("update RefreshToken token set token.revoked = true where token.user = :user and token.revoked = false")
    int revokeAllByUser(@Param("user") User user);

    void deleteByUser(User user);
}

