package com.footpedia.ingestion;

import com.footpedia.domain.Competition;
import com.footpedia.domain.Fixture;
import com.footpedia.domain.FixtureStatus;
import com.footpedia.domain.Standing;
import com.footpedia.domain.Team;
import com.footpedia.providers.footballdata.FootballDataClient;
import com.footpedia.providers.footballdata.dto.CompetitionDto;
import com.footpedia.providers.footballdata.dto.MatchDto;
import com.footpedia.providers.footballdata.dto.StandingsResponse;
import com.footpedia.providers.footballdata.dto.TeamDto;
import com.footpedia.repository.CompetitionRepository;
import com.footpedia.repository.FixtureRepository;
import com.footpedia.repository.StandingRepository;
import com.footpedia.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Syncs football-data.org into the local schema. Every external ID is
 * translated through {@link ExternalRefResolver} so provider IDs never leak
 * past the ingestion layer.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FootballDataIngestionService {

    private static final String PROVIDER = "football-data";

    private final FootballDataClient client;
    private final ExternalRefResolver externalRefs;
    private final CompetitionRepository competitionRepository;
    private final TeamRepository teamRepository;
    private final FixtureRepository fixtureRepository;
    private final StandingRepository standingRepository;

    @Transactional
    public Competition syncCompetition(String competitionCode) {
        CompetitionDto dto = client.getCompetition(competitionCode);

        Competition competition = competitionRepository.findByCode(competitionCode).orElseGet(Competition::new);
        competition.setCode(dto.code());
        competition.setName(dto.name());
        competition.setCountry(dto.area() == null ? null : dto.area().name());
        competition.setEmblemUrl(dto.emblem());
        if (dto.currentSeason() != null) {
            competition.setCurrentSeason(seasonLabel(dto.currentSeason().startDate()));
        }
        competition = competitionRepository.save(competition);

        externalRefs.link("COMPETITION", PROVIDER, String.valueOf(dto.id()), competition.getId());
        log.info("Synced competition {} ({})", competition.getName(), competition.getCode());
        return competition;
    }

    @Transactional
    public void syncTeams(Competition competition, String competitionCode) {
        for (TeamDto dto : client.getTeams(competitionCode)) {
            Long internalId = externalRefs.resolve("TEAM", PROVIDER, String.valueOf(dto.id())).orElse(null);
            Team team = internalId == null ? new Team() : teamRepository.findById(internalId).orElseGet(Team::new);

            team.setName(dto.name());
            team.setShortName(dto.shortName());
            team.setTla(dto.tla());
            team.setCrestUrl(dto.crest());
            team.setVenue(dto.venue());
            team.setFounded(dto.founded());
            team = teamRepository.save(team);

            externalRefs.link("TEAM", PROVIDER, String.valueOf(dto.id()), team.getId());
        }
        log.info("Synced teams for competition {}", competitionCode);
    }

    @Transactional
    public void syncFixtures(Competition competition, String competitionCode) {
        for (MatchDto dto : client.getMatches(competitionCode)) {
            Long homeTeamId = requireTeam(dto.homeTeam().id());
            Long awayTeamId = requireTeam(dto.awayTeam().id());

            Long fixtureId = externalRefs.resolve("FIXTURE", PROVIDER, String.valueOf(dto.id())).orElse(null);
            Fixture fixture = fixtureId == null
                    ? new Fixture()
                    : fixtureRepository.findById(fixtureId).orElseGet(Fixture::new);

            fixture.setCompetitionId(competition.getId());
            fixture.setSeason(competition.getCurrentSeason());
            fixture.setMatchday(dto.matchday());
            fixture.setUtcKickoff(OffsetDateTime.parse(dto.utcDate()).toInstant());
            fixture.setStatus(FixtureStatus.valueOf(dto.status()));
            fixture.setHomeTeamId(homeTeamId);
            fixture.setAwayTeamId(awayTeamId);
            if (dto.score() != null && dto.score().fullTime() != null) {
                fixture.setHomeScore(dto.score().fullTime().home());
                fixture.setAwayScore(dto.score().fullTime().away());
            }
            fixture.setUpdatedAt(Instant.now());
            fixture = fixtureRepository.save(fixture);

            externalRefs.link("FIXTURE", PROVIDER, String.valueOf(dto.id()), fixture.getId());
        }
        log.info("Synced fixtures for competition {}", competitionCode);
    }

    @Transactional
    public void syncStandings(Competition competition, String competitionCode) {
        for (StandingsResponse.TableRowDto row : client.getStandings(competitionCode)) {
            Long teamId = requireTeam(row.team().id());

            Standing standing = standingRepository
                    .findByCompetitionIdAndSeasonAndTeamId(competition.getId(), competition.getCurrentSeason(), teamId)
                    .orElseGet(Standing::new);

            standing.setCompetitionId(competition.getId());
            standing.setSeason(competition.getCurrentSeason());
            standing.setTeamId(teamId);
            standing.setPosition(row.position());
            standing.setPlayed(row.playedGames());
            standing.setWon(row.won());
            standing.setDraw(row.draw());
            standing.setLost(row.lost());
            standing.setPoints(row.points());
            standing.setGoalsFor(row.goalsFor());
            standing.setGoalsAgainst(row.goalsAgainst());
            standing.setForm(row.form());
            standing.setUpdatedAt(Instant.now());

            standingRepository.save(standing);
        }
        log.info("Synced standings for competition {}", competitionCode);
    }

    private Long requireTeam(long externalTeamId) {
        return externalRefs.resolve("TEAM", PROVIDER, String.valueOf(externalTeamId))
                .orElseThrow(() -> new IllegalStateException(
                        "Unknown team external_id=%d — run team sync before fixtures/standings"
                                .formatted(externalTeamId)));
    }

    /** football-data.org identifies a season by its start year, e.g. "2025" for 2025/26. */
    private String seasonLabel(String startDate) {
        return startDate.substring(0, 4);
    }
}
