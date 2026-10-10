package ee.kerrete.ainterview.config;

import ee.kerrete.ainterview.model.AppUser;
import ee.kerrete.ainterview.model.UserProfile;
import ee.kerrete.ainterview.model.UserRole;
import ee.kerrete.ainterview.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FP-19: ProdDataSeeder takes credentials only from configuration (SEED_* env vars) and never logs a password.
 */
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ProdDataSeederTest {

    private static final String ADMIN_EMAIL = "ops-admin@futureproof.test";
    private static final String ADMIN_PASSWORD = "Adm1n-Secret-From-Env!";
    private static final String DEMO_EMAIL = "demo-user@futureproof.test";
    private static final String DEMO_PASSWORD = "Dem0-Secret-From-Env!";

    @Mock AppUserRepository appUserRepository;
    @Mock UserProfileRepository userProfileRepository;
    @Mock CvSummaryRepository cvSummaryRepository;
    @Mock JobAnalysisSessionRepository jobAnalysisSessionRepository;
    @Mock RoadmapTaskRepository roadmapTaskRepository;
    @Mock TrainingTaskRepository trainingTaskRepository;
    @Mock TrainingProgressRepository trainingProgressRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private ProdDataSeeder seeder(String adminEmail, String adminPassword, String demoEmail, String demoPassword) {
        return new ProdDataSeeder(appUserRepository, userProfileRepository, cvSummaryRepository,
                jobAnalysisSessionRepository, roadmapTaskRepository, trainingTaskRepository,
                trainingProgressRepository, passwordEncoder, adminEmail, adminPassword, demoEmail, demoPassword);
    }

    @BeforeEach
    void lenientDefaults() {
        lenient().when(appUserRepository.findByEmail(any())).thenReturn(Optional.empty());
    }

    @Test
    void noEnv_createsNoUsersAndNoDemoData(CapturedOutput output) {
        seeder("", "", "", "").run();

        verify(appUserRepository, never()).save(any());
        verifyNoInteractions(userProfileRepository, cvSummaryRepository, jobAnalysisSessionRepository,
                roadmapTaskRepository, trainingTaskRepository, trainingProgressRepository);
        assertThat(output.getAll()).contains("Admin seeding skipped").contains("Demo seeding skipped");
        assertThat(output.getAll()).doesNotContainIgnoringCase("password=");
    }

    @Test
    void emailWithoutPassword_createsNoUser() {
        seeder(ADMIN_EMAIL, " ", DEMO_EMAIL, null).run();

        verify(appUserRepository, never()).save(any());
        verifyNoInteractions(userProfileRepository);
    }

    @Test
    void envSet_createsAdminAndDemoUserWithEncodedPasswords(CapturedOutput output) {
        seeder(ADMIN_EMAIL, ADMIN_PASSWORD, DEMO_EMAIL, DEMO_PASSWORD).run();

        ArgumentCaptor<AppUser> users = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository, times(2)).save(users.capture());
        List<AppUser> saved = users.getAllValues();

        AppUser admin = saved.stream().filter(u -> ADMIN_EMAIL.equals(u.getEmail())).findFirst().orElseThrow();
        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(admin.getPassword()).isNotEqualTo(ADMIN_PASSWORD);
        assertThat(passwordEncoder.matches(ADMIN_PASSWORD, admin.getPassword())).isTrue();

        AppUser demo = saved.stream().filter(u -> DEMO_EMAIL.equals(u.getEmail())).findFirst().orElseThrow();
        assertThat(demo.getRole()).isEqualTo(UserRole.USER);
        assertThat(passwordEncoder.matches(DEMO_PASSWORD, demo.getPassword())).isTrue();

        ArgumentCaptor<UserProfile> profile = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(profile.capture());
        assertThat(profile.getValue().getEmail()).isEqualTo(DEMO_EMAIL);

        assertThat(output.getAll()).contains("Seeded admin user " + ADMIN_EMAIL).contains("Seeded demo user " + DEMO_EMAIL);
    }

    @Test
    void passwordsAreNeverLogged(CapturedOutput output) {
        seeder(ADMIN_EMAIL, ADMIN_PASSWORD, DEMO_EMAIL, DEMO_PASSWORD).run();

        assertThat(output.getAll())
                .doesNotContain(ADMIN_PASSWORD)
                .doesNotContain(DEMO_PASSWORD)
                .doesNotContainIgnoringCase("password=");
    }

    @Test
    void existingAdmin_keepsItsPassword() {
        AppUser existing = AppUser.builder()
                .email(ADMIN_EMAIL).password("existing-hash").role(UserRole.USER).enabled(false)
                .createdAt(LocalDateTime.now()).build();
        when(appUserRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(existing));

        seeder(ADMIN_EMAIL, ADMIN_PASSWORD, "", "").run();

        verify(appUserRepository).save(existing);
        assertThat(existing.getPassword()).isEqualTo("existing-hash");
        assertThat(existing.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(existing.isEnabled()).isTrue();
    }
}
