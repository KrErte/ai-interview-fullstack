package ee.krerte.aiinterview.config;

import ee.krerte.aiinterview.model.JobAnalysisSession;
import ee.krerte.aiinterview.model.TrainingTask;
import ee.krerte.aiinterview.repository.JobAnalysisSessionRepository;
import ee.krerte.aiinterview.repository.TrainingTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final JobAnalysisSessionRepository jobAnalysisSessionRepository;
    private final TrainingTaskRepository trainingTaskRepository;

    @Override
    public void run(String... args) {

        String email = "test@ai.com";

        seedJobAnalysis(email);
        seedTrainingTasks(email);

        log.info(">>> SEEDING COMPLETE (JobAnalysis + Training)");
    }

    private void seedJobAnalysis(String email) {
        long existing = jobAnalysisSessionRepository.countByEmail(email);
        log.info("JobAnalysisSession existing rows for {} before seeding: {}", email, existing);

        if (existing > 0) {
            log.info("JobAnalysisSession already seeded for {}, skipping.", email);
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        JobAnalysisSession s1 = JobAnalysisSession.builder()
                .email(email)
                .matchScore(82.5)
                .summary("Väga tugev taust Java ja mikroteenuste arenduses. Sobib kõrgema taseme arendajaks.")
                .missingSkillsJson("[\"DevOps: Kubernetes\", \"CI/CD: GitLab pipelines\"]")
                .roadmapJson("[{\"title\":\"Tiimikonflikti lahendamine\",\"steps\":3},{\"title\":\"TDD praktika\",\"steps\":2}]")
                .suggestedImprovementsJson("[\"Harjutada süsteemidisaini\",\"Lisa kogemus DevOps valdkonnas\"]")
                .createdAt(now.minusDays(3))
                .build();

        JobAnalysisSession s2 = JobAnalysisSession.builder()
                .email(email)
                .matchScore(74.0)
                .summary("Hea sobivus vanem-tasemel backend arendajaks, vaja tugevdada DevOps oskusi.")
                .missingSkillsJson("[\"Kubernetes\", \"Helm\", \"Observability (Prometheus, Grafana)\"]")
                .roadmapJson("[{\"title\":\"DevOps alapõhi\",\"steps\":3}]")
                .suggestedImprovementsJson("[\"Rõhuta süsteemidisaini kogemust\",\"Too välja monitoring-lahendused\"]")
                .createdAt(now.minusDays(1))
                .build();

        jobAnalysisSessionRepository.saveAll(List.of(s1, s2));

        long after = jobAnalysisSessionRepository.countByEmail(email);
        log.info("JobAnalysisSession rows for {} after seeding: {}", email, after);
    }

    private void seedTrainingTasks(String email) {
        if (!trainingTaskRepository.findByEmail(email).isEmpty()) {
            log.info("Training tasks already exist for {}, skipping.", email);
            return;
        }

        TrainingTask t1 = TrainingTask.builder()
                .email(email)
                .question("Kirjelda olukorda, kus pidid lahendama keerulise tehnilise probleemi.")
                .answer("Selgitasin, kuidas lahendasin mikroteenuste deadlocki probleemi.")
                .score(8)
                .taskKey("team_conflict_resolution")
                .completed(true)
                .createdAt(LocalDateTime.now().minusDays(2))
                .scoreUpdated(LocalDateTime.now())
                .build();

        TrainingTask t2 = TrainingTask.builder()
                .email(email)
                .question("Kui tihti kasutad test-driven development'i?")
                .answer("Kasutangi regulaarselt keerukamate domeenide puhul.")
                .score(7)
                .taskKey("tdd_practices")
                .completed(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .scoreUpdated(LocalDateTime.now())
                .build();

        trainingTaskRepository.saveAll(List.of(t1, t2));
        log.info("Training tasks seeded for {}.", email);
    }
}
