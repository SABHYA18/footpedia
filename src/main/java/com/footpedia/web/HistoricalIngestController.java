package com.footpedia.web;

import com.footpedia.ingestion.HistoricalIngestSummary;
import com.footpedia.ingestion.HistoricalIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manual trigger for backfilling one historical (API-Football free-plan)
 * matchday. {@link com.footpedia.ingestion.HistoricalIngestionScheduler}
 * covers ongoing progression automatically; this remains for re-running a
 * specific matchday on demand (e.g. after a parsing fix).
 */
@RestController
@RequestMapping("/api/admin/historical")
@RequiredArgsConstructor
public class HistoricalIngestController {

    private final HistoricalIngestionService historicalIngestionService;

    @PostMapping("/ingest")
    public HistoricalIngestSummary ingest(
            @RequestParam(defaultValue = "PL") String competitionCode,
            @RequestParam(defaultValue = "39") int leagueId,
            @RequestParam int season,
            @RequestParam int matchday) {
        return historicalIngestionService.ingestMatchday(competitionCode, leagueId, season, matchday);
    }
}
