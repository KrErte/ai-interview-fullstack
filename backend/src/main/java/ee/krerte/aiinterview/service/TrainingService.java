package ee.krerte.aiinterview.service;

import ee.krerte.aiinterview.dto.TrainingProgressResponse;
import ee.krerte.aiinterview.dto.TrainingTaskRequest;
import ee.krerte.aiinterview.model.TrainingProgress;
import ee.krerte.aiinterview.model.TrainingStatus;
import ee.krerte.aiinterview.model.TrainingTask;
import ee.krerte.aiinterview.repository.TrainingProgressRepository;
import ee.krerte.aiinterview.repository.TrainingTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainingService {

    private final TrainingTaskRepository trainingTaskRepository;
    private final TrainingProgressRepository trainingProgressRepository;

    /**
     * VANA signatuur, mida kasutavad ProgressController ja TrainingProgressController:
     * trainingService.getProgress(email)
     */
    @Transactional(readOnly = true)
    public TrainingProgressResponse getProgress(String email) {
        return getTrainingProgress(email);
    }

    /**
     * VANA signatuur, mida kasutavad erinevad kontrollerid:
     * trainingService.updateTaskStatus(request)
     *
     * Salvestab/uuendab taski ja tagastab uuendatud progressi.
     */
    @Transactional
    public TrainingProgressResponse updateTaskStatus(TrainingTaskRequest request) {
        saveOrUpdateTask(request);
        return getTrainingProgress(request.getEmail());
    }

    /**
     * Põhimeetod progressi arvutamiseks.
     */
    @Transactional(readOnly = true)
    public TrainingProgressResponse getTrainingProgress(String email) {
        long totalTasksCount = trainingTaskRepository.countByEmail(email);
        long completedTasksCount = trainingTaskRepository.countByEmailAndCompletedIsTrue(email);

        int totalTasks = Math.toIntExact(totalTasksCount);
        int completedTasks = Math.toIntExact(completedTasksCount);

        // Arvuta protsent Double-na
        Double progressPercent = 0.0;
        if (totalTasksCount > 0) {
            progressPercent = (completedTasksCount * 100.0) / totalTasksCount;
        }
        Double roundedPercent = Math.round(progressPercent * 10.0) / 10.0;

        // Viimane aktiivsus: võtame viimase taski järgi
        List<TrainingTask> tasks = trainingTaskRepository.findByEmailOrderByCreatedAtDesc(email);

        LocalDateTime lastActivity = null;
        if (!tasks.isEmpty()) {
            TrainingTask latest = tasks.get(0);
            if (latest.getUpdatedAt() != null) {
                lastActivity = latest.getUpdatedAt();
            } else {
                lastActivity = latest.getCreatedAt();
            }
        }

        // Võtame olemasoleva TrainingProgress või loome uue (ilma lastActivity viiteta lambdas!)
        TrainingProgress progress = trainingProgressRepository.findByEmail(email)
                .orElseGet(() -> TrainingProgress.builder()
                        .email(email)
                        .totalTasks(0)
                        .completedTasks(0)
                        .totalJobAnalyses(0)
                        .totalTrainingSessions(0)
                        .trainingProgressPercent(0)
                        .status(TrainingStatus.NOT_STARTED)
                        .build()
                );

        // Uuendame entitit
        progress.setTotalTasks(totalTasks);
        progress.setCompletedTasks(completedTasks);
        progress.setTrainingProgressPercent(progressPercent.intValue());
        progress.setLastActivityAt(lastActivity);
        progress.setLastUpdated(lastActivity != null ? lastActivity : LocalDateTime.now());

        if (totalTasks == 0) {
            progress.setStatus(TrainingStatus.NOT_STARTED);
        } else if (completedTasks == 0) {
            progress.setStatus(TrainingStatus.IN_PROGRESS);
        } else if (completedTasks == totalTasks) {
            progress.setStatus(TrainingStatus.COMPLETED);
        } else {
            progress.setStatus(TrainingStatus.IN_PROGRESS);
        }

        trainingProgressRepository.save(progress);

        return TrainingProgressResponse.builder()
                .email(email)
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .totalJobAnalyses(progress.getTotalJobAnalyses())
                .totalTrainingSessions(progress.getTotalTrainingSessions())
                .trainingProgressPercent(roundedPercent)
                .lastActivityAt(progress.getLastActivityAt())
                .status(progress.getStatus())
                .build();
    }

    /**
     * Loob või uuendab üksikut treening-taski (kasutaja vastuse ja staatuse põhjal).
     */
    @Transactional
    public TrainingTask saveOrUpdateTask(TrainingTaskRequest request) {
        String email = request.getEmail();
        String taskKey = request.resolveTaskKey(); // kasutame helperit

        TrainingTask task = trainingTaskRepository.findByEmailAndTaskKey(email, taskKey)
                .orElseGet(() -> TrainingTask.builder()
                        .email(email)
                        .taskKey(taskKey)
                        .createdAt(LocalDateTime.now())
                        .build()
                );

        // Vastuse tekst – eelistame "answer", kui tühi/null, siis "answerText"
        String answer = request.getAnswer();
        if (answer == null || answer.isBlank()) {
            answer = request.getAnswerText();
        }

        task.setQuestion(request.getQuestion());
        task.setAnswer(answer);

        // completed: loeme Boolean-ist -> primitive boolean
        boolean completed = Boolean.TRUE.equals(request.getCompleted());
        task.setCompleted(completed);

        task.setUpdatedAt(LocalDateTime.now());

        if (request.getScore() != null) {
            task.setScore(request.getScore());
            task.setScoreUpdated(LocalDateTime.now());
        }

        return trainingTaskRepository.save(task);
    }

    /**
     * Eraldi meetod skoori uuendamiseks – kui vaja ainult skoori muuta.
     */
    @Transactional
    public void updateTaskScore(String email, String taskKey, int score) {
        TrainingTask task = trainingTaskRepository.findByEmailAndTaskKey(email, taskKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Treening-taski ei leitud: email=" + email + ", taskKey=" + taskKey));

        task.setScore(score);
        task.setScoreUpdated(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        trainingTaskRepository.save(task);
    }
}
