package com.footpedia.repository;

import com.footpedia.domain.FixtureLineup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FixtureLineupRepository extends JpaRepository<FixtureLineup, Long> {
    List<FixtureLineup> findByFixtureId(Long fixtureId);

    void deleteByFixtureId(Long fixtureId);
}
