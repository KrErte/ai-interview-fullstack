package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.UserProgressResponse;
import ee.kerrete.ainterview.model.JobAnalysisSession;
import ee.kerrete.ainterview.model.TrainingProgress;
import ee.kerrete.ainterview.model.TrainingTask;
import ee.kerrete.ainterview.repository.JobAnalysisSessionRepository;
import ee.kerrete.ainterview.repository.TrainingProgressRepository;
import ee.kerrete.ainterview.repository.TrainingTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProgressService {

    private final TrainingProgressRepository trainingProgressRepository;
    private final TrainingTaskRepository trainingTaskRepository;
    private final JobAnalysisSessionRepository jobAnalysisSessionRepository;

    /**
     * Põhimeetod kasutaja profiili progressi jaoks.
     */
    @Transactional(readOnly = true)
    public UserProgressResponse getUserProgress(String email) {
        // Võtame TrainingProgress, kui olemas
        TrainingProgress progress = trainingProgressRepository.findByEmail(email).orElse(null);

        // Mitu Job Matcheri analüüsi on tehtud
        long totalJobAnalyses = jobAnalysisSessionRepository.countByEmail(email);

        // Mitu treeningsessiooni – hoiame TrainingProgressis
        int totalTrainingSessions = progress != null ? progress.getTotalTrainingSessions() : 0;

        // Viimane aktiivsus
        LocalDateTime lastActive = null;

        // 1) kui TrainingProgressis on juba lastActivityAt, kasutame seda
        if (progress != null && progress.getLastActivityAt() != null) {
            lastActive = progress.getLastActivityAt();
        } else {
            // 2) arvutame viimase aktiivsuse treening-taskide ja Job Matcheri põhjal
            LocalDateTime lastTaskActivity = trainingTaskRepository.findByEmailOrderByCreatedAtDesc(email)
                    .stream()
                    .findFirst()
                    .map(task -> {
                        LocalDateTime updated = task.getUpdatedAt();
                        return updated != null ? updated : task.getCreatedAt();
                    })
                    .orElse(null);

            LocalDateTime lastJobActivity = jobAnalysisSessionRepository
                    .findTopByEmailOrderByCreatedAtDesc(email)
                    .map(JobAnalysisSession::getCreatedAt)
                    .orElse(null);

            lastActive = max(lastTaskActivity, lastJobActivity);
        }

        // Viimane match skoor + kokkuvõte
        Double lastMatchScore = null;
        String lastMatchSummary = null;

        if (progress != null) {
            lastMatchScore = progress.getLastMatchScore();
            lastMatchSummary = progress.getLastMatchSummary();
        }

        // Kui TrainingProgressis EI OLE skoori, proovime viimasest JobAnalysisSessionist.
        // (Kokkuvõtet me enam JobAnalysisSessionist ei küsi – pole getMatchSummary meetodit.)
        if (lastMatchScore == null) {
            JobAnalysisSession lastJob = jobAnalysisSessionRepository
                    .findTopByEmailOrderByCreatedAtDesc(email)
                    .orElse(null);

            if (lastJob != null) {
                lastMatchScore = lastJob.getMatchScore();
            }
        }

        // Treeningu progress protsentides
        Double trainingProgressPercent = null;
        if (progress != null) {
            trainingProgressPercent = (double) progress.getTrainingProgressPercent();
        }

        // Strengths/weaknesses – praegu tühi list, et mitte viidata mitteeksisteerivatele meetoditele
        List<String> strengths = List.of();
        List<String> weaknesses = List.of();

        return UserProgressResponse.builder()
                .email(email)
                .totalJobAnalyses(totalJobAnalyses)
                .totalTrainingSessions(totalTrainingSessions)
                .lastActive(lastActive)
                .lastMatchScore(lastMatchScore)
                .lastMatchSummary(lastMatchSummary)
                .lastTrainerStrengths(strengths)
                .lastTrainerWeaknesses(weaknesses)
                .trainingProgressPercent(trainingProgressPercent)
                .build();
    }

    /**
     * Alias, juhuks kui kontroller kutsub vana nimega meetodit.
     */
    @Transactional(readOnly = true)
    public UserProgressResponse getProgress(String email) {
        return getUserProgress(email);
    }

    private LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }
}
