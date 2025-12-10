package ee.kerrete.ainterview.config;

import ee.kerrete.ainterview.model.AppUser;
import ee.kerrete.ainterview.model.JobAnalysisSession;
import ee.kerrete.ainterview.model.TrainingProgress;
import ee.kerrete.ainterview.model.TrainingStatus;
import ee.kerrete.ainterview.model.TrainingTask;
import ee.kerrete.ainterview.model.UserRole;
import ee.kerrete.ainterview.model.WorkstyleSession;
import ee.kerrete.ainterview.repository.AppUserRepository;
import ee.kerrete.ainterview.repository.JobAnalysisSessionRepository;
import ee.kerrete.ainterview.repository.TrainingProgressRepository;
import ee.kerrete.ainterview.repository.TrainingTaskRepository;
import ee.kerrete.ainterview.repository.WorkstyleSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
@Profile({"local", "dev"})
@RequiredArgsConstructor
// Local dev demo users: admin@local.test / Admin123!, user@local.test / User123!
public class DataSeeder implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@local.test";
    private static final String USER_EMAIL = "user@local.test";
    private static final String ADMIN_PASSWORD = "Admin123!";
    private static final String USER_PASSWORD = "User123!";


    private final AppUserRepository appUserRepository;
    private final TrainingTaskRepository trainingTaskRepository;
    private final TrainingProgressRepository trainingProgressRepository;
    private final JobAnalysisSessionRepository jobAnalysisSessionRepository;
    private final WorkstyleSessionRepository workstyleSessionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        LocalDateTime now = LocalDateTime.now();

        seedAppUser(ADMIN_EMAIL, ADMIN_PASSWORD, "Local Admin", UserRole.ADMIN, now);
        seedAppUser(USER_EMAIL, USER_PASSWORD, "Local User", UserRole.USER, now);

        seedTrainingTasks(USER_EMAIL, now);

        seedTrainingProgressIfMissing(USER_EMAIL, now);
        seedTrainingProgressIfMissing(ADMIN_EMAIL, now);

        seedJobAnalysisSessionIfMissing(USER_EMAIL, now);
        seedJobAnalysisSessionIfMissing(ADMIN_EMAIL, now);

        seedWorkstyleSessionIfMissing(USER_EMAIL, now);

        log.info("Demo admin user ready: email={} password={}", ADMIN_EMAIL, ADMIN_PASSWORD);
        log.info("Demo normal user ready: email={} password={}", USER_EMAIL, USER_PASSWORD);
    }

    private void seedAppUser(String email, String rawPassword, String fullName, UserRole role, LocalDateTime now) {
        Optional<AppUser> existingUser = appUserRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            AppUser user = existingUser.get();
            boolean updated = false;

            if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(rawPassword));
                updated = true;
            }
            if (user.getRole() != role) {
                user.setRole(role);
                updated = true;
            }
            if (!user.isEnabled()) {
                user.setEnabled(true);
                updated = true;
            }
            if (user.getFullName() == null || user.getFullName().isBlank()) {
                user.setFullName(fullName);
                updated = true;
            }

            if (updated) {
                user.setUpdatedAt(now);
                appUserRepository.save(user);
                log.info("Updated seeded user {}", email);
            } else {
                log.debug("Seed user {} already up-to-date", email);
            }
            return;
        }

        AppUser user = AppUser.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .fullName(fullName)
                .role(role)
                .enabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        appUserRepository.save(Objects.requireNonNull(user));
        log.info("Seeded default user {}", email);
    }

    private void seedTrainingTasks(String email, LocalDateTime now) {
        List<TrainingTask> tasks = List.of(
                buildTask(email, "Sample training question 1", "Sample training answer 1", now, "demo-task-1"),
                buildTask(email, "Sample training question 2", "Sample training answer 2", now, "demo-task-2"),
                buildTask(email, "Sample training question 3", "Sample training answer 3", now, "demo-task-3")
        );

        tasks.forEach(task -> {
            if (trainingTaskRepository.existsByEmailAndQuestion(email, task.getQuestion())) {
                return;
            }
            trainingTaskRepository.save(Objects.requireNonNull(task));
        });
    }

    private TrainingTask buildTask(String email, String question, String answer, LocalDateTime timestamp, String taskKey) {
        return TrainingTask.builder()
                .email(email)
                .question(question)
                .answer(answer)
                .score(null)
                .taskKey(taskKey)
                .completed(false)
                .createdAt(timestamp)
                .updatedAt(timestamp)
                .scoreUpdated(timestamp)
                .build();
    }

    private void seedTrainingProgressIfMissing(String email, LocalDateTime now) {
        if (trainingProgressRepository.existsByEmail(email)) {
            return;
        }

        int totalTasks = (int) trainingTaskRepository.countByEmail(email);

        TrainingProgress tp = TrainingProgress.builder()
                .email(email)
                .totalTasks(totalTasks)
                .completedTasks(0)
                .totalJobAnalyses(0)
                .totalTrainingSessions(0)
                .trainingProgressPercent(0)
                .status(TrainingStatus.NOT_STARTED)
                .lastActivityAt(now)
                .lastUpdated(now)
                .lastMatchScore(0.0)
                .lastMatchSummary("Seeded placeholder")
                .build();

        trainingProgressRepository.save(Objects.requireNonNull(tp));
    }

    private void seedJobAnalysisSessionIfMissing(String email, LocalDateTime now) {
        if (jobAnalysisSessionRepository.existsByEmail(email)) {
            return;
        }

        JobAnalysisSession session = JobAnalysisSession.builder()
                .email(email)
                .userEmail(email)
                .jobTitle("Sample job for " + email)
                .jobDescription("Placeholder description for seeded local data.")
                .analysisResult("N/A")
                .missingSkillsJson("[]")
                .roadmapJson("[]")
                .suggestedImprovementsJson("[]")
                .aiSummary("Seeded summary")
                .aiScore(75)
                .matchScore(0.0)
                .summary("Seeded job analysis session")
                .createdAt(now)
                .build();

        jobAnalysisSessionRepository.save(Objects.requireNonNull(session));
    }

    private void seedWorkstyleSessionIfMissing(String email, LocalDateTime now) {
        WorkstyleSession existing = workstyleSessionRepository.findByEmailAndCompletedFalse(email);
        if (existing != null) {
            return;
        }

        WorkstyleSession session = new WorkstyleSession();
        session.setEmail(email);
        session.setCurrentQuestion("Seeded workstyle question");
        session.setAnswersJson("{}");
        session.setCompleted(false);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);

        workstyleSessionRepository.save(Objects.requireNonNull(session));
    }
}

