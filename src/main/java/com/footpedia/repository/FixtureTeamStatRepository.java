package com.footpedia.repository;

import com.footpedia.domain.FixtureTeamStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixtureTeamStatRepository extends JpaRepository<FixtureTeamStat, Long> {
    List<FixtureTeamStat> findByFixtureId(Long fixtureId);

    Optional<FixtureTeamStat> findByFixtureIdAndTeamIdAndStatType(Long fixtureId, Long teamId, String statType);
}
