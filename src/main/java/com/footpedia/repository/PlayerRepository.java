package com.footpedia.repository;

import com.footpedia.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> findByCurrentTeamIdOrderByNameAsc(Long currentTeamId);
}
