package com.footpedia.repository;

import com.footpedia.domain.UserLeaguePreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserLeaguePreferenceRepository extends JpaRepository<UserLeaguePreference, Long> {
    List<UserLeaguePreference> findByUserId(Long userId);

    @Modifying
    @Query("delete from UserLeaguePreference p where p.userId = :userId")
    void deleteByUserId(Long userId);
}
