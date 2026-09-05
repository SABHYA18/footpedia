package com.footpedia.repository;

import com.footpedia.domain.FixtureLineup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FixtureLineupRepository extends JpaRepository<FixtureLineup, Long> {
    List<FixtureLineup> findByFixtureId(Long fixtureId);

    /**
     * Atomic upsert on (fixture_id, player_id). A JPA find-or-create here
     * (find by key, then save) still has a window where two concurrent ESPN
     * syncs for the same fixture (a real occurrence -- see
     * EspnMatchDetailPersister's javadoc) both see "not found" for a
     * genuinely new player and both try to insert, one losing to the unique
     * constraint. ON CONFLICT DO UPDATE resolves that atomically in Postgres
     * itself instead.
     */
    @Modifying
    @Query(
            value = """
            insert into fixture_lineup (fixture_id, team_id, player_id, position, jersey_number, starter, subbed_in, subbed_out, formation_place)
            values (:fixtureId, :teamId, :playerId, :position, :jerseyNumber, :starter, :subbedIn, :subbedOut, :formationPlace)
            on conflict (fixture_id, player_id) do update set
                team_id = excluded.team_id,
                position = excluded.position,
                jersey_number = excluded.jersey_number,
                starter = excluded.starter,
                subbed_in = excluded.subbed_in,
                subbed_out = excluded.subbed_out,
                formation_place = excluded.formation_place
            """,
            nativeQuery = true)
    void upsert(
            @Param("fixtureId") Long fixtureId,
            @Param("teamId") Long teamId,
            @Param("playerId") Long playerId,
            @Param("position") String position,
            @Param("jerseyNumber") Integer jerseyNumber,
            @Param("starter") boolean starter,
            @Param("subbedIn") boolean subbedIn,
            @Param("subbedOut") boolean subbedOut,
            @Param("formationPlace") Integer formationPlace);
}
