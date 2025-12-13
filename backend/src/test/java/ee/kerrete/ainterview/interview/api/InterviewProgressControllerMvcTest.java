package ee.kerrete.ainterview.interview.api;

import ee.kerrete.ainterview.dto.CreateInterviewSessionRequest;
import ee.kerrete.ainterview.dto.CreateInterviewSessionResponse;
import ee.kerrete.ainterview.interview.dto.InterviewIntelligenceResponseDto;
import ee.kerrete.ainterview.interview.dto.InterviewNextQuestionRequestDto;
import ee.kerrete.ainterview.interview.service.InterviewIntelligenceService;
import ee.kerrete.ainterview.service.InterviewSessionService;
import ee.kerrete.ainterview.auth.jwt.JwtAuthenticationFilter;
import ee.kerrete.ainterview.auth.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InterviewProgressController.class)
@AutoConfigureMockMvc(addFilters = false)
class InterviewProgressControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InterviewIntelligenceService interviewIntelligenceService;

    @MockBean
    private InterviewSessionService interviewSessionService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtService jwtService;

    @Test
    void startEndpointIsMapped() throws Exception {
        CreateInterviewSessionResponse response = new CreateInterviewSessionResponse(1L, "a@b.com", UUID.randomUUID());
        when(interviewSessionService.createSession(any(CreateInterviewSessionRequest.class)))
            .thenReturn(response);

        mockMvc.perform(post("/api/interviews/start")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"email\":\"a@b.com\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void nextQuestionEndpointIsMapped() throws Exception {
        UUID sessionId = UUID.randomUUID();
        InterviewIntelligenceResponseDto resp = InterviewIntelligenceResponseDto.builder()
            .question("q1")
            .decision("probe")
            .sessionComplete(false)
            .build();
        when(interviewIntelligenceService.nextQuestion(eq(sessionId), any(InterviewNextQuestionRequestDto.class)))
            .thenReturn(resp);

        mockMvc.perform(post("/api/interviews/" + sessionId + "/next-question")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"answer\":\"hi\"}"))
            .andExpect(status().isOk());
    }
}

