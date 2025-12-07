package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.DashboardResponse;
import ee.kerrete.ainterview.dto.UserProfileDto;
import ee.kerrete.ainterview.model.JobAnalysisSession;
import ee.kerrete.ainterview.model.TrainingProgress;
import ee.kerrete.ainterview.repository.CvSummaryRepository;
import ee.kerrete.ainterview.repository.JobAnalysisSessionRepository;
import ee.kerrete.ainterview.repository.RoadmapTaskRepository;
import ee.kerrete.ainterview.repository.TrainingProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserProfileService userProfileService;
    private final CvSummaryRepository cvSummaryRepository;
    private final TrainingProgressRepository trainingProgressRepository;
    private final RoadmapTaskRepository roadmapTaskRepository;
    private final JobAnalysisSessionRepository jobAnalysisSessionRepository;

    @Transactional(readOnly = true)
    public DashboardResponse get(String email) {
        UserProfileDto profile = userProfileService.getProfile(email);

        TrainingProgress progress = trainingProgressRepository.findByEmail(email)
                .orElse(null);

        int totalRoadmapTasks = Math.toIntExact(roadmapTaskRepository.countByEmail(email));
        int completedRoadmap = Math.toIntExact(roadmapTaskRepository.countByEmailAndCompletedIsTrue(email));

        int totalTrainingTasks = totalRoadmapTasks;
        int completedTrainingTasks = completedRoadmap;

        if (progress != null) {
            totalTrainingTasks += progress.getTotalTasks();
            completedTrainingTasks += progress.getCompletedTasks();
        }

        JobAnalysisSession last = jobAnalysisSessionRepository.findTopByEmailOrderByCreatedAtDesc(email).orElse(null);
        Double lastScore = last != null ? last.getMatchScore() : (progress != null ? progress.getLastMatchScore() : null);
        String lastSummary = last != null ? last.getSummary() : (progress != null ? progress.getLastMatchSummary() : null);
        String lastActive = null;
        if (progress != null && progress.getLastActivityAt() != null) {
            lastActive = progress.getLastActivityAt().toString();
        } else if (last != null && last.getCreatedAt() != null) {
            lastActive = last.getCreatedAt().toString();
        }

        boolean cvUploaded = cvSummaryRepository.existsByEmail(email);

        return DashboardResponse.builder()
                .email(email)
                .fullName(profile.getFullName())
                .targetRole(profile.getTargetRole())
                .profileCompleteness(profile.getProfileCompleteness())
                .latestFitScore(lastScore)
                .totalTrainingTasks(totalTrainingTasks)
                .completedTrainingTasks(completedTrainingTasks)
                .trainingProgressPercent(totalTrainingTasks > 0 ? Math.toIntExact(Math.round(completedTrainingTasks * 100.0 / totalTrainingTasks)) :
                        (progress != null ? progress.getTrainingProgressPercent() : 0))
                .cvUploaded(cvUploaded)
                .lastAnalysisSummary(lastSummary)
                .lastActive(lastActive)
                .build();
    }
}

