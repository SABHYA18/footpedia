package com.footpedia.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "reminder")
@Getter
@Setter
@NoArgsConstructor
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_ref", nullable = false, length = 64)
    private String userRef;

    @Column(name = "fixture_id", nullable = false)
    private Long fixtureId;

    @Column(name = "lead_time_seconds", nullable = false)
    private Integer leadTimeSeconds;

    @Column(name = "notify_at", nullable = false)
    private Instant notifyAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ReminderStatus status;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
