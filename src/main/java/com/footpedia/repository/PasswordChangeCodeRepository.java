package com.footpedia.repository;

import com.footpedia.domain.PasswordChangeCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface PasswordChangeCodeRepository extends JpaRepository<PasswordChangeCode, Long> {
    Optional<PasswordChangeCode> findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(Long userId);

    // @Transactional here (not just on the calling service method) -- a bulk
    // @Modifying query, unlike the inherited save()/delete() methods, doesn't
    // get an implicit transaction from Spring Data on its own and throws
    // TransactionRequiredException without one. Annotating it here means it
    // works whether or not the caller happens to be transactional (join if
    // one's already open, via the default REQUIRED propagation).
    @Modifying
    @Transactional
    @Query("delete from PasswordChangeCode c where c.userId = :userId and c.usedAt is null")
    void deleteUnusedByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("delete from PasswordChangeCode c where c.userId = :userId")
    void deleteByUserId(Long userId);
}
