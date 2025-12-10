package ee.kerrete.ainterview.recruiter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.model.AppUser;
import ee.kerrete.ainterview.model.JobAnalysisSession;
import ee.kerrete.ainterview.model.TrainingProgress;
import ee.kerrete.ainterview.model.UserProfile;
import ee.kerrete.ainterview.repository.JobAnalysisSessionRepository;
import ee.kerrete.ainterview.repository.TrainingProgressRepository;
import ee.kerrete.ainterview.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecruiterOverviewService {

    private final UserProfileRepository userProfileRepository;
    private final TrainingProgressRepository trainingProgressRepository;
    private final JobAnalysisSessionRepository jobAnalysisSessionRepository;
    private final ObjectMapper objectMapper;

    public RecruiterOverviewResponse getOverview(AppUser user, RecruiterOverviewRequest request) {
        String email = user.getEmail();

        RecruiterCandidateSummaryDto candidate = buildCandidateSummary(user, email);

        return RecruiterOverviewResponse.builder()
                .jobDescriptionEcho(request != null ? request.getJobDescription() : null)
                .candidates(List.of(candidate))
                .build();
    }

    private RecruiterCandidateSummaryDto buildCandidateSummary(AppUser user, String email) {
        UserProfile profile = userProfileRepository.findByEmail(email).orElse(null);
        String name = profile != null && StringUtils.hasText(profile.getFullName())
                ? profile.getFullName()
                : (user.getFullName() != null ? user.getFullName() : email);

        List<String> skills = parseSkills(profile != null ? profile.getSkills() : null);

        long analysesRun = jobAnalysisSessionRepository.countByEmail(email);
        JobAnalysisSession latestAnalysis = jobAnalysisSessionRepository.findTopByEmailOrderByCreatedAtDesc(email).orElse(null);
        double matchScore = latestAnalysis != null && latestAnalysis.getMatchScore() != null
                ? latestAnalysis.getMatchScore()
                : 84.0; // TODO: replace placeholder once scoring is always available

        String scoreLabel = labelForScore(matchScore);

        TrainingProgress progress = trainingProgressRepository.findByEmail(email).orElse(null);
        int trainingSessions = progress != null ? Optional.ofNullable(progress.getTotalTrainingSessions()).orElse(0) : 5; // TODO: real sessions if available
        int trainingProgressPercent = progress != null ? Optional.ofNullable(progress.getTrainingProgressPercent()).orElse(0) : 42; // TODO: real percent if available

        Instant lastUpdated = resolveLastUpdated(latestAnalysis, progress, profile);

        return RecruiterCandidateSummaryDto.builder()
                .id(user.getId() != null ? String.valueOf(user.getId()) : email)
                .name(name)
                .email(email)
                .matchScore(matchScore)
                .latestScoreLabel(scoreLabel)
                .keySkills(skills)
                .analysesRun((int) analysesRun)
                .trainingSessions(trainingSessions)
                .trainingProgressPercent(trainingProgressPercent)
                .lastUpdated(lastUpdated)
                .build();
    }

    private List<String> parseSkills(String rawSkills) {
        if (!StringUtils.hasText(rawSkills)) {
            return Collections.emptyList();
        }
        // Try JSON array first, fallback to comma-split
        try {
            return objectMapper.readValue(rawSkills, new TypeReference<List<String>>() {});
        } catch (Exception ignored) {
        }
        String[] parts = rawSkills.split(",");
        return List.of(parts).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String labelForScore(double score) {
        if (score >= 80.0) return "Strong fit";
        if (score >= 60.0) return "Good fit";
        if (score >= 40.0) return "Moderate fit";
        return "Needs improvement";
    }

    private Instant resolveLastUpdated(JobAnalysisSession analysis,
                                       TrainingProgress progress,
                                       UserProfile profile) {
        Instant fromAnalysis = analysis != null && analysis.getCreatedAt() != null
                ? toInstant(analysis.getCreatedAt()) : null;
        Instant fromProgress = progress != null && progress.getLastUpdated() != null
                ? toInstant(progress.getLastUpdated()) : null;
        Instant fromProfile = profile != null && profile.getUpdatedAt() != null
                ? toInstant(profile.getUpdatedAt()) : null;

        return List.of(fromAnalysis, fromProgress, fromProfile).stream()
                .filter(i -> i != null)
                .max(Instant::compareTo)
                .orElse(Instant.now()); // TODO: replace with real last-updated source when available
    }

    private Instant toInstant(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant();
    }
}

