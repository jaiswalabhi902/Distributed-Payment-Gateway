package com.microservices.auth.repository;

import com.microservices.auth.entity.RefreshToken;
import com.microservices.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.user = :user and r.revoked = false")
    void revokeAllForUser(User user);

    @Modifying
    @Query("delete from RefreshToken r where r.expiryDate < :now")
    void deleteExpired(Instant now);
}
