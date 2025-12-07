package ee.kerrete.ainterview.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.model.*;
import ee.kerrete.ainterview.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProdDataSeeder implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String DEMO_EMAIL = "demo@example.com";
    private static final String DEMO_PASSWORD = "demo123!";
    private static final int ADMIN_PASSWORD_LENGTH = 32;
    private static final String PASSWORD_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#%&*";

    private final AppUserRepository appUserRepository;
    private final UserProfileRepository userProfileRepository;
    private final CvSummaryRepository cvSummaryRepository;
    private final JobAnalysisSessionRepository jobAnalysisSessionRepository;
    private final RoadmapTaskRepository roadmapTaskRepository;
    private final TrainingTaskRepository trainingTaskRepository;
    private final TrainingProgressRepository trainingProgressRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public void run(String... args) {
        LocalDateTime now = LocalDateTime.now();

        String adminPassword = seedAdmin(now);
        seedDemoUser(now);
        seedDemoProfile(now);
        seedCvSummary(now);
        seedJobAnalyses(now);
        seedRoadmap(now);
        seedTrainingTasks(now);
        seedTrainingProgress(now);

        if (adminPassword != null) {
            log.info("PROD ADMIN CREATED -> email={} password={}", ADMIN_EMAIL, adminPassword);
        }
        log.info("PROD demo data ready for {}", DEMO_EMAIL);
    }

    private String seedAdmin(LocalDateTime now) {
        AppUser existing = appUserRepository.findByEmail(ADMIN_EMAIL).orElse(null);
        if (existing != null) {
            boolean updated = false;
            if (!existing.isEnabled()) {
                existing.setEnabled(true);
                updated = true;
            }
            if (existing.getRole() != UserRole.ADMIN) {
                existing.setRole(UserRole.ADMIN);
                updated = true;
            }
            if (updated) {
                existing.setUpdatedAt(now);
                appUserRepository.save(existing);
            }
            return null;
        }

        String password = generateSecurePassword(ADMIN_PASSWORD_LENGTH);
        AppUser admin = AppUser.builder()
                .email(ADMIN_EMAIL)
                .fullName("Production Admin")
                .password(passwordEncoder.encode(password))
                .role(UserRole.ADMIN)
                .enabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
        appUserRepository.save(Objects.requireNonNull(admin));
        return password;
    }

    private void seedDemoUser(LocalDateTime now) {
        appUserRepository.findByEmail(DEMO_EMAIL)
                .ifPresentOrElse(user -> {
                    boolean changed = false;
                    if (!user.isEnabled()) {
                        user.setEnabled(true);
                        changed = true;
                    }
                    if (user.getRole() != UserRole.USER) {
                        user.setRole(UserRole.USER);
                        changed = true;
                    }
                    if (changed) {
                        user.setUpdatedAt(now);
                        appUserRepository.save(user);
                    }
                }, () -> {
                    AppUser demo = AppUser.builder()
                            .email(DEMO_EMAIL)
                            .fullName("Demo Candidate")
                            .password(passwordEncoder.encode(DEMO_PASSWORD))
                            .role(UserRole.USER)
                            .enabled(true)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    appUserRepository.save(Objects.requireNonNull(demo));
                    log.info("Seeded demo user {}", DEMO_EMAIL);
                });
    }

    private void seedDemoProfile(LocalDateTime now) {
        UserProfile profile = userProfileRepository.findByEmail(DEMO_EMAIL)
                .orElseGet(() -> UserProfile.builder()
                        .email(DEMO_EMAIL)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());

        profile.setFullName("Demo Candidate");
        profile.setCurrentRole("Mid-level Software Engineer");
        profile.setTargetRole("Senior Java + Angular Engineer");
        profile.setYearsOfExperience(5);
        profile.setSkills("Java, Spring Boot, Angular, TypeScript, Docker, PostgreSQL, AWS, CI/CD, Kubernetes, REST");
        profile.setBio("Demo profile with curated skills to showcase dashboards, job matching and learning paths.");

        userProfileRepository.save(profile);
    }

    private void seedCvSummary(LocalDateTime now) {
        CvSummary summary = cvSummaryRepository.findByEmail(DEMO_EMAIL)
                .orElseGet(() -> CvSummary.builder()
                        .email(DEMO_EMAIL)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());

        summary.setHeadline("Full-stack engineer who ships production features weekly.");
        summary.setParsedSkills(toJson(List.of(
                "Java", "Spring Boot", "Angular", "TypeScript", "PostgreSQL", "Docker", "Kubernetes", "AWS", "CI/CD"
        )));
        summary.setExperienceSummary("""
                5+ years building REST APIs and Angular frontends, owning CI/CD pipelines, and deploying on AWS.
                Experience with containerization (Docker/K8s), observability, and mentoring junior engineers.
                """);
        summary.setRawText("Condensed CV used for demo seeding to drive matching and dashboards.");
        cvSummaryRepository.save(summary);
    }

    private void seedJobAnalyses(LocalDateTime now) {
        if (jobAnalysisSessionRepository.countByEmail(DEMO_EMAIL) >= 5) {
            return;
        }

        List<JobAnalysisSession> sessions = new ArrayList<>();
        sessions.add(buildJobAnalysis(now.minusDays(4),
                "Senior Java Engineer",
                88.5,
                List.of("java", "spring boot", "postgresql", "docker"),
                List.of("kubernetes", "aws eks"),
                List.of("Enhance k8s production playbooks", "Add autoscaling policies"),
                "Backend services at scale, observability-first mindset.",
                """
                        Strong API design and database tuning. Add deeper Kubernetes and cloud-native ops to reach 95% fit.
                        """));

        sessions.add(buildJobAnalysis(now.minusDays(3),
                "Full-Stack Engineer (Angular)",
                84.0,
                List.of("angular", "typescript", "html/css", "java", "rest"),
                List.of("ngrx", "playwright"),
                List.of("Add state management patterns", "Automate E2E coverage"),
                "End-to-end feature delivery with solid UI skills.",
                "Great UI fundamentals; invest in NgRx patterns and E2E automation to close remaining gaps."));

        sessions.add(buildJobAnalysis(now.minusDays(2),
                "DevOps Engineer",
                77.0,
                List.of("docker", "ci/cd", "monitoring"),
                List.of("terraform", "prometheus", "grafana"),
                List.of("Provision IaC blueprints", "Add alerting SLIs/SLOs"),
                "Reliability focus with delivery automation.",
                "Strong CI/CD and containers. Add IaC and observability depth to improve platform readiness."));

        sessions.add(buildJobAnalysis(now.minusDays(1),
                "Data Engineer",
                72.0,
                List.of("sql", "python", "airflow"),
                List.of("spark", "kafka", "dbt"),
                List.of("Prototype streaming ingestion", "Add dbt modeling layer"),
                "Pipelines experience ready for extension to streaming.",
                "Solid batch background; layer in streaming and modeling to raise fit."));

        sessions.add(buildJobAnalysis(now,
                "Product Engineer",
                81.0,
                List.of("typescript", "product mindset", "testing"),
                List.of("growth experiments", "feature flags"),
                List.of("Add feature flag rollout playbook", "Define experiment success metrics"),
                "Full-cycle delivery with fast iteration.",
                "Great product sensibilities; add experimentation discipline for higher impact."));

        jobAnalysisSessionRepository.saveAll(sessions);
    }

    private JobAnalysisSession buildJobAnalysis(LocalDateTime createdAt,
                                                String title,
                                                double fitScore,
                                                List<String> strengths,
                                                List<String> gaps,
                                                List<String> roadmap,
                                                String aiSummary,
                                                String summary) {
        return JobAnalysisSession.builder()
                .email(DEMO_EMAIL)
                .userEmail(DEMO_EMAIL)
                .jobTitle(title)
                .jobDescription("Demo description for " + title)
                .analysisResult(toJson(Map.of(
                        "title", title,
                        "fit", fitScore,
                        "strengths", strengths,
                        "gaps", gaps
                )))
                .missingSkillsJson(toJson(gaps))
                .strengthsJson(toJson(strengths))
                .weaknessesJson(toJson(gaps))
                .roadmapJson(toJson(roadmap))
                .suggestedImprovementsJson(toJson(roadmap))
                .aiSummary(aiSummary)
                .aiScore((int) Math.round(fitScore))
                .matchScore(fitScore)
                .summary(summary)
                .createdAt(createdAt)
                .build();
    }

    private void seedRoadmap(LocalDateTime now) {
        if (roadmapTaskRepository.countByEmail(DEMO_EMAIL) > 0) {
            return;
        }

        List<RoadmapTask> tasks = List.of(
                RoadmapTask.builder().email(DEMO_EMAIL).taskKey("day-1-java-healthcheck")
                        .title("Day 1: Java service healthcheck")
                        .description("Add /actuator health, metrics, and structured logging.")
                        .completed(true).dayNumber(1).orderIndex(1).build(),
                RoadmapTask.builder().email(DEMO_EMAIL).taskKey("day-2-angular-hardening")
                        .title("Day 2: Angular hardening")
                        .description("Add route guards, lazy loading, and production error handler.")
                        .completed(false).dayNumber(2).orderIndex(2).build(),
                RoadmapTask.builder().email(DEMO_EMAIL).taskKey("day-3-ci-cd")
                        .title("Day 3: CI/CD and container polish")
                        .description("Optimize Dockerfiles, multi-stage builds, and image scanning.")
                        .completed(false).dayNumber(3).orderIndex(3).build(),
                RoadmapTask.builder().email(DEMO_EMAIL).taskKey("day-4-k8s-readiness")
                        .title("Day 4: Kubernetes readiness")
                        .description("Add readiness/liveness probes, resource limits, and rollout plan.")
                        .completed(false).dayNumber(4).orderIndex(4).build()
        );

        roadmapTaskRepository.saveAll(new ArrayList<>(tasks));
    }

    private void seedTrainingTasks(LocalDateTime now) {
        List<TrainingTask> tasks = List.of(
                TrainingTask.builder()
                        .email(DEMO_EMAIL)
                        .question("Tell me about a time you led a migration to the cloud.")
                        .answer("Led a phased migration to AWS with zero downtime using blue/green deployments.")
                        .score(82)
                        .createdAt(now.minusDays(5))
                        .updatedAt(now.minusDays(4))
                        .scoreUpdated(now.minusDays(4))
                        .taskKey("behavioral-cloud-migration")
                        .skillKey("cloud_delivery")
                        .completed(true)
                        .feedback("Clear STAR structure. Add cost-optimization results.")
                        .build(),
                TrainingTask.builder()
                        .email(DEMO_EMAIL)
                        .question("Explain how you ensure Angular apps stay performant in production.")
                        .answer("Use lazy loading, trackBy, OnPush change detection, and bundle analysis.")
                        .score(78)
                        .createdAt(now.minusDays(3))
                        .updatedAt(now.minusDays(2))
                        .scoreUpdated(now.minusDays(2))
                        .taskKey("frontend-performance")
                        .skillKey("frontend")
                        .completed(true)
                        .feedback("Good mention of OnPush; add perf budgets and real-user monitoring.")
                        .build(),
                TrainingTask.builder()
                        .email(DEMO_EMAIL)
                        .question("How would you design CI/CD for this repository?")
                        .answer("Multi-stage Docker builds, quality gates, SBOM, image scanning, and staged deploy.")
                        .score(0)
                        .createdAt(now.minusDays(1))
                        .updatedAt(now.minusHours(12))
                        .scoreUpdated(now.minusHours(12))
                        .taskKey("cicd-design")
                        .skillKey("cicd")
                        .completed(false)
                        .feedback("Draft plan captured. Add rollback and smoke-test steps.")
                        .build()
        );

        tasks.forEach(task -> {
            if (!trainingTaskRepository.existsByEmailAndQuestion(task.getEmail(), task.getQuestion())) {
                trainingTaskRepository.save(task);
            }
        });
    }

    private void seedTrainingProgress(LocalDateTime now) {
        TrainingProgress progress = trainingProgressRepository.findByEmail(DEMO_EMAIL)
                .orElseGet(() -> TrainingProgress.builder()
                        .email(DEMO_EMAIL)
                        .totalTasks(0)
                        .completedTasks(0)
                        .totalJobAnalyses(0)
                        .totalTrainingSessions(0)
                        .trainingProgressPercent(0)
                        .status(TrainingStatus.NOT_STARTED)
                        .lastUpdated(now)
                        .lastActivityAt(now)
                        .build());

        int trainingTasks = Math.toIntExact(trainingTaskRepository.countByEmail(DEMO_EMAIL));
        int completedTraining = Math.toIntExact(trainingTaskRepository.countByEmailAndCompletedIsTrue(DEMO_EMAIL));
        long analyses = jobAnalysisSessionRepository.countByEmail(DEMO_EMAIL);

        progress.setTotalTasks(trainingTasks);
        progress.setCompletedTasks(completedTraining);
        progress.setTotalJobAnalyses((int) analyses);
        progress.setTotalTrainingSessions(Math.max(progress.getTotalTrainingSessions(), 1));
        progress.setTrainingProgressPercent(trainingTasks > 0 ? Math.toIntExact(Math.round(completedTraining * 100.0 / trainingTasks)) : 0);
        progress.setStatus(progress.getTrainingProgressPercent() >= 80 ? TrainingStatus.COMPLETED : TrainingStatus.IN_PROGRESS);
        progress.setLastActivityAt(now);
        progress.setLastUpdated(now);

        jobAnalysisSessionRepository.findByEmail(DEMO_EMAIL).stream()
                .max(Comparator.comparing(JobAnalysisSession::getMatchScore))
                .ifPresent(top -> {
                    progress.setLastMatchScore(top.getMatchScore());
                    progress.setLastMatchSummary(top.getSummary());
                });

        trainingProgressRepository.save(progress);
    }

    private String generateSecurePassword(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(PASSWORD_ALPHABET.charAt(random.nextInt(PASSWORD_ALPHABET.length())));
        }
        return sb.toString();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}

