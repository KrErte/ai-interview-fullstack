package ee.krerte.aiinterview.service;

import ee.krerte.aiinterview.dto.TrainingProgressDto;
import ee.krerte.aiinterview.model.TrainingProgress;
import ee.krerte.aiinterview.model.TrainingStatus;
import ee.krerte.aiinterview.repository.TrainingProgressRepository;
import ee.krerte.aiinterview.repository.TrainingTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final TrainingTaskRepository trainingTaskRepository;
    private final TrainingProgressRepository trainingProgressRepository;

    @Transactional(readOnly = true)
    public TrainingProgressDto getProgress(String email) {
        long totalTasks = trainingTaskRepository.countByEmail(email);
        long completedTasks = trainingTaskRepository.countByEmailAndCompletedIsTrue(email);

        TrainingProgress progress = trainingProgressRepository.findByEmail(email)
                .orElseGet(() -> TrainingProgress.builder()
                        .email(email)
                        .totalTasks(0)
                        .completedTasks(0)
                        .totalJobAnalyses(0)
                        .totalTrainingSessions(0)
                        .trainingProgressPercent(0)
                        .status(TrainingStatus.NOT_STARTED)
                        .lastActivityAt(null)
                        .lastUpdated(LocalDateTime.now())
                        .lastMatchScore(null)
                        .lastMatchSummary(null)
                        .build()
                );

        progress.setTotalTasks((int) totalTasks);
        progress.setCompletedTasks((int) completedTasks);

        // Kui ProgressService kasutab trainingProgressPercenti, võime siingi seda uuendada
        int percent = (totalTasks == 0)
                ? 0
                : (int) Math.round(100.0 * completedTasks / totalTasks);
        progress.setTrainingProgressPercent(percent);
        progress.setLastUpdated(LocalDateTime.now());

        trainingProgressRepository.save(progress);

        return TrainingProgressDto.fromEntity(progress, completedTasks, totalTasks);
    }
}
