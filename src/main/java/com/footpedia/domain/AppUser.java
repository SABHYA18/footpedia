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
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Overrides footpedia.reminders.default-lead-time for this user. Null = use the system default. */
    @Column(name = "default_lead_time_minutes")
    private Integer defaultLeadTimeMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_channel", nullable = false, length = 16)
    private NotificationChannel notificationChannel = NotificationChannel.EMAIL;

    @Column(name = "telegram_handle", length = 64)
    private String telegramHandle;
}
