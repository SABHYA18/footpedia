package com.footpedia.domain;

/** Mirrors football-data.org's match status values. */
public enum FixtureStatus {
    SCHEDULED,
    TIMED,
    IN_PLAY,
    PAUSED,
    FINISHED,
    SUSPENDED,
    POSTPONED,
    CANCELLED,
    AWARDED
}
