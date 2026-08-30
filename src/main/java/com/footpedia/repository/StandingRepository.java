package com.footpedia.repository;

import com.footpedia.domain.Standing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StandingRepository extends JpaRepository<Standing, Long> {
    Optional<Standing> findByCompetitionIdAndSeasonAndTeamId(Long competitionId, String season, Long teamId);

    List<Standing> findByCompetitionIdAndSeasonOrderByPositionAsc(Long competitionId, String season);
}
