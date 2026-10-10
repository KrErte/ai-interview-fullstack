package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.CandidatePlanRequest;
import ee.kerrete.ainterview.model.LearningPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.lenient;

/** FP-24: the last candidate plan is stored per user, never globally. */
@ExtendWith(MockitoExtension.class)
class CandidatePlanServiceTest {

    @Mock
    private OpenAiClient openAiClient;

    private static CandidatePlanRequest request(String cv, String goal) {
        CandidatePlanRequest r = new CandidatePlanRequest();
        r.setCvText(cv);
        r.setGoal(goal);
        return r;
    }

    private static String planJson(String headline) {
        return "{\"headline\":\"" + headline + "\",\"summary\":\"s\",\"steps\":[\"one\"]}";
    }

    @Test
    void plansAreKeptPerUser() {
        CandidatePlanService service = new CandidatePlanService(openAiClient);
        lenient().when(openAiClient.createChatCompletion(anyString(), contains("CV of Alice"))).thenReturn(planJson("Alice plan"));
        lenient().when(openAiClient.createChatCompletion(anyString(), contains("CV of Bob"))).thenReturn(planJson("Bob plan"));

        service.buildPlan("alice@example.com", request("CV of Alice, phone +372 5555 0000", "Java"));
        service.buildPlan("bob@example.com", request("CV of Bob", "Angular"));

        assertThat(service.getLastPlan("alice@example.com").getHeadline()).isEqualTo("Alice plan");
        assertThat(service.getLastPlan("bob@example.com").getHeadline()).isEqualTo("Bob plan");
        assertThat(service.getLastPlan("carol@example.com")).isNull();
    }

    @Test
    void emailKeyIsExactNotNormalized() {
        CandidatePlanService service = new CandidatePlanService(openAiClient);
        lenient().when(openAiClient.createChatCompletion(anyString(), anyString())).thenReturn(planJson("Alice plan"));

        service.buildPlan("A@x", request("cv", "goal"));

        assertThat(service.getLastPlan("A@x").getHeadline()).isEqualTo("Alice plan");
        assertThat(service.getLastPlan("a@x")).isNull();
        assertThat(service.getLastPlan(" a@x")).isNull();
    }

    @Test
    void newPlanReplacesOnlyThatUsersPlan() {
        CandidatePlanService service = new CandidatePlanService(openAiClient);
        lenient().when(openAiClient.createChatCompletion(anyString(), contains("first"))).thenReturn(planJson("A1"));
        lenient().when(openAiClient.createChatCompletion(anyString(), contains("second"))).thenReturn(planJson("A2"));
        lenient().when(openAiClient.createChatCompletion(anyString(), contains("bob"))).thenReturn(planJson("B1"));

        service.buildPlan("a@example.com", request("first", "g"));
        service.buildPlan("b@example.com", request("bob", "g"));
        service.buildPlan("a@example.com", request("second", "g"));

        assertThat(service.getLastPlan("a@example.com").getHeadline()).isEqualTo("A2");
        assertThat(service.getLastPlan("b@example.com").getHeadline()).isEqualTo("B1");
    }

    @Test
    void fallbackPlanIsStoredPerUserToo() {
        CandidatePlanService service = new CandidatePlanService(openAiClient);
        lenient().when(openAiClient.createChatCompletion(anyString(), anyString())).thenReturn("not json");

        LearningPlan fallback = service.buildPlan("a@example.com", request("cv", "goal"));

        assertThat(service.getLastPlan("a@example.com")).isSameAs(fallback);
        assertThat(service.getLastPlan("b@example.com")).isNull();
    }

    @Test
    void blankEmailIsRejected() {
        CandidatePlanService service = new CandidatePlanService(openAiClient);
        assertThatThrownBy(() -> service.getLastPlan(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.buildPlan(null, request("cv", "g"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void concurrentUsersDoNotMixPlans() throws Exception {
        CandidatePlanService service = new CandidatePlanService(openAiClient);
        lenient().when(openAiClient.createChatCompletion(anyString(), anyString()))
                .thenAnswer(inv -> planJson("plan-for-" + ((String) inv.getArgument(1)).lines()
                        .filter(l -> l.startsWith("user-")).findFirst().orElseThrow()));

        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                int n = i % 20;
                futures.add(pool.submit(() -> service.buildPlan("user-" + n + "@example.com",
                        request("user-" + n + "@example.com", "g"))));
            }
            for (Future<?> f : futures) f.get();
        } finally {
            pool.shutdown();
        }

        for (int n = 0; n < 20; n++) {
            assertThat(service.getLastPlan("user-" + n + "@example.com").getHeadline())
                    .isEqualTo("plan-for-user-" + n + "@example.com");
        }
    }
}
