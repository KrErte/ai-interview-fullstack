package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.ProfileOverviewResponse;
import ee.kerrete.ainterview.model.JobAnalysisSession;
import ee.kerrete.ainterview.model.TrainingProgress;
import ee.kerrete.ainterview.repository.JobAnalysisSessionRepository;
import ee.kerrete.ainterview.repository.TrainingProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Teenus, mis annab profiili ülevaate.
 *
 * Loeb Job Matcheri ajaloo põhjal:
 *  - mitu analüüsi kasutaja on kokku teinud (ainult tema enda, FP-25)
 *  - mitu analüüsi antud emailiga
 *  - viimase analüüsi skoor ja kokkuvõte
 *  - viimase aktiivsuse aja
 */
@Service
@RequiredArgsConstructor
public class ProfileOverviewService {

    private final JobAnalysisSessionRepository jobAnalysisSessionRepository;
    private final TrainingProgressRepository trainingProgressRepository;

    public ProfileOverviewResponse getOverview(String email) {

        // FP-25: totalAnalyses used to be the count of ALL users' analyses; it is now the user's own count
        // (kept as a field for existing clients, same value as totalAnalysesForEmail).
        int totalAnalysesForEmail = email != null
                ? Math.toIntExact(jobAnalysisSessionRepository.countByEmail(email))
                : 0;
        int totalAnalyses = totalAnalysesForEmail;

        JobAnalysisSession last =
                email != null ? jobAnalysisSessionRepository.findTopByEmailOrderByCreatedAtDesc(email).orElse(null) : null;

        Double lastScore = last != null ? last.getMatchScore() : null;
        String lastSummary = last != null ? last.getSummary() : null;
        String lastActive = last != null && last.getCreatedAt() != null ? last.getCreatedAt().toString() : null;

        TrainingProgress progress = email != null
                ? trainingProgressRepository.findByEmail(email).orElse(null)
                : null;

        return ProfileOverviewResponse.builder()
                .totalAnalyses(totalAnalyses)
                .totalAnalysesForEmail(totalAnalysesForEmail)
                .lastMatchScoreForEmail(lastScore)
                .lastSummaryForEmail(lastSummary)
                .lastActive(lastActive)
                .trainingSessionCount(progress != null ? progress.getTotalTrainingSessions() : 0)
                .build();
    }
}
