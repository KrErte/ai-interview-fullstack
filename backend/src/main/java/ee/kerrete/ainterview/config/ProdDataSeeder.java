package ee.kerrete.ainterview.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.model.*;
import ee.kerrete.ainterview.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Seeds the production database at startup.
 * <p>
 * FP-19: no credentials are hardcoded and no password is ever logged. Users are created only when
 * their credentials are configured (see application-prod.yml):
 * <ul>
 *     <li>{@code SEED_ADMIN_EMAIL} + {@code SEED_ADMIN_PASSWORD}: admin user (role ADMIN)</li>
 *     <li>{@code SEED_DEMO_EMAIL} + {@code SEED_DEMO_PASSWORD}: demo user (role USER) plus demo profile,
 *     CV summary, job analyses, roadmap, training tasks and progress</li>
 * </ul>
 * If a pair is missing (or blank) that user is not created. Existing users are never given a new
 * password; they are only re-enabled and given their expected role.
 */
@Slf4j
@Component
@Profile("prod")
public class ProdDataSeeder implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final UserProfileRepository userProfileRepository;
    private final CvSummaryRepository cvSummaryRepository;
    private final JobAnalysisSessionRepository jobAnalysisSessionRepository;
    private final RoadmapTaskRepository roadmapTaskRepository;
    private final TrainingTaskRepository trainingTaskRepository;
    private final TrainingProgressRepository trainingProgressRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String adminEmail;
    private final String adminPassword;
    private final String demoEmail;
    private final String demoPassword;

    public ProdDataSeeder(AppUserRepository appUserRepository,
                          UserProfileRepository userProfileRepository,
                          CvSummaryRepository cvSummaryRepository,
                          JobAnalysisSessionRepository jobAnalysisSessionRepository,
                          RoadmapTaskRepository roadmapTaskRepository,
                          TrainingTaskRepository trainingTaskRepository,
                          TrainingProgressRepository trainingProgressRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.seed.admin.email:}") String adminEmail,
                          @Value("${app.seed.admin.password:}") String adminPassword,
                          @Value("${app.seed.demo.email:}") String demoEmail,
                          @Value("${app.seed.demo.password:}") String demoPassword) {
        this.appUserRepository = appUserRepository;
        this.userProfileRepository = userProfileRepository;
        this.cvSummaryRepository = cvSummaryRepository;
        this.jobAnalysisSessionRepository = jobAnalysisSessionRepository;
        this.roadmapTaskRepository = roadmapTaskRepository;
        this.trainingTaskRepository = trainingTaskRepository;
        this.trainingProgressRepository = trainingProgressRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = trimToNull(adminEmail);
        this.adminPassword = adminPassword;
        this.demoEmail = trimToNull(demoEmail);
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        LocalDateTime now = LocalDateTime.now();

        if (adminEmail != null && StringUtils.hasText(adminPassword)) {
            seedAdmin(now);
        } else {
            log.info("Admin seeding skipped: SEED_ADMIN_EMAIL and SEED_ADMIN_PASSWORD are not both set");
        }

        if (demoEmail != null && StringUtils.hasText(demoPassword)) {
            seedDemoUser(now);
            seedDemoProfile(now);
            seedCvSummary(now);
            seedJobAnalyses(now);
            seedRoadmap(now);
            seedTrainingTasks(now);
            seedTrainingProgress(now);
            log.info("PROD demo data ready for {}", demoEmail);
        } else {
            log.info("Demo seeding skipped: SEED_DEMO_EMAIL and SEED_DEMO_PASSWORD are not both set");
        }
    }

    private void seedAdmin(LocalDateTime now) {
        AppUser existing = appUserRepository.findByEmail(adminEmail).orElse(null);
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
            return;
        }

        AppUser admin = AppUser.builder()
                .email(adminEmail)
                .fullName("Production Admin")
                .password(passwordEncoder.encode(adminPassword))
                .role(UserRole.ADMIN)
                .enabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
        appUserRepository.save(Objects.requireNonNull(admin));
        log.info("Seeded admin user {}", adminEmail);
    }

    private void seedDemoUser(LocalDateTime now) {
        appUserRepository.findByEmail(demoEmail)
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
                            .email(demoEmail)
                            .fullName("Demo Candidate")
                            .password(passwordEncoder.encode(demoPassword))
                            .role(UserRole.USER)
                            .enabled(true)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    appUserRepository.save(Objects.requireNonNull(demo));
                    log.info("Seeded demo user {}", demoEmail);
                });
    }

    private void seedDemoProfile(LocalDateTime now) {
        UserProfile profile = userProfileRepository.findByEmail(demoEmail)
                .orElseGet(() -> UserProfile.builder()
                        .email(demoEmail)
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
        CvSummary summary = cvSummaryRepository.findByEmail(demoEmail)
                .orElseGet(() -> CvSummary.builder()
                        .email(demoEmail)
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
        if (jobAnalysisSessionRepository.countByEmail(demoEmail) >= 5) {
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
                .email(demoEmail)
                .userEmail(demoEmail)
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
        if (roadmapTaskRepository.countByEmail(demoEmail) > 0) {
            return;
        }

        List<RoadmapTask> tasks = List.of(
                RoadmapTask.builder().email(demoEmail).taskKey("day-1-java-healthcheck")
                        .title("Day 1: Java service healthcheck")
                        .description("Add /actuator health, metrics, and structured logging.")
                        .completed(true).dayNumber(1).orderIndex(1).build(),
                RoadmapTask.builder().email(demoEmail).taskKey("day-2-angular-hardening")
                        .title("Day 2: Angular hardening")
                        .description("Add route guards, lazy loading, and production error handler.")
                        .completed(false).dayNumber(2).orderIndex(2).build(),
                RoadmapTask.builder().email(demoEmail).taskKey("day-3-ci-cd")
                        .title("Day 3: CI/CD and container polish")
                        .description("Optimize Dockerfiles, multi-stage builds, and image scanning.")
                        .completed(false).dayNumber(3).orderIndex(3).build(),
                RoadmapTask.builder().email(demoEmail).taskKey("day-4-k8s-readiness")
                        .title("Day 4: Kubernetes readiness")
                        .description("Add readiness/liveness probes, resource limits, and rollout plan.")
                        .completed(false).dayNumber(4).orderIndex(4).build()
        );

        roadmapTaskRepository.saveAll(new ArrayList<>(tasks));
    }

    private void seedTrainingTasks(LocalDateTime now) {
        List<TrainingTask> tasks = List.of(
                TrainingTask.builder()
                        .email(demoEmail)
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
                        .email(demoEmail)
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
                        .email(demoEmail)
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
        TrainingProgress progress = trainingProgressRepository.findByEmail(demoEmail)
                .orElseGet(() -> TrainingProgress.builder()
                        .email(demoEmail)
                        .totalTasks(0)
                        .completedTasks(0)
                        .totalJobAnalyses(0)
                        .totalTrainingSessions(0)
                        .trainingProgressPercent(0)
                        .status(TrainingStatus.NOT_STARTED)
                        .lastUpdated(now)
                        .lastActivityAt(now)
                        .build());

        int trainingTasks = Math.toIntExact(trainingTaskRepository.countByEmail(demoEmail));
        int completedTraining = Math.toIntExact(trainingTaskRepository.countByEmailAndCompletedIsTrue(demoEmail));
        long analyses = jobAnalysisSessionRepository.countByEmail(demoEmail);

        progress.setTotalTasks(trainingTasks);
        progress.setCompletedTasks(completedTraining);
        progress.setTotalJobAnalyses((int) analyses);
        progress.setTotalTrainingSessions(Math.max(progress.getTotalTrainingSessions(), 1));
        progress.setTrainingProgressPercent(trainingTasks > 0 ? Math.toIntExact(Math.round(completedTraining * 100.0 / trainingTasks)) : 0);
        progress.setStatus(progress.getTrainingProgressPercent() >= 80 ? TrainingStatus.COMPLETED : TrainingStatus.IN_PROGRESS);
        progress.setLastActivityAt(now);
        progress.setLastUpdated(now);

        jobAnalysisSessionRepository.findByEmail(demoEmail).stream()
                .max(Comparator.comparing(JobAnalysisSession::getMatchScore))
                .ifPresent(top -> {
                    progress.setLastMatchScore(top.getMatchScore());
                    progress.setLastMatchSummary(top.getSummary());
                });

        trainingProgressRepository.save(progress);
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}

