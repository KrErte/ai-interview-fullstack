package ee.kerrete.ainterview.config;

import ee.kerrete.ainterview.model.*;
import ee.kerrete.ainterview.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Seeds realistic demo data for development and testing.
 * Uses @EventListener(ApplicationReadyEvent.class) with @Order(100)
 * to ensure it runs after DataSeeder (which uses CommandLineRunner).
 */
@Slf4j
@Component
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class DemoDataSeeder {

    private static final List<String> USER_EMAILS = List.of(
            "admin@local.test",
            "user@local.test",
            "test@mentor.ee"
    );

    private final AppUserRepository appUserRepository;
    private final CvSummaryRepository cvSummaryRepository;
    private final JobAnalysisSessionRepository jobAnalysisSessionRepository;
    private final RoadmapTaskRepository roadmapTaskRepository;
    private final TrainingTaskRepository trainingTaskRepository;
    private final UserProfileRepository userProfileRepository;
    private final TrainingProgressRepository trainingProgressRepository;

    private final Random random = new Random(42); // Fixed seed for reproducibility

    @EventListener(ApplicationReadyEvent.class)
    @Order(100)
    public void seedDemoData() {
        log.info("Starting DemoDataSeeder...");

        for (String email : USER_EMAILS) {
            if (appUserRepository.findByEmail(email).isEmpty()) {
                log.warn("User {} not found, skipping demo data", email);
                continue;
            }

            seedCvSummary(email);
            seedJobAnalysisSessions(email);
            seedRoadmapTasks(email);
            seedTrainingTasks(email);
            seedUserProfileSafe(email);
            updateTrainingProgress(email);
        }

        log.info("DemoDataSeeder completed.");
    }

    private void seedCvSummary(String email) {
        if (cvSummaryRepository.existsByEmail(email)) {
            log.debug("CV summary already exists for {}", email);
            return;
        }

        String fullName = getFullNameForEmail(email);
        String role = getRoleForEmail(email);

        String rawCvText = generateRealisticCvText(fullName, role, email);
        String skills = generateSkillsJson(role);
        String headline = role + " | " + fullName;
        String experienceSummary = generateExperienceSummary(role);

        CvSummary cv = CvSummary.builder()
                .email(email)
                .headline(headline)
                .parsedSkills(skills)
                .experienceSummary(experienceSummary)
                .rawText(rawCvText)
                .build();

        cvSummaryRepository.save(cv);
        log.info("Seeded CV summary for {}", email);
    }

    private void seedJobAnalysisSessions(String email) {
        List<JobAnalysisSession> existing = jobAnalysisSessionRepository.findByEmail(email);
        Map<String, JobAnalysisSession> existingByTitle = existing.stream()
                .collect(Collectors.toMap(JobAnalysisSession::getJobTitle, j -> j, (a, b) -> a));

        List<JobPosting> jobs = getRealisticJobPostings();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(28);
        int target = 8;
        int created = 0;

        for (int i = 0; i < target; i++) {
            JobPosting job = jobs.get(i % jobs.size());
            if (existingByTitle.containsKey(job.title())) {
                continue;
            }
            double matchScore = 65 + random.nextDouble() * 30; // 65-95%
            LocalDateTime createdAt = baseTime.plusDays(i * 2L);

            JobAnalysisSession session = JobAnalysisSession.builder()
                    .email(email)
                    .userEmail(email)
                    .jobTitle(job.title())
                    .jobDescription(job.description())
                    .analysisResult(generateAnalysisResult(job.title(), matchScore))
                    .missingSkillsJson(generateMissingSkillsJson(job.title()))
                    .strengthsJson(generateStrengthsJson())
                    .weaknessesJson(generateWeaknessesJson())
                    .roadmapJson(generateRoadmapJson(job.title()))
                    .suggestedImprovementsJson(generateSuggestedImprovementsJson())
                    .aiSummary(generateAiSummary(job.title(), matchScore))
                    .aiScore((int) Math.round(matchScore))
                    .matchScore(matchScore)
                    .summary(String.format("Analysis for %s - %.0f%% match", job.title(), matchScore))
                    .createdAt(createdAt)
                    .build();

            jobAnalysisSessionRepository.save(session);
            created++;
        }

        log.info("Seeded {} job analysis sessions for {}", created, email);
    }

    private void seedRoadmapTasks(String email) {
        List<RoadmapTaskTemplate> templates = getRoadmapTaskTemplates();
        int target = Math.min(templates.size(), 18);
        long existing = roadmapTaskRepository.countByEmail(email);
        if (existing >= target) {
            log.debug("Roadmap tasks already at target for {}", email);
            return;
        }

        int created = 0;
        for (int i = 0; i < target; i++) {
            RoadmapTaskTemplate template = templates.get(i);
            if (roadmapTaskRepository.findByEmailAndTaskKey(email, template.key).isPresent()) {
                continue;
            }
            boolean completed = i < 6; // mix of DONE and TODO

            RoadmapTask task = RoadmapTask.builder()
                    .email(email)
                    .taskKey(template.key)
                    .title(template.title)
                    .description(template.description)
                    .dayNumber(template.dayNumber)
                    .orderIndex(i + 1)
                    .completed(completed)
                    .build();

            roadmapTaskRepository.save(task);
            created++;
        }

        log.info("Seeded {} roadmap tasks for {}", created, email);
    }

    private void seedTrainingTasks(String email) {
        List<TrainingTaskSeed> seeds = getTrainingTaskSeeds();
        int created = 0;
        for (int i = 0; i < seeds.size(); i++) {
            TrainingTaskSeed seed = seeds.get(i);
            if (trainingTaskRepository.findByEmailAndTaskKey(email, seed.taskKey()).isPresent()) {
                continue;
            }
            LocalDateTime createdAt = LocalDateTime.now().minusDays(seeds.size() - i).truncatedTo(ChronoUnit.SECONDS);
            TrainingTask task = TrainingTask.builder()
                    .email(email)
                    .taskKey(seed.taskKey())
                    .skillKey(seed.skillKey())
                    .question(seed.question())
                    .answer(seed.answer())
                    .feedback(seed.feedback())
                    .score(seed.score())
                    .completed(seed.completed())
                    .createdAt(createdAt)
                    .updatedAt(createdAt.plusHours(4))
                    .scoreUpdated(createdAt.plusHours(4))
                    .build();
            trainingTaskRepository.save(task);
            created++;
        }
        if (created > 0) {
            log.info("Seeded {} training tasks for {}", created, email);
        }
    }

    /**
     * Seeds user profile with safe handling for H2 @Lob column issues.
     * The bio and skills fields use @Lob which can cause issues in H2 PostgreSQL mode.
     * This method catches and logs any errors, allowing the seeder to continue.
     */
    private void seedUserProfileSafe(String email) {
        try {
            if (userProfileRepository.existsByEmail(email)) {
                // Update existing profile with demo data
                userProfileRepository.findByEmail(email).ifPresent(profile -> {
                    if (profile.getTargetRole() == null || profile.getTargetRole().isEmpty()) {
                        updateProfileWithDemoData(profile, email);
                        userProfileRepository.save(profile);
                        log.info("Updated user profile for {}", email);
                    }
                });
                return;
            }

            String fullName = getFullNameForEmail(email);
            String role = getRoleForEmail(email);

            UserProfile profile = UserProfile.builder()
                    .email(email)
                    .fullName(fullName)
                    .currentRole(role)
                    .targetRole(getTargetRoleForEmail(email))
                    .skills(generateSkillsJson(role))
                    .bio(generateBio(fullName, role))
                    .yearsOfExperience(3 + random.nextInt(8))
                    .build();

            userProfileRepository.save(profile);
            log.info("Seeded user profile for {}", email);
        } catch (Exception e) {
            log.warn("Could not seed user profile for {} (likely @Lob column issue in H2): {}",
                    email, e.getMessage());
        }
    }

    private void updateProfileWithDemoData(UserProfile profile, String email) {
        String role = getRoleForEmail(email);
        profile.setTargetRole(getTargetRoleForEmail(email));
        profile.setSkills(generateSkillsJson(role));
        profile.setBio(generateBio(profile.getFullName(), role));
        profile.setYearsOfExperience(3 + random.nextInt(8));
    }

    private void updateTrainingProgress(String email) {
        trainingProgressRepository.findByEmail(email).ifPresent(progress -> {
            long completedTasks = roadmapTaskRepository.countByEmailAndCompletedIsTrue(email);
            long totalTasks = roadmapTaskRepository.countByEmail(email);
            long jobAnalyses = jobAnalysisSessionRepository.countByEmail(email);

            progress.setTotalTasks((int) totalTasks);
            progress.setCompletedTasks((int) completedTasks);
            progress.setTotalJobAnalyses((int) jobAnalyses);
            progress.setTotalTrainingSessions((int) (completedTasks + 2));

            int progressPercent = totalTasks > 0 ? (int) ((completedTasks * 100) / totalTasks) : 0;
            progress.setTrainingProgressPercent(progressPercent);

            if (completedTasks == 0) {
                progress.setStatus(TrainingStatus.NOT_STARTED);
            } else if (completedTasks < totalTasks) {
                progress.setStatus(TrainingStatus.IN_PROGRESS);
            } else {
                progress.setStatus(TrainingStatus.COMPLETED);
            }

            double matchScore = 65 + random.nextDouble() * 25;
            progress.setLastMatchScore(matchScore);
            progress.setLastMatchSummary(String.format("Good progress! Current match score: %.0f%%", matchScore));
            progress.setLastActivityAt(LocalDateTime.now());
            progress.setLastUpdated(LocalDateTime.now());

            trainingProgressRepository.save(progress);
            log.info("Updated training progress for {}", email);
        });
    }

    // ==================== Helper Methods ====================

    private String getFullNameForEmail(String email) {
        return switch (email) {
            case "admin@local.test" -> "Alex Administrator";
            case "user@local.test" -> "Sam Developer";
            case "test@mentor.ee" -> "Test Kasutaja";
            default -> "Demo User";
        };
    }

    private String getRoleForEmail(String email) {
        return switch (email) {
            case "admin@local.test" -> "Senior Software Engineer";
            case "user@local.test" -> "Full Stack Developer";
            case "test@mentor.ee" -> "Junior Developer";
            default -> "Software Developer";
        };
    }

    private String getTargetRoleForEmail(String email) {
        return switch (email) {
            case "admin@local.test" -> "Engineering Manager";
            case "user@local.test" -> "Senior Full Stack Developer";
            case "test@mentor.ee" -> "Mid-Level Developer";
            default -> "Senior Developer";
        };
    }

    private String generateRealisticCvText(String fullName, String role, String email) {
        return String.format("""
                %s
                %s
                Email: %s

                PROFESSIONAL SUMMARY
                Experienced software professional with a strong background in building scalable web applications.
                Passionate about clean code, agile methodologies, and continuous learning.
                Proven track record of delivering high-quality solutions in fast-paced environments.

                TECHNICAL SKILLS
                • Programming Languages: Java, TypeScript, Python, SQL
                • Frameworks: Spring Boot, Angular, React, Node.js
                • Databases: PostgreSQL, MySQL, MongoDB, Redis
                • Cloud & DevOps: AWS, Docker, Kubernetes, CI/CD
                • Tools: Git, Jira, IntelliJ IDEA, VS Code

                WORK EXPERIENCE

                %s
                TechCorp Solutions | 2021 - Present
                • Designed and implemented RESTful APIs serving 10,000+ daily users
                • Led migration from monolithic architecture to microservices
                • Mentored junior developers and conducted code reviews
                • Reduced deployment time by 60%% through CI/CD pipeline improvements

                Software Developer
                StartupXYZ | 2019 - 2021
                • Built customer-facing web applications using Angular and Spring Boot
                • Implemented automated testing, achieving 85%% code coverage
                • Collaborated with product team to define technical requirements
                • Optimized database queries, improving response times by 40%%

                EDUCATION
                Bachelor of Science in Computer Science
                University of Technology | 2015 - 2019

                CERTIFICATIONS
                • AWS Certified Developer - Associate
                • Oracle Certified Professional, Java SE 11 Developer

                LANGUAGES
                • English (Fluent)
                • Estonian (Native)
                """, fullName, role, email, role);
    }

    private String generateSkillsJson(String role) {
        if (role.contains("Full Stack")) {
            return "[\"Java\", \"Spring Boot\", \"TypeScript\", \"Angular\", \"PostgreSQL\", \"Docker\", \"AWS\", \"Git\", \"REST APIs\", \"Agile\"]";
        } else if (role.contains("Senior")) {
            return "[\"Java\", \"Python\", \"System Design\", \"Microservices\", \"Kubernetes\", \"AWS\", \"Team Leadership\", \"Code Review\", \"CI/CD\", \"Architecture\"]";
        } else {
            return "[\"Java\", \"JavaScript\", \"HTML/CSS\", \"Git\", \"SQL\", \"Spring Boot\", \"React\", \"Unit Testing\", \"Agile\", \"Problem Solving\"]";
        }
    }

    private String generateExperienceSummary(String role) {
        return String.format("""
                Current Position: %s with 5+ years of experience in software development.

                Key Achievements:
                • Successfully delivered 15+ production applications
                • Led technical initiatives impacting 50,000+ users
                • Contributed to open-source projects with 500+ GitHub stars
                • Presented at 3 tech conferences on modern development practices

                Domain Expertise: E-commerce, FinTech, SaaS platforms
                """, role);
    }

    private List<JobPosting> getRealisticJobPostings() {
        List<JobPosting> jobs = new ArrayList<>();
        jobs.add(new JobPosting(
                "Senior Full Stack Developer (Berlin)",
                """
                Location: Berlin, Germany
                We are looking for a Senior Full Stack Developer to join our growing team.
                Stack: Java, Spring Boot, Angular, PostgreSQL, AWS, Docker, Kubernetes.
                """
        ));
        jobs.add(new JobPosting(
                "Backend Engineer - Platform Team (Remote EU)",
                """
                Location: Remote in EU time zones
                Build platform services with Java/Kotlin, Kafka, PostgreSQL, Terraform, AWS.
                """
        ));
        jobs.add(new JobPosting(
                "Technical Lead - Web Applications (Tallinn)",
                """
                Location: Tallinn, Estonia
                Lead a web team building React/Node/Spring services, mentor devs, drive architecture.
                """
        ));
        jobs.add(new JobPosting(
                "Site Reliability Engineer (Amsterdam)",
                """
                Location: Amsterdam, Netherlands
                Focus on observability, incident response, Kubernetes, and CI/CD hardening.
                """
        ));
        jobs.add(new JobPosting(
                "Data Engineer (London)",
                """
                Location: London, UK
                Own data pipelines with Spark, Kafka, Airflow; collaborate with analytics teams.
                """
        ));
        jobs.add(new JobPosting(
                "Cloud DevOps Engineer (Stockholm)",
                """
                Location: Stockholm, Sweden
                Improve AWS infrastructure, IaC with Terraform, and delivery pipelines.
                """
        ));
        jobs.add(new JobPosting(
                "Mobile Engineer (React Native) (Copenhagen)",
                """
                Location: Copenhagen, Denmark
                Deliver mobile features with React Native, TypeScript, and CI for mobile apps.
                """
        ));
        jobs.add(new JobPosting(
                "Security Engineer (Remote EU)",
                """
                Location: Remote EU
                Implement security controls, threat modeling, and secure SDLC practices.
                """
        ));
        jobs.add(new JobPosting(
                "Product Engineer (Paris)",
                """
                Location: Paris, France
                Ship end-to-end features with TypeScript/React/Node, UX collaboration, and testing.
                """
        ));
        jobs.add(new JobPosting(
                "AI Backend Engineer (Helsinki)",
                """
                Location: Helsinki, Finland
                Build AI-powered services with Java/Python, vector stores, and cloud infra.
                """
        ));
        return jobs;
    }

    private List<RoadmapTaskTemplate> getRoadmapTaskTemplates() {
        List<RoadmapTaskTemplate> tasks = new ArrayList<>();
        tasks.add(new RoadmapTaskTemplate("star_method_intro", "Master the STAR Method",
                "Practice STAR (Situation, Task, Action, Result) with 3 scenarios.", 1));
        tasks.add(new RoadmapTaskTemplate("technical_fundamentals", "Review Technical Fundamentals",
                "Refresh data structures, algorithms, and complexity basics.", 2));
        tasks.add(new RoadmapTaskTemplate("project_showcase", "Prepare Project Showcase",
                "Document top 3 projects with role, tech, challenges, and impact.", 3));
        tasks.add(new RoadmapTaskTemplate("mock_interview", "Complete Mock Interview",
                "Schedule a mock interview covering behavioral + technical.", 4));
        tasks.add(new RoadmapTaskTemplate("company_research", "Research Target Companies",
                "Research 5 target companies: stack, culture, news, questions.", 5));
        tasks.add(new RoadmapTaskTemplate("rest_api_design", "REST API Design Drills",
                "Design 2 APIs with resources, error handling, pagination, versioning.", 6));
        tasks.add(new RoadmapTaskTemplate("system_design_scaling", "System Design: Scaling",
                "Sketch a scalable service: load balancing, caching, queues, DB sharding.", 7));
        tasks.add(new RoadmapTaskTemplate("cloud_aws_basics", "Cloud Foundations (AWS)",
                "Hands-on with S3, EC2, RDS, IAM least privilege, and cost awareness.", 8));
        tasks.add(new RoadmapTaskTemplate("ci_cd_pipeline", "CI/CD Improvements",
                "Add build checks, tests, container scan, and blue/green deployment draft.", 9));
        tasks.add(new RoadmapTaskTemplate("observability_intro", "Observability Basics",
                "Add structured logging, metrics, tracing plan; define SLOs.", 10));
        tasks.add(new RoadmapTaskTemplate("testing_junit", "Testing Discipline",
                "Write unit + integration tests for a sample service; measure coverage.", 11));
        tasks.add(new RoadmapTaskTemplate("security_basics", "Secure Coding",
                "Review OWASP Top 10, add input validation and secrets handling checklist.", 12));
        tasks.add(new RoadmapTaskTemplate("database_tuning", "Database Performance",
                "Optimize queries, add indexes, measure with EXPLAIN, fix N+1.", 13));
        tasks.add(new RoadmapTaskTemplate("frontend_angular", "Angular Polish",
                "Improve accessibility, routing guards, and error handling in Angular.", 14));
        tasks.add(new RoadmapTaskTemplate("communication_storytelling", "Communication Practice",
                "Craft 3 concise STAR stories for leadership, conflict, delivery.", 15));
        tasks.add(new RoadmapTaskTemplate("leadership_growth", "Leadership & Ownership",
                "Define mentoring plan, delegation checklist, and risk tracking habit.", 16));
        tasks.add(new RoadmapTaskTemplate("ownership_mindset", "Ownership Mindset",
                "Pick a small product improvement, ship it end-to-end, document outcomes.", 17));
        tasks.add(new RoadmapTaskTemplate("mentoring_junior", "Mentor a Junior",
                "Pair-program 2 sessions, share feedback template, and learning plan.", 18));
        return tasks;
    }

    private List<TrainingTaskSeed> getTrainingTaskSeeds() {
        return List.of(
                new TrainingTaskSeed("demo-task-1", "self_reflection",
                        "Describe a challenging project and your personal impact.",
                        "I led a migration effort, coordinating stakeholders and delivering on time.",
                        "Clear articulation of impact; add metrics for stronger proof.",
                        78, true),
                new TrainingTaskSeed("trainer_q1", "conflict_management",
                        "Tell me about a conflict and how you resolved it.",
                        "Handled a priority clash by aligning PM and infra team on trade-offs.",
                        "Good resolution detail; mention timeline and metrics.", 82, true),
                new TrainingTaskSeed("rest_api_design", "backend",
                        "Design a REST API for scheduling interviews.",
                        "Outlined resources, auth, pagination, and idempotent mutations.",
                        "Add rate limits and idempotency keys for safety.", 76, true),
                new TrainingTaskSeed("system_design_scaling", "architecture",
                        "How would you scale a real-time collaboration app?",
                        "Proposed websockets, Redis pub/sub, sharded DB, and autoscaling.",
                        "Consider backpressure and chaos testing plan.", 74, true),
                new TrainingTaskSeed("cloud_aws_basics", "cloud",
                        "Explain how you secure an S3-backed file service.",
                        "Private buckets, presigned URLs, KMS encryption, and audit logs.",
                        "Add lifecycle policies and per-tenant buckets.", 80, true),
                new TrainingTaskSeed("ci_cd_pipeline", "devops",
                        "Describe a robust CI/CD pipeline for a Spring Boot service.",
                        "Lint, tests, SCA, Docker build, SBOM, staged deploy, health checks.",
                        "Include feature flags and rollout metrics.", 79, true),
                new TrainingTaskSeed("observability_intro", "devops",
                        "How do you add observability to a legacy service?",
                        "Structured logs, trace IDs, Prometheus metrics, basic alerts.",
                        "Add RUM/UX metrics and SLO error budget policy.", 72, false),
                new TrainingTaskSeed("testing_junit", "quality",
                        "What is your testing strategy for critical flows?",
                        "Unit + integration + contract tests; data fixtures; regression suite.",
                        "Note mutation testing and load testing for hotspots.", 70, false),
                new TrainingTaskSeed("security_basics", "security",
                        "How do you prevent OWASP Top 10 issues in a REST API?",
                        "Input validation, authZ, CSRF tokens, secrets vault, logging.",
                        "Add dependency scanning and SSRF controls.", 68, false),
                new TrainingTaskSeed("database_tuning", "data",
                        "Steps to optimize slow SQL queries?",
                        "Use EXPLAIN, indexes, caching, batching, connection pool tuning.",
                        "Add partitioning and deadlock monitoring.", 73, true),
                new TrainingTaskSeed("frontend_angular", "frontend",
                        "How to keep an Angular app resilient?",
                        "Guards, interceptors, error boundaries, lazy loading, a11y checklist.",
                        "Add perf budget and bundle analysis.", 69, false),
                new TrainingTaskSeed("communication_storytelling", "soft",
                        "Give me a concise project win story.",
                        "Delivered payment refactor saving 30% infra cost, aligned 3 teams.",
                        "Great brevity; add user impact metrics.", 85, true),
                new TrainingTaskSeed("leadership_growth", "soft",
                        "How do you mentor juniors effectively?",
                        "Weekly 1:1s, pairing, growth plans, code review coaching.",
                        "Include shadowing and feedback loops.", 77, true),
                new TrainingTaskSeed("ownership_mindset", "culture",
                        "Example of ownership beyond your scope?",
                        "Stabilized flaky CI across teams and documented playbooks.",
                        "Add incident follow-up metrics.", 81, true),
                new TrainingTaskSeed("mentoring_junior", "soft",
                        "Describe a time you unblocked a teammate.",
                        "Introduced feature flag, reduced scope, and paired to ship.",
                        "Add customer impact summary.", 78, true)
        );
    }

    private String generateAnalysisResult(String jobTitle, double matchScore) {
        return String.format("""
                {
                    "overallAssessment": "Strong candidate for %s position",
                    "matchScore": %.1f,
                    "keyFindings": [
                        "Technical skills align well with requirements",
                        "Relevant industry experience",
                        "Some gaps in specific technologies"
                    ]
                }
                """, jobTitle, matchScore);
    }

    private String generateMissingSkillsJson(String jobTitle) {
        if (jobTitle.contains("Technical Lead")) {
            return "[\"Team management experience\", \"Stakeholder communication\", \"Budget planning\"]";
        } else if (jobTitle.contains("Platform")) {
            return "[\"Kafka expertise\", \"Distributed tracing\", \"SRE practices\"]";
        }
        return "[\"GraphQL\", \"Terraform\", \"Performance optimization\"]";
    }

    private String generateStrengthsJson() {
        return "[\"Strong programming fundamentals\", \"Good problem-solving skills\", \"Team collaboration\", \"Continuous learning mindset\"]";
    }

    private String generateWeaknessesJson() {
        return "[\"Limited experience with specific tools\", \"Could improve system design skills\", \"Public speaking experience\"]";
    }

    private String generateRoadmapJson(String jobTitle) {
        return String.format("""
                [
                    {"week": 1, "focus": "Technical preparation", "tasks": ["Review %s requirements", "Practice coding"]},
                    {"week": 2, "focus": "Behavioral preparation", "tasks": ["STAR method practice", "Mock interviews"]},
                    {"week": 3, "focus": "Company research", "tasks": ["Study company culture", "Prepare questions"]}
                ]
                """, jobTitle);
    }

    private String generateSuggestedImprovementsJson() {
        return """
                [
                    "Gain hands-on experience with cloud-native technologies",
                    "Contribute to open-source projects to showcase skills",
                    "Practice system design interviews",
                    "Improve communication skills through presentations"
                ]
                """;
    }

    private String generateAiSummary(String jobTitle, double matchScore) {
        String assessment = matchScore >= 80 ? "excellent" : matchScore >= 70 ? "good" : "moderate";
        return String.format(
                "Based on your CV analysis, you show %s alignment with the %s position. " +
                "Your technical foundation is solid, and with focused preparation on the identified gaps, " +
                "you can significantly improve your chances. Focus on the recommended roadmap tasks.",
                assessment, jobTitle
        );
    }

    private String generateBio(String fullName, String role) {
        return String.format(
                "%s is a passionate %s with a focus on building scalable, user-centric applications. " +
                "With experience across the full development lifecycle, they bring strong technical skills " +
                "combined with excellent collaboration abilities. Outside of work, they enjoy contributing " +
                "to open-source projects and staying current with emerging technologies.",
                fullName, role
        );
    }

    // ==================== Inner Classes ====================

    private record JobPosting(String title, String description) {}

    private record RoadmapTaskTemplate(String key, String title, String description, int dayNumber) {}

    private record TrainingTaskSeed(
            String taskKey,
            String skillKey,
            String question,
            String answer,
            String feedback,
            Integer score,
            boolean completed
    ) {}
}
