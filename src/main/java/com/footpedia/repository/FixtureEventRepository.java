package com.footpedia.repository;

import com.footpedia.domain.FixtureEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FixtureEventRepository extends JpaRepository<FixtureEvent, Long> {
    List<FixtureEvent> findByFixtureIdOrderByMinuteAsc(Long fixtureId);

    void deleteByFixtureId(Long fixtureId);
}
