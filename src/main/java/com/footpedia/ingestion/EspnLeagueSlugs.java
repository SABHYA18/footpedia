package com.footpedia.ingestion;

import java.util.Map;

/** Maps our internal (football-data.org-shaped) competition codes onto ESPN's own league slugs. */
public final class EspnLeagueSlugs {

    private static final Map<String, String> SLUGS = Map.of(
            "PL", "eng.1",
            "PD", "esp.1",
            "SA", "ita.1",
            "BL1", "ger.1",
            "CL", "uefa.champions");

    private EspnLeagueSlugs() {
    }

    public static String slugFor(String competitionCode) {
        return SLUGS.get(competitionCode);
    }
}
