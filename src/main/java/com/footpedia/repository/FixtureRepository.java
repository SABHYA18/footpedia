package com.footpedia.repository;

import com.footpedia.domain.Fixture;
import com.footpedia.domain.FixtureStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FixtureRepository extends JpaRepository<Fixture, Long> {
    Optional<Fixture> findByCompetitionIdAndSeasonAndHomeTeamIdAndAwayTeamIdAndUtcKickoff(
            Long competitionId, String season, Long homeTeamId, Long awayTeamId, Instant utcKickoff);

    List<Fixture> findByCompetitionIdAndSeasonOrderByUtcKickoffAsc(Long competitionId, String season);

    List<Fixture> findByCompetitionIdAndSeasonAndMatchdayOrderByUtcKickoffAsc(
            Long competitionId, String season, Integer matchday);

    List<Fixture> findByCompetitionIdAndSeasonAndStatusInOrderByUtcKickoffAsc(
            Long competitionId, String season, List<FixtureStatus> statuses);

    List<Fixture> findByCompetitionIdAndSeasonAndUtcKickoffBetweenOrderByUtcKickoffAsc(
            Long competitionId, String season, Instant from, Instant to);

    List<Fixture> findByCompetitionIdAndHomeTeamIdAndAwayTeamIdAndUtcKickoffBetween(
            Long competitionId, Long homeTeamId, Long awayTeamId, Instant from, Instant to);

    List<Fixture> findByCompetitionIdAndSeasonAndStatusAndDetailFetchedAtIsNull(
            Long competitionId, String season, FixtureStatus status);

    @Query("select f from Fixture f where (f.homeTeamId in :teamIds or f.awayTeamId in :teamIds) "
            + "and f.utcKickoff >= :from order by f.utcKickoff asc")
    List<Fixture> findUpcomingForTeams(@Param("teamIds") List<Long> teamIds, @Param("from") Instant from, Pageable pageable);

    @Query("select max(f.matchday) from Fixture f where f.competitionId = :competitionId and f.season = :season")
    Optional<Integer> findMaxMatchday(@Param("competitionId") Long competitionId, @Param("season") String season);
}
