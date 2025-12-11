package ee.kerrete.ainterview.softskills.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.auth.jwt.JwtAuthenticationFilter;
import ee.kerrete.ainterview.auth.jwt.JwtService;
import ee.kerrete.ainterview.softskills.dto.SoftSkillEvaluationRequest;
import ee.kerrete.ainterview.softskills.dto.SoftSkillEvaluationResponse;
import ee.kerrete.ainterview.softskills.enums.SoftSkillSource;
import ee.kerrete.ainterview.softskills.service.SoftSkillEvaluationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SoftSkillEvaluationController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("all")
class SoftSkillEvaluationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SoftSkillEvaluationService evaluationService;

    // Security-related beans mocked to satisfy context
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private JwtService jwtService;

    @Test
    void createEvaluation_returnsCreated() throws Exception {
        SoftSkillEvaluationRequest request = SoftSkillEvaluationRequest.builder()
            .email("candidate@example.com")
            .sourceType(SoftSkillSource.HR.name())
            .scores(List.of(
                SoftSkillEvaluationRequest.SoftSkillScoreRequest.builder()
                    .dimensionKey("communication")
                    .score(85)
                    .explanation("Great communicator")
                    .build()
            ))
            .build();

        SoftSkillEvaluationResponse response = SoftSkillEvaluationResponse.builder()
            .id(UUID.randomUUID())
            .email("candidate@example.com")
            .dimension("communication")
            .source(SoftSkillSource.HR)
            .score(85)
            .comment("Great communicator")
            .createdAt(LocalDateTime.now())
            .build();

        when(evaluationService.createEvaluations(any())).thenReturn(List.of(response));

        mockMvc.perform(post("/api/soft-skill/evaluations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$[0].email").value("candidate@example.com"))
            .andExpect(jsonPath("$[0].dimension").value("communication"))
            .andExpect(jsonPath("$[0].score").value(85));
    }

    @Test
    void createEvaluation_missingDimension_returnsBadRequest() throws Exception {
        SoftSkillEvaluationRequest request = SoftSkillEvaluationRequest.builder()
            .email("candidate@example.com")
            .sourceType(SoftSkillSource.HR.name())
            .scores(List.of())
            .build();

        mockMvc.perform(post("/api/soft-skill/evaluations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}

