package ee.krerte.aiinterview.config;

import ee.krerte.aiinterview.model.TrainingProgress;
import ee.krerte.aiinterview.model.TrainingStatus;
import ee.krerte.aiinterview.repository.TrainingProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final TrainingProgressRepository trainingProgressRepository;

    @Override
    public void run(String... args) throws Exception {
        seedTrainingProgress();
        // kui sul on siin veel muud seeditavad asjad (app users jne),
        // jäta need ka alles
    }

    private void seedTrainingProgress() {
        // NB! kasuta sama e-maili, mida frontendis testimiseks kasutad
        String email = "user@example.com";

        Optional<TrainingProgress> existing = trainingProgressRepository.findByEmail(email);
        if (existing.isPresent()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        TrainingProgress progress = TrainingProgress.builder()
                .email(email)
                .totalTasks(0)
                .completedTasks(0)
                .totalJobAnalyses(0)
                .totalTrainingSessions(0)
                .trainingProgressPercent(0)
                .status(TrainingStatus.NOT_STARTED)
                .lastActivityAt(now)
                .lastUpdated(now)          // <<=== OLULINE RIDA
                .lastMatchScore(null)
                .lastMatchSummary(null)
                .build();

        trainingProgressRepository.save(progress);
    }
}
