package ee.krerte.aiinterview.service;

import ee.krerte.aiinterview.dto.UserProgressResponse;
import ee.krerte.aiinterview.model.JobAnalysisSession;
import ee.krerte.aiinterview.model.TrainingProgress;
import ee.krerte.aiinterview.model.TrainingTask;
import ee.krerte.aiinterview.repository.JobAnalysisSessionRepository;
import ee.krerte.aiinterview.repository.TrainingProgressRepository;
import ee.krerte.aiinterview.repository.TrainingTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UserProgressService {

    private final TrainingProgressRepository trainingProgressRepository;
    private final TrainingTaskRepository trainingTaskRepository;
    private final JobAnalysisSessionRepository jobAnalysisSessionRepository;

    /**
     * Tagastab profiili koondandmed vastavalt UserProgressResponse DTO ülesehitusele.
     */
    public UserProgressResponse getUserProgress(String email) {

        TrainingProgress progress = trainingProgressRepository
                .findByEmail(email)
                .orElse(null);

        long totalJobAnalyses = progress != null ? progress.getTotalJobAnalyses() : 0;
        int totalTrainingSessions = progress != null ? progress.getTotalTrainingSessions() : 0;

        Double trainingProgressPercent =
                progress != null ? (double) progress.getTrainingProgressPercent() : null;

        // JAGU 1: päris viimane aktiivsus
        LocalDateTime lastActive = calculateLastActivity(email);

        // JAGU 2: logi viimane job matcheri skoor (kui olemas)
        Optional<JobAnalysisSession> lastSessionOpt =
                jobAnalysisSessionRepository.findTopByEmailOrderByCreatedAtDesc(email);

        Double lastMatchScore = lastSessionOpt.map(JobAnalysisSession::getMatchScore).orElse(null);
        String lastMatchSummary = lastSessionOpt.map(JobAnalysisSession::getSummary).orElse(null);

        // JAGU 3: viimase treeningu tugevused ja nõrkused
        List<String> strengths = lastSessionOpt
                .map(JobAnalysisSession::getStrengths)
                .orElse(Collections.emptyList());

        List<String> weaknesses = lastSessionOpt
                .map(JobAnalysisSession::getWeaknesses)
                .orElse(Collections.emptyList());

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
     * Arvutab viimase aktiivsuse:
     * max(viimane TrainingTask.createdAt, viimane JobAnalysisSession.createdAt)
     */
    private LocalDateTime calculateLastActivity(String email) {

        Optional<TrainingTask> lastTaskOpt =
                trainingTaskRepository.findTopByEmailOrderByCreatedAtDesc(email);

        Optional<JobAnalysisSession> lastJobOpt =
                jobAnalysisSessionRepository.findTopByEmailOrderByCreatedAtDesc(email);

        LocalDateTime lastTaskTime = lastTaskOpt.map(TrainingTask::getCreatedAt).orElse(null);
        LocalDateTime lastJobTime = lastJobOpt.map(JobAnalysisSession::getCreatedAt).orElse(null);

        return Stream.of(lastTaskTime, lastJobTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }
}
