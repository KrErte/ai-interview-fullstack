package ee.krerte.aiinterview.service;

import ee.krerte.aiinterview.dto.UserProgressResponse;
import ee.krerte.aiinterview.model.JobAnalysisSession;
import ee.krerte.aiinterview.model.TrainingProgress;
import ee.krerte.aiinterview.repository.JobAnalysisSessionRepository;
import ee.krerte.aiinterview.repository.TrainingProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserProgressService {

    private final TrainingProgressRepository trainingProgressRepository;
    private final JobAnalysisSessionRepository jobAnalysisSessionRepository;

    public UserProgressResponse getProgress(String email) {

        Optional<TrainingProgress> progressOpt = trainingProgressRepository.findByEmail(email);
        Optional<JobAnalysisSession> lastJob = jobAnalysisSessionRepository.findTopByEmailOrderByCreatedAtDesc(email);

        Double trainingPercent = progressOpt.map(TrainingProgress::getProgressPercent).orElse(0.0);
        Integer roundedPercent = trainingPercent != null ? (int) Math.round(trainingPercent) : 0;

        Instant lastJobInstant = lastJob
                .map(job -> job.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant())
                .orElse(null);

        return UserProgressResponse.builder()
                .email(email)
                .trainingProgressPercent(roundedPercent)
                .lastJobAnalysis(lastJobInstant)
                .build();
    }
}
