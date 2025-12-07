package ee.kerrete.ainterview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.dto.*;
import ee.kerrete.ainterview.model.JobAnalysisSession;
import ee.kerrete.ainterview.model.TrainingProgress;
import ee.kerrete.ainterview.model.TrainingStatus;
import ee.kerrete.ainterview.repository.JobAnalysisSessionRepository;
import ee.kerrete.ainterview.repository.TrainingProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class JobMatchService {

    private final UserProfileService userProfileService;
    private final CvSummaryService cvSummaryService;
    private final TrainingProgressRepository trainingProgressRepository;
    private final JobAnalysisSessionRepository jobAnalysisSessionRepository;
    private final ObjectMapper objectMapper;

    private record JobRole(String title, String description, List<String> requiredSkills) {}

    private static final List<JobRole> JOB_ROLES = List.of(
            new JobRole("Senior Java Engineer", "Build APIs with Spring Boot and cloud native patterns",
                    List.of("java", "spring", "spring boot", "docker", "kubernetes", "sql", "aws")),
            new JobRole("Full-Stack Engineer (Angular)", "Ship user-facing features with Angular and REST backends",
                    List.of("angular", "typescript", "javascript", "html", "css", "java", "spring")),
            new JobRole("Data Engineer", "Data pipelines and streaming for analytics",
                    List.of("python", "kafka", "spark", "sql", "airflow", "docker")),
            new JobRole("DevOps Engineer", "CI/CD, infrastructure as code, and observability",
                    List.of("aws", "azure", "gcp", "terraform", "docker", "kubernetes", "ci/cd", "linux")),
            new JobRole("Backend Engineer (Node)", "APIs and services on Node/TypeScript",
                    List.of("node", "typescript", "javascript", "docker", "aws", "sql")),
            new JobRole("Mobile Engineer", "Ship mobile apps with React Native",
                    List.of("react", "react native", "typescript", "javascript", "ci/cd")),
            new JobRole("Platform Engineer", "Internal developer platform and reliability",
                    List.of("kubernetes", "terraform", "aws", "observability", "java", "python")),
            new JobRole("Product Engineer", "Full-cycle product delivery",
                    List.of("typescript", "react", "node", "sql", "ux", "testing"))
    );

    @Transactional(readOnly = true)
    public List<JobMatchDto> match(JobMatchRequest request) {
        String email = resolveEmail(request);
        UserProfileDto profile = userProfileService.getProfile(email);
        List<String> profileSkills = Arrays.asList(userProfileService.splitSkills(profile.getSkills()));

        List<String> cvSkills = cvSummaryService.findByEmail(email)
                .map(CvSummaryDto::getParsedSkills)
                .orElse(List.of());

        if (StringUtils.hasText(request.getCvText())) {
            // lightweight parse ad-hoc text
            cvSkills = cvSummaryService.extractSkills(request.getCvText());
        }

        Set<String> candidateSkills = new LinkedHashSet<>();
        candidateSkills.addAll(profileSkills.stream().map(String::toLowerCase).toList());
        candidateSkills.addAll(cvSkills.stream().map(String::toLowerCase).toList());

        JobAnalysisSession lastAnalysis = jobAnalysisSessionRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElse(null);

        Set<String> analysisStrengths = parseJsonList(lastAnalysis != null ? lastAnalysis.getStrengthsJson() : null);
        candidateSkills.addAll(analysisStrengths);

        TrainingProgress progress = trainingProgressRepository.findByEmail(email).orElse(null);
        double progressBonus = progress != null ? progress.getTrainingProgressPercent() / 100.0 * 0.15 : 0.0;
        double lastFitBonus = progress != null && progress.getLastMatchScore() != null
                ? Math.min(0.15, progress.getLastMatchScore() / 100.0 * 0.1)
                : 0.0;

        JobRole target = findRole(request.getTargetRole());

        List<JobRole> rolesToScore = target != null ? List.of(target) : JOB_ROLES;

        List<JobMatchDto> results = new ArrayList<>();
        for (JobRole role : rolesToScore) {
            Set<String> required = role.requiredSkills.stream().map(String::toLowerCase).collect(Collectors.toSet());
            Set<String> strengths = candidateSkills.stream()
                    .filter(required::contains)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Set<String> gaps = required.stream()
                    .filter(s -> !candidateSkills.contains(s))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            double overlapRatio = required.isEmpty() ? 0 : (double) strengths.size() / required.size();
            double fit = (overlapRatio + progressBonus + lastFitBonus) * 100.0;
            fit = Math.min(100.0, Math.round(fit * 10.0) / 10.0);

            results.add(JobMatchDto.builder()
                    .jobTitle(role.title())
                    .jobDescription(role.description())
                    .fitScore(fit)
                    .strengths(new ArrayList<>(strengths))
                    .gaps(new ArrayList<>(gaps))
                    .roadmap(buildRoadmap(gaps))
                    .summary(buildSummary(role.title(), fit, strengths, gaps))
                    .source("in-memory-sample")
                    .build());
        }
        // sort by fit desc
        results.sort(Comparator.comparing(JobMatchDto::getFitScore, Comparator.nullsLast(Comparator.reverseOrder())));

        // persist last match to training progress
        progress = trainingProgressRepository.findByEmail(email)
                .orElseGet(() -> TrainingProgress.builder()
                        .email(email)
                        .totalTasks(0)
                        .completedTasks(0)
                        .totalJobAnalyses(0)
                        .totalTrainingSessions(0)
                        .trainingProgressPercent(0)
                        .status(TrainingStatus.NOT_STARTED)
                        .lastUpdated(LocalDateTime.now())
                        .build());
        if (!results.isEmpty()) {
            JobMatchDto top = results.get(0);
            progress.setLastMatchScore(top.getFitScore());
            progress.setLastMatchSummary(top.getSummary());
            progress.setLastUpdated(LocalDateTime.now());
            trainingProgressRepository.save(progress);
        }

        return results;
    }

    private JobRole findRole(String targetRole) {
        if (!StringUtils.hasText(targetRole)) return null;
        String normalized = targetRole.toLowerCase(Locale.ROOT);
        return JOB_ROLES.stream()
                .filter(role -> role.title.toLowerCase(Locale.ROOT).contains(normalized))
                .findFirst()
                .orElse(null);
    }

    private List<String> buildRoadmap(Set<String> gaps) {
        List<String> roadmap = new ArrayList<>();
        int day = 1;
        for (String gap : gaps) {
            roadmap.add("Day " + day + ": focus on " + gap + " with a 1-hour tutorial and a small practice task.");
            day++;
        }
        if (roadmap.isEmpty()) {
            roadmap.add("Continue deepening current strengths; add a portfolio example this week.");
        }
        return roadmap;
    }

    private String buildSummary(String title, double fit, Set<String> strengths, Set<String> gaps) {
        return "Fit for " + title + " is " + Math.round(fit) + "%. "
                + "Strong in " + String.join(", ", strengths)
                + (gaps.isEmpty() ? ". Minor gaps detected." : ". Mind the gaps: " + String.join(", ", gaps));
    }

    private String resolveEmail(JobMatchRequest request) {
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            return request.getEmail();
        }
        throw new IllegalArgumentException("Email is required for job match");
    }

    private Set<String> parseJsonList(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Set.of();
        }
        try {
            List<String> list = objectMapper.readValue(raw, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            return list.stream().map(String::toLowerCase).collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (Exception e) {
            return Set.of();
        }
    }
}

