package com.footpedia.repository;

import com.footpedia.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    // @Transactional here (not just on the calling service method) -- a bulk
    // @Modifying query doesn't get an implicit transaction from Spring Data
    // the way save()/delete() do, and throws TransactionRequiredException
    // without one. Matters now that requestReset() isn't @Transactional
    // itself (see PasswordResetServiceImpl).
    @Modifying
    @Transactional
    @Query("delete from PasswordResetToken t where t.userId = :userId and t.usedAt is null")
    void deleteUnusedByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("delete from PasswordResetToken t where t.userId = :userId")
    void deleteByUserId(Long userId);
}
