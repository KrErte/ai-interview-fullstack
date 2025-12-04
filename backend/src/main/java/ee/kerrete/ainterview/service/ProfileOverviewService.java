package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.ProfileOverviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Teenus, mis annab profiili ülevaate.
 *
 * Loeb Job Matcheri ajaloo põhjal:
 *  - mitut analüüsi on kokku tehtud
 *  - mitu analüüsi antud emailiga
 *  - viimase analüüsi skoor ja kokkuvõte
 *  - viimase aktiivsuse aja
 */
@Service
@RequiredArgsConstructor
public class ProfileOverviewService {

    private final JobAnalysisStatsService jobAnalysisStatsService;

    public ProfileOverviewResponse getOverview(String email) {

        int totalAnalyses = jobAnalysisStatsService.getTotalAnalyses();
        int totalAnalysesForEmail = jobAnalysisStatsService.getTotalAnalysesFor(email);

        JobAnalysisStatsService.JobAnalysisRecord last =
                jobAnalysisStatsService.getLastFor(email);

        Double lastScore = last != null ? last.getScore() : null;
        String lastSummary = last != null ? last.getSummary() : null;
        String lastActive = last != null ? last.getTimestamp().toString() : null;

        return ProfileOverviewResponse.builder()
                .totalAnalyses(totalAnalyses)
                .totalAnalysesForEmail(totalAnalysesForEmail)
                .lastMatchScoreForEmail(lastScore)
                .lastSummaryForEmail(lastSummary)
                .lastActive(lastActive)
                .build();
    }
}
