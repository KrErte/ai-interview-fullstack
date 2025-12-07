package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.TrainingProgressResponse;
import ee.kerrete.ainterview.dto.TrainingTaskRequest;
import ee.kerrete.ainterview.model.JobAnalysisSession;
import ee.kerrete.ainterview.model.TrainingProgress;
import ee.kerrete.ainterview.model.TrainingStatus;
import ee.kerrete.ainterview.model.TrainingTask;
import ee.kerrete.ainterview.repository.JobAnalysisSessionRepository;
import ee.kerrete.ainterview.repository.RoadmapTaskRepository;
import ee.kerrete.ainterview.repository.TrainingProgressRepository;
import ee.kerrete.ainterview.repository.TrainingTaskRepository;
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
    private final JobAnalysisSessionRepository jobAnalysisSessionRepository;
    private final RoadmapTaskRepository roadmapTaskRepository;

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
     * Põhimeetod progressi arvutamiseks (vanade kontrollerite jaoks).
     *
     * Arvutab:
     *  - totalTasks, completedTasks
     *  - trainingProgressPercent (0–100)
     *  - lastActivityAt = max(viimane treening-task, viimane Job Matcheri sessioon)
     */
    @Transactional(readOnly = true)
    public TrainingProgressResponse getTrainingProgress(String email) {
        long totalTasksCount = trainingTaskRepository.countByEmail(email);
        long completedTasksCount = trainingTaskRepository.countByEmailAndCompletedIsTrue(email);

        long roadmapTotal = roadmapTaskRepository.countByEmail(email);
        long roadmapCompleted = roadmapTaskRepository.countByEmailAndCompletedIsTrue(email);

        int totalTasks = Math.toIntExact(totalTasksCount);
        int completedTasks = Math.toIntExact(completedTasksCount);

        totalTasks += Math.toIntExact(roadmapTotal);
        completedTasks += Math.toIntExact(roadmapCompleted);

        // Protsent double'ina
        double progressPercent = 0.0;
        if (totalTasksCount > 0) {
            progressPercent = (completedTasksCount * 100.0) / totalTasksCount;
        }
        if (roadmapTotal > 0) {
            progressPercent = ((completedTasks) * 100.0) / (double) (totalTasks);
        }
        // Ümardame Integeriks (0–100)
        int roundedPercent = (int) Math.round(progressPercent);

        // 1) Viimane aktiivsus treening-taskide järgi (updatedAt > createdAt)
        List<TrainingTask> tasks = trainingTaskRepository.findByEmailOrderByCreatedAtDesc(email);

        LocalDateTime lastTaskActivity = null;
        if (!tasks.isEmpty()) {
            TrainingTask latest = tasks.get(0);
            if (latest.getUpdatedAt() != null) {
                lastTaskActivity = latest.getUpdatedAt();
            } else {
                lastTaskActivity = latest.getCreatedAt();
            }
        }

        // 2) Viimane aktiivsus Job Matcheri järgi (JobAnalysisSession.createdAt)
        LocalDateTime lastJobActivity = jobAnalysisSessionRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .map(JobAnalysisSession::getCreatedAt)
                .orElse(null);

        // 3) Viimane aktiivsus kokku: max(lastTaskActivity, lastJobActivity)
        LocalDateTime lastActivity = null;
        if (lastTaskActivity != null) {
            lastActivity = lastTaskActivity;
        }
        if (lastJobActivity != null) {
            if (lastActivity == null || lastJobActivity.isAfter(lastActivity)) {
                lastActivity = lastJobActivity;
            }
        }

        // Võtame olemasoleva TrainingProgress või loome uue
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

        // Uuendame entitit
        progress.setTotalTasks(totalTasks);
        progress.setCompletedTasks(completedTasks);
        progress.setTrainingProgressPercent(roundedPercent);
        progress.setLastActivityAt(lastActivity);
        progress.setLastUpdated(lastActivity != null ? lastActivity : LocalDateTime.now());
        progress.setTotalJobAnalyses(Math.toIntExact(jobAnalysisSessionRepository.countByEmail(email)));

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
                .lastMatchScore(progress.getLastMatchScore())
                .lastMatchSummary(progress.getLastMatchSummary())
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

        if (task.getCreatedAt() == null) {
            task.setCreatedAt(LocalDateTime.now());
        }

        task.setSkillKey(request.getSkillKey());
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
