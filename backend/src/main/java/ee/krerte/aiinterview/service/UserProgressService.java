package ee.krerte.aiinterview.service;

import ee.krerte.aiinterview.dto.TrainingProgressResponse;
import ee.krerte.aiinterview.model.JobAnalysisSession;
import ee.krerte.aiinterview.model.TrainingProgress;
import ee.krerte.aiinterview.model.TrainingStatus;
import ee.krerte.aiinterview.repository.JobAnalysisSessionRepository;
import ee.krerte.aiinterview.repository.TrainingProgressRepository;
import ee.krerte.aiinterview.repository.TrainingTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProgressService {

    private final TrainingProgressRepository trainingProgressRepository;
    private final TrainingTaskRepository trainingTaskRepository;
    private final JobAnalysisSessionRepository jobAnalysisSessionRepository;

    /**
     * Tagastab kasutaja koond-progressi profiili jaoks.
     * - Kui training_progress kirjet ei ole, luuakse see automaatselt
     * - Iga päringu ajal uuendatakse kokkuvõtte väljad ja salvestatakse tagasi
     */
    @Transactional
    public TrainingProgressResponse getUserProgress(String email) {

        // --- 1) Toorandmed reposidelt (long -> int) ---
        long totalTasksCount = trainingTaskRepository.countByEmail(email);
        long completedTasksCount = trainingTaskRepository.countByEmailAndCompletedTrue(email);
        long totalJobAnalysesCount = jobAnalysisSessionRepository.countByEmail(email);

        int totalTasks = Math.toIntExact(totalTasksCount);
        int completedTasks = Math.toIntExact(completedTasksCount);
        int totalJobAnalyses = Math.toIntExact(totalJobAnalysesCount);

        // --- 2) Viimane Job Matcheri analüüs (ainult kuupäev, ilma progressita) ---
        List<JobAnalysisSession> sessions = jobAnalysisSessionRepository.findByEmail(email);
        LocalDateTime lastActivityFromJobs = sessions.stream()
                .max(Comparator.comparing(JobAnalysisSession::getCreatedAt))
                .map(JobAnalysisSession::getCreatedAt)
                .orElse(null);

        // --- 3) Võtame olemasoleva training_progress või loome uue ---
        TrainingProgress progress = trainingProgressRepository.findByEmail(email)
                .orElseGet(() -> {
                    // Esmane staatus uue rea jaoks
                    TrainingStatus initialStatus =
                            (totalTasks > 0 || totalJobAnalyses > 0)
                                    ? TrainingStatus.IN_PROGRESS
                                    : TrainingStatus.NOT_STARTED;

                    int initialPercent = 0;
                    if (totalTasksCount > 0) {
                        initialPercent = (int) Math.round((completedTasksCount * 100.0) / totalTasksCount);
                    }

                    TrainingProgress p = TrainingProgress.builder()
                            .email(email)
                            .totalTasks(totalTasks)
                            .completedTasks(completedTasks)
                            .totalJobAnalyses(totalJobAnalyses)
                            .totalTrainingSessions(0)
                            .trainingProgressPercent(initialPercent)
                            .status(initialStatus)
                            .lastActivityAt(lastActivityFromJobs)
                            .lastUpdated(LocalDateTime.now())
                            .lastMatchScore(null)
                            .lastMatchSummary(null)
                            .build();

                    return trainingProgressRepository.save(p);
                });

        // --- 4) Uuendame alati kogused progressi reas (et see peegelduks ka H2-s) ---
        progress.setTotalTasks(totalTasks);
        progress.setCompletedTasks(completedTasks);
        progress.setTotalJobAnalyses(totalJobAnalyses);

        // --- 5) Arvutame progressi protsendi ja staatuse ---
        int progressPercent = 0;
        if (totalTasksCount > 0) {
            progressPercent = (int) Math.round((completedTasksCount * 100.0) / totalTasksCount);
        }
        progress.setTrainingProgressPercent(progressPercent);

        TrainingStatus status;
        if (totalTasks == 0 && totalJobAnalyses == 0) {
            status = TrainingStatus.NOT_STARTED;
        } else if (totalTasks > 0 && completedTasks == totalTasks) {
            status = TrainingStatus.COMPLETED;
        } else {
            status = TrainingStatus.IN_PROGRESS;
        }
        progress.setStatus(status);

        // --- 6) Viimane aktiivsus: kombineerime Job Matcher + progressi enda timestampid ---
        LocalDateTime lastActivity = lastActivityFromJobs;
        if (progress.getLastActivityAt() != null &&
                (lastActivity == null || progress.getLastActivityAt().isAfter(lastActivity))) {
            lastActivity = progress.getLastActivityAt();
        }
        progress.setLastActivityAt(lastActivity);

        // --- 7) Viimati uuendatud ---
        progress.setLastUpdated(LocalDateTime.now());

        // Salvestame uuendatud progressi tagasi
        trainingProgressRepository.save(progress);

        // Frontend tahab Double protsenti ühe komakohani
        Double roundedPercent = Math.round(progressPercent * 10.0) / 10.0;

        // --- 8) Vastus frontile ---
        return TrainingProgressResponse.builder()
                .email(email)
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .totalJobAnalyses(totalJobAnalyses)
                .totalTrainingSessions(progress.getTotalTrainingSessions())
                .trainingProgressPercent(roundedPercent)
                .lastActivityAt(lastActivity)
                .status(status)
                .build();
    }
}
