package ee.kerrete.ainterview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.auth.dto.request.LoginRequest;
import ee.kerrete.ainterview.model.AppUser;
import ee.kerrete.ainterview.model.UserRole;
import ee.kerrete.ainterview.repository.AppUserRepository;
import ee.kerrete.ainterview.service.AiEvaluationClient;
import ee.kerrete.ainterview.service.OpenAiClient;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected AppUserRepository appUserRepository;

    @MockBean
    protected OpenAiClient openAiClient;

    @MockBean
    protected AiEvaluationClient aiEvaluationClient;

    @BeforeEach
    void setupBaseMocks() {
        appUserRepository.deleteAll();

        when(openAiClient.generateQuestionsFromCv(anyString(), anyInt(), anyInt()))
            .thenReturn(List.of(new ee.kerrete.ainterview.model.Question(
                "1",
                "Describe your favorite project.",
                "TECH",
                "JUNIOR"
            )));

        var evalResponse = new ee.kerrete.ainterview.dto.EvaluateAnswerResponse();
        evalResponse.setScore(80);
        evalResponse.setStrengths("Good structure");
        evalResponse.setWeaknesses("Add more detail");
        evalResponse.setSuggestions("Provide metrics");
        evalResponse.setFallback(false);
        when(aiEvaluationClient.evaluateStarAnswer(any())).thenReturn(evalResponse);
        Mockito.reset(aiEvaluationClient); // ensure we can override per test if needed
        when(aiEvaluationClient.evaluateStarAnswer(any())).thenReturn(evalResponse);
    }

    protected AppUser createUser(String email, String rawPassword, boolean enabled, UserRole role) {
        AppUser user = AppUser.builder()
            .email(email.toLowerCase())
            .password(passwordEncoder.encode(rawPassword))
            .fullName("Test User")
            .role(role)
            .enabled(enabled)
            .build();
        return appUserRepository.save(user);
    }

    protected String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode node = objectMapper.readTree(body);
        return node.path("token").asText();
    }
}

