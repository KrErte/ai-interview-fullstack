package ee.kerrete.ainterview.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.ai.AiUnavailableException;
import ee.kerrete.ainterview.repository.JobAnalysisSessionRepository;
import ee.kerrete.ainterview.repository.TrainingProgressRepository;
import ee.kerrete.ainterview.repository.TrainingTaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * FP-20: no-key mode is a configuration state, logged at INFO without a stack trace (not ERROR "OpenAI error");
 * a genuine AI failure is logged at WARN. Both still end as ai_unavailable (OpenAiCoachException).
 */
class SoftSkillsCoachLoggingTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(SoftSkillsCoachService.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void attach() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detach() {
        logger.detachAppender(appender);
    }

    private SoftSkillsCoachService service(String key) {
        SoftSkillsCoachService s = new SoftSkillsCoachService(new ObjectMapper(), mock(TrainingTaskRepository.class),
                mock(TrainingProgressRepository.class), mock(SoftSkillSeedService.class),
                mock(JobAnalysisSessionRepository.class));
        ReflectionTestUtils.setField(s, "apiKey", key);
        ReflectionTestUtils.setField(s, "baseUrl", "https://openai.invalid/v1");
        ReflectionTestUtils.setField(s, "model", "m");
        return s;
    }

    private Object callOpenAi(SoftSkillsCoachService s) {
        return ReflectionTestUtils.invokeMethod(s, "callOpenAi", "system", "user");
    }

    @Test
    void noKeyModeLogsInfoWithoutStackTraceAndMakesNoCall() {
        try (MockedConstruction<RestTemplate> rts = mockConstruction(RestTemplate.class)) {
            SoftSkillsCoachService s = service("");

            assertThatThrownBy(() -> callOpenAi(s)).isInstanceOf(AiUnavailableException.class);

            rts.constructed().forEach(rt -> verifyNoInteractions(rt));
        }
        assertThat(appender.list).noneMatch(e -> e.getLevel().isGreaterOrEqual(Level.WARN));
        assertThat(appender.list).anySatisfy(e -> {
            assertThat(e.getLevel()).isEqualTo(Level.INFO);
            assertThat(e.getFormattedMessage()).contains("no-key mode");
            assertThat(e.getThrowableProxy()).isNull();
        });
    }

    @Test
    void genuineAiFailureLogsWarnNotError() {
        try (MockedConstruction<RestTemplate> ignored = mockConstruction(RestTemplate.class, (rt, ctx) ->
                when(rt.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                        .thenThrow(new ResourceAccessException("connect timed out")))) {
            SoftSkillsCoachService s = service("sk-test");

            assertThatThrownBy(() -> callOpenAi(s)).isInstanceOf(AiUnavailableException.class);
        }
        assertThat(appender.list).noneMatch(e -> e.getLevel() == Level.ERROR);
        assertThat(appender.list).anySatisfy(e -> {
            assertThat(e.getLevel()).isEqualTo(Level.WARN);
            assertThat(e.getFormattedMessage()).contains("OpenAI call failed");
        });
    }
}
