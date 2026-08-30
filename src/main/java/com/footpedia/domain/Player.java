package com.footpedia.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "player")
@Getter
@Setter
@NoArgsConstructor
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 32)
    private String position;

    @Column(length = 64)
    private String nationality;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "current_team_id")
    private Long currentTeamId;

    @Column(name = "photo_url", length = 512)
    private String photoUrl;
}
