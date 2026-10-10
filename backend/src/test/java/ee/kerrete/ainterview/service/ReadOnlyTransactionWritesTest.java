package ee.kerrete.ainterview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.dto.JobMatchRequest;
import ee.kerrete.ainterview.model.UserProfile;
import ee.kerrete.ainterview.repository.TrainingProgressRepository;
import ee.kerrete.ainterview.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FP-26: H2 ignores read-only transactions, PostgreSQL rejects writes in them with
 * "cannot execute INSERT in a read-only transaction". This test wraps the DataSource so that, like
 * PostgreSQL, a connection marked read-only (Spring does that for @Transactional(readOnly = true))
 * refuses INSERT/UPDATE/DELETE. Every service method that writes must therefore not be read-only.
 * <p>
 * The test methods must not run inside a test-managed transaction: the service methods would join it
 * and their own readOnly flag would never be applied.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {
        // NON_KEYWORDS + default upper-casing: lets H2 accept the unquoted reserved column current_role that
        // main still maps (fixed separately by FP-16, which renames it to current_job_role).
        "spring.datasource.url=jdbc:h2:mem:fp26_readonly;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;NON_KEYWORDS=CURRENT_ROLE",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@Import({UserProfileService.class, JobMatchService.class, DashboardService.class, CvSummaryService.class,
        TrainingService.class, UserProgressService.class,
        ReadOnlyTransactionWritesTest.PostgresLikeReadOnly.class})
class ReadOnlyTransactionWritesTest {

    @Autowired UserProfileService userProfileService;
    @Autowired JobMatchService jobMatchService;
    @Autowired DashboardService dashboardService;
    @Autowired CvSummaryService cvSummaryService;
    @Autowired TrainingService trainingService;
    @Autowired UserProgressService userProgressService;
    @Autowired UserProfileRepository userProfileRepository;
    @Autowired TrainingProgressRepository trainingProgressRepository;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void harnessRejectsWritesInReadOnlyTransactionLikePostgres() {
        TransactionTemplate readOnly = new TransactionTemplate(transactionManager);
        readOnly.setReadOnly(true);

        assertThatThrownBy(() -> readOnly.executeWithoutResult(status ->
                userProfileRepository.saveAndFlush(UserProfile.builder().email("harness@fp26.test").build())))
                .hasStackTraceContaining("read-only transaction");
    }

    @Test
    void getProfileCreatesProfileForNewUser() {
        assertThat(userProfileService.getProfile("profile@fp26.test").getEmail()).isEqualTo("profile@fp26.test");
        assertThat(userProfileRepository.findByEmail("profile@fp26.test")).isPresent();
    }

    @Test
    void jobMatchSavesLastMatchToTrainingProgress() {
        JobMatchRequest request = new JobMatchRequest();
        request.setEmail("match@fp26.test");

        assertThat(jobMatchService.match(request)).isNotEmpty();
        assertThat(trainingProgressRepository.findByEmail("match@fp26.test"))
                .hasValueSatisfying(p -> assertThat(p.getLastMatchScore()).isNotNull());
    }

    @Test
    void dashboardWorksForNewUser() {
        assertThat(dashboardService.get("dashboard@fp26.test").getEmail()).isEqualTo("dashboard@fp26.test");
        assertThat(userProfileRepository.findByEmail("dashboard@fp26.test")).isPresent();
    }

    @Test
    void trainingProgressIsSaved() {
        trainingService.getProgress("training@fp26.test");
        trainingService.getTrainingProgress("training2@fp26.test");

        assertThat(trainingProgressRepository.findByEmail("training@fp26.test")).isPresent();
        assertThat(trainingProgressRepository.findByEmail("training2@fp26.test")).isPresent();
    }

    @Test
    void remainingReadOnlyMethodsOnlyRead() {
        assertThat(userProgressService.getUserProgress("reader@fp26.test").getEmail()).isEqualTo("reader@fp26.test");
        assertThat(userProgressService.getProgress("reader@fp26.test")).isNotNull();
        assertThat(cvSummaryService.findByEmail("reader@fp26.test")).isEmpty();
    }

    @TestConfiguration
    static class PostgresLikeReadOnly {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        static BeanPostProcessor readOnlyEnforcingDataSource() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if (bean instanceof DataSource dataSource) {
                        return Proxy.newProxyInstance(DataSource.class.getClassLoader(),
                                new Class<?>[]{DataSource.class}, (proxy, method, args) -> {
                                    Object result = invoke(dataSource, method, args);
                                    return result instanceof Connection c ? enforcing(c) : result;
                                });
                    }
                    return bean;
                }
            };
        }

        private static Connection enforcing(Connection target) {
            AtomicBoolean readOnly = new AtomicBoolean(false);
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "setReadOnly" -> readOnly.set((Boolean) args[0]);
                            case "isReadOnly" -> {
                                return readOnly.get();
                            }
                            case "prepareStatement" -> {
                                if (readOnly.get() && args != null && args[0] instanceof String sql) {
                                    String verb = sql.stripLeading().split("\\s+", 2)[0].toUpperCase(Locale.ROOT);
                                    if (verb.equals("INSERT") || verb.equals("UPDATE") || verb.equals("DELETE")) {
                                        throw new SQLException("ERROR: cannot execute " + verb
                                                + " in a read-only transaction", "25006");
                                    }
                                }
                            }
                            default -> { }
                        }
                        return invoke(target, method, args);
                    });
        }

        private static Object invoke(Object target, java.lang.reflect.Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
    }
}
