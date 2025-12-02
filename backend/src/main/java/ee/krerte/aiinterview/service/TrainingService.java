package ee.krerte.aiinterview.service;

import ee.krerte.aiinterview.dto.TrainingProgressResponse;
import ee.krerte.aiinterview.dto.TrainingTaskRequest;
import ee.krerte.aiinterview.model.TrainingTask;
import ee.krerte.aiinterview.repository.TrainingTaskRepository;
import ee.krerte.aiinterview.repository.TrainingProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainingService {

    private final TrainingTaskRepository trainingTaskRepository;

    // jätame constructorisse, aga EI kasuta praegu, et DB ei hakkaks pinda käima
    @SuppressWarnings("unused")
    private final TrainingProgressRepository trainingProgressRepository;

    // UUS: Job Matcheri statistika profiili jaoks
    private final JobAnalysisStatsService jobAnalysisStatsService;

    /**
     * Profiili vaate progress: /api/progress?email=...
     *
     * Arvutab treeningu osa puhtalt training_task tabeli põhjal
     * + toob Job Matcheri kokkuvõtte JobAnalysisStatsService kaudu.
     */
    @Transactional(readOnly = true)
    public TrainingProgressResponse getProgress(String email) {

        // 1) TREENER – training_task tabel
        long totalTasks = trainingTaskRepository.countByEmail(email);
        long completedTasks = trainingTaskRepository.countByEmailAndCompletedIsTrue(email);

        int totalTrainingSessions = (int) totalTasks;

        int trainingProgressPercent;
        if (totalTasks == 0) {
            trainingProgressPercent = 0;
        } else {
            double ratio = (double) completedTasks / (double) totalTasks;
            trainingProgressPercent = (int) Math.round(ratio * 100.0);
        }

        // viimane aktiivsus – viimane task createdAt
        LocalDateTime lastActive = null;
        List<TrainingTask> tasks = trainingTaskRepository.findByEmailOrderByCreatedAtDesc(email);
        if (!tasks.isEmpty()) {
            lastActive = tasks.get(0).getCreatedAt();
        }

        // 2) JOB MATCHER – päris statistika mälust
        int totalJobAnalyses = jobAnalysisStatsService.getTotalAnalysesFor(email);

        Integer lastMatchScore = null;
        String lastMatchSummary = null;

        JobAnalysisStatsService.JobAnalysisRecord last =
                jobAnalysisStatsService.getLastFor(email);

        if (last != null) {
            Double score = last.getScore(); // JobAnalysisResponse.score (0–100)
            if (score != null) {
                lastMatchScore = (int) Math.round(score);
            }
            lastMatchSummary = last.getSummary();
        }

        return TrainingProgressResponse.builder()
                .totalJobAnalyses(totalJobAnalyses)
                .totalTrainingSessions(totalTrainingSessions)
                .trainingProgressPercent(trainingProgressPercent)
                .lastActive(lastActive)
                .lastMatchScore(lastMatchScore)
                .lastMatchSummary(lastMatchSummary)
                // treeneri tugevused/nõrkused – hetkel tühjad listid
                .lastTrainerStrengths(Collections.emptyList())
                .lastTrainerWeaknesses(Collections.emptyList())
                .build();
    }

    /**
     * Uuendab ühe treening-taski staatust (nt MindsetRoadmap/SkillCoach/TrainingProgress controllerid)
     * ja tagastab värskendatud progressi.
     *
     * NB! Siin me ka EI puutu TrainingProgress tabelisse – ainult training_task.
     */
    @Transactional
    public TrainingProgressResponse updateTaskStatus(TrainingTaskRequest request) {
        String email = request.getEmail();
        String taskKey = request.getTaskKey();

        TrainingTask task = trainingTaskRepository
                .findByEmailAndTaskKey(email, taskKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Training task not found for email=" + email + " taskKey=" + taskKey));

        task.setCompleted(true);
        task.setUpdatedAt(LocalDateTime.now());
        task.setScoreUpdated(LocalDateTime.now());

        trainingTaskRepository.save(task);

        // tagastame värske progressi (puhas TrainingTask põhine arvutus)
        return getProgress(email);
    }
}
