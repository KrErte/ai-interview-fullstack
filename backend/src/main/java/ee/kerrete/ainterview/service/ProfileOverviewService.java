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
 *  - mitut analüüsi on kokku tehtud
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

        int totalAnalyses = Math.toIntExact(jobAnalysisSessionRepository.count());
        int totalAnalysesForEmail = email != null
                ? Math.toIntExact(jobAnalysisSessionRepository.countByEmail(email))
                : 0;

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
