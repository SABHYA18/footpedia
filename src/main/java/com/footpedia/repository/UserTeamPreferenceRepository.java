package com.footpedia.repository;

import com.footpedia.domain.UserTeamPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserTeamPreferenceRepository extends JpaRepository<UserTeamPreference, Long> {
    List<UserTeamPreference> findByUserId(Long userId);

    @Modifying
    @Query("delete from UserTeamPreference p where p.userId = :userId")
    void deleteByUserId(Long userId);
}
