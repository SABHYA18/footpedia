package com.footpedia.repository;

import com.footpedia.domain.FixturePlayerStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixturePlayerStatRepository extends JpaRepository<FixturePlayerStat, Long> {
    List<FixturePlayerStat> findByFixtureId(Long fixtureId);

    Optional<FixturePlayerStat> findByFixtureIdAndPlayerId(Long fixtureId, Long playerId);
}
