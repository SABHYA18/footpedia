package com.footpedia.repository;

import com.footpedia.domain.PasswordChangeCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PasswordChangeCodeRepository extends JpaRepository<PasswordChangeCode, Long> {
    Optional<PasswordChangeCode> findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("delete from PasswordChangeCode c where c.userId = :userId and c.usedAt is null")
    void deleteUnusedByUserId(Long userId);

    @Modifying
    @Query("delete from PasswordChangeCode c where c.userId = :userId")
    void deleteByUserId(Long userId);
}
