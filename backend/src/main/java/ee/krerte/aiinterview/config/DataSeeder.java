package ee.krerte.aiinterview.config;

import ee.krerte.aiinterview.model.JobAnalysisSession;
import ee.krerte.aiinterview.repository.JobAnalysisSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final JobAnalysisSessionRepository jobAnalysisSessionRepository;

    @Override
    public void run(String... args) {
        String email = "test@ai.com";

        boolean exists = jobAnalysisSessionRepository.existsByEmail(email);
        if (!exists) {
            JobAnalysisSession session = JobAnalysisSession.builder()
                    .email(email)
                    .userEmail(email)
                    .jobTitle("Senior Java Developer")
                    .jobDescription("Example seeded job description.")
                    .analysisResult("Seeded analysis result.")
                    .missingSkillsJson("[]")
                    .roadmapJson("[]")
                    .suggestedImprovementsJson("[]")
                    .matchScore(0.75)
                    .summary("Seeded summary for test user.")
                    .createdAt(LocalDateTime.now())
                    .build();

            jobAnalysisSessionRepository.save(session);
        }
    }
}
