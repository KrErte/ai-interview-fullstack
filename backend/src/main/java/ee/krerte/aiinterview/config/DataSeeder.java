package ee.krerte.aiinterview.config;

import ee.krerte.aiinterview.model.JobAnalysisSession;
import ee.krerte.aiinterview.model.TrainingProgress;
import ee.krerte.aiinterview.model.TrainingStatus;
import ee.krerte.aiinterview.repository.JobAnalysisSessionRepository;
import ee.krerte.aiinterview.repository.TrainingProgressRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataSeeder {

    private final TrainingProgressRepository trainingProgressRepository;
    private final JobAnalysisSessionRepository jobAnalysisSessionRepository;

    @PostConstruct
    public void seed() {
        seedUser("user@example.com");
        seedUser("test@ai.com");
    }

    private void seedUser(String email) {
        seedTrainingProgress(email);
        seedJobAnalysisSession(email);
    }

    private void seedTrainingProgress(String email) {
        boolean trainingExists = trainingProgressRepository.existsByEmail(email);

        if (trainingExists) {
            return;
        }

        TrainingProgress tp = TrainingProgress.builder()
                .email(email)
                .totalTasks(0)
                .completedTasks(0)
                .totalJobAnalyses(0)
                .totalTrainingSessions(0)
                .trainingProgressPercent(0)
                .status(TrainingStatus.NOT_STARTED)
                .lastActivityAt(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                // Double – kasutame 0.0, mitte 0
                .lastMatchScore(0.0)
                .lastMatchSummary("No match yet")
                .build();

        trainingProgressRepository.save(tp);
    }

    private void seedJobAnalysisSession(String email) {
        boolean jobExists = jobAnalysisSessionRepository.existsByEmail(email);

        if (jobExists) {
            return;
        }

        JobAnalysisSession session = JobAnalysisSession.builder()
                .email(email)
                .createdAt(LocalDateTime.now().minusDays(1))
                .jobTitle("Software Engineer")
                .jobDescription("Example description for initial seeded Job Matcher session.")
                .aiSummary("Initial seeded AI summary for demo purposes.")
                .aiScore(75)
                .build();

        jobAnalysisSessionRepository.save(session);
    }
}
