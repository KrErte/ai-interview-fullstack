package ee.kerrete.ainterview.ai;

import ee.kerrete.ainterview.service.AIExampleAnswerService;
import ee.kerrete.ainterview.service.AiAdaptiveService;
import ee.kerrete.ainterview.service.AiEvaluationClient;
import ee.kerrete.ainterview.service.JobAnalysisService;
import ee.kerrete.ainterview.service.SoftSkillQuestionService;
import ee.kerrete.ainterview.service.SoftSkillsCoachService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FP-20: an empty, blank or placeholder OpenAI key means "no key" (no outbound call, 503 ai_unavailable),
 * and the configuration no longer defaults the key to a placeholder.
 */
class OpenAiKeyTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "dummy-openai-key", " DUMMY-OPENAI-KEY ", "changeme", "your-openai-api-key",
            "YOUR_OPENAI_API_KEY", "placeholder", "sk-xxx", "${OPENAI_API_KEY}", "<your key>"})
    void emptyBlankOrPlaceholderKeyIsNotConfigured(String key) {
        assertThat(OpenAiKey.isConfigured(key)).isFalse();
        assertThatThrownBy(() -> OpenAiKey.require(key)).isInstanceOf(AiUnavailableException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"sk-test", "sk-proj-abc123", "sk-live-1234567890"})
    void realLookingKeyIsConfigured(String key) {
        assertThat(OpenAiKey.isConfigured(key)).isTrue();
        assertThatCode(() -> OpenAiKey.require(key)).doesNotThrowAnyException();
    }

    @Test
    void applicationYmlDefaultsTheKeyToEmpty() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/application.yml")) {
            String yml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(yml).contains("api-key: ${OPENAI_API_KEY:}").doesNotContain("dummy-openai-key");
        }
    }

    @Test
    void noValueAnnotationDefaultsTheKeyToAPlaceholder() {
        List<Class<?>> classes = List.of(AIExampleAnswerService.class, AiAdaptiveService.class, AiEvaluationClient.class,
                SoftSkillQuestionService.class, SoftSkillsCoachService.class, JobAnalysisService.class);
        int checked = 0;
        for (Class<?> c : classes) {
            for (Field f : c.getDeclaredFields()) {
                Value v = f.getAnnotation(Value.class);
                if (v != null && v.value().contains("api-key")) {
                    assertThat(v.value()).as(c.getSimpleName() + "." + f.getName()).isEqualTo("${openai.api-key:}");
                    checked++;
                }
            }
        }
        assertThat(checked).isEqualTo(classes.size());
    }
}
