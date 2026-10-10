package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.auth.JwtService;
import ee.kerrete.ainterview.config.SecurityConfig;
import ee.kerrete.ainterview.dto.JobAnalysisRequest;
import ee.kerrete.ainterview.dto.JobAnalysisResponse;
import ee.kerrete.ainterview.repository.AppUserRepository;
import ee.kerrete.ainterview.service.JobAnalysisService;
import ee.kerrete.ainterview.service.JobAnalysisStatsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice test for POST /api/job-analysis using the application's real SecurityConfig
 * (CSRF disabled, stateless, endpoint requires role ADMIN/USER/CANDIDATE, 401 when unauthenticated).
 */
@WebMvcTest(JobAnalysisController.class)
@Import(SecurityConfig.class)
class JobAnalysisControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobAnalysisService jobAnalysisService;
    @MockBean
    private JobAnalysisStatsService jobAnalysisStatsService;

    // Dependencies of the JwtAuthenticationFilter picked up by the slice
    @MockBean
    private JwtService jwtService;
    @MockBean
    private AppUserRepository appUserRepository;

    private static JobAnalysisResponse sampleResponse() {
        JobAnalysisResponse res = new JobAnalysisResponse();
        res.setMatchScore(0.82);
        res.setStrengths(List.of("Java", "Spring"));
        res.setWeaknesses(List.of("Cloud"));
        res.setMissingSkills(List.of("Kubernetes"));
        res.setRoadmap(List.of("Learn K8s"));
        res.setSuggestedImprovements("Add metrics");
        res.setSummary("Hea sobivus");
        return res;
    }

    @Test
    @WithMockUser(username = "jwt-user@example.com", roles = "USER")
    void analyzeReturnsJobAnalysisJson() throws Exception {
        when(jobAnalysisService.analyze(any(JobAnalysisRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/job-analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"cand@example.com","cvText":"my cv","jobDescription":"the job"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchScore").value(0.82))
                .andExpect(jsonPath("$.strengths", contains("Java", "Spring")))
                .andExpect(jsonPath("$.weaknesses", contains("Cloud")))
                .andExpect(jsonPath("$.missingSkills", contains("Kubernetes")))
                .andExpect(jsonPath("$.roadmap", hasSize(1)))
                .andExpect(jsonPath("$.suggestedImprovements").value("Add metrics"))
                .andExpect(jsonPath("$.summary").value("Hea sobivus"));

        ArgumentCaptor<JobAnalysisRequest> captor = ArgumentCaptor.forClass(JobAnalysisRequest.class);
        verify(jobAnalysisService).analyze(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("cand@example.com");
        assertThat(captor.getValue().getCvText()).isEqualTo("my cv");
        assertThat(captor.getValue().getJobDescription()).isEqualTo("the job");
        verify(jobAnalysisStatsService).recordAnalysis(any(JobAnalysisRequest.class), any(JobAnalysisResponse.class));
    }

    @Test
    @WithMockUser(username = "jwt-user@example.com", roles = "CANDIDATE")
    void missingEmailIsResolvedFromAuthenticatedPrincipal() throws Exception {
        when(jobAnalysisService.analyze(any(JobAnalysisRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/job-analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cvText\":\"cv\",\"jobDescription\":\"jd\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<JobAnalysisRequest> captor = ArgumentCaptor.forClass(JobAnalysisRequest.class);
        verify(jobAnalysisService).analyze(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("jwt-user@example.com");
    }

    @Test
    void unauthenticatedRequestIsRejectedWith401() throws Exception {
        mockMvc.perform(post("/api/job-analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cvText\":\"cv\",\"jobDescription\":\"jd\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(jobAnalysisService, jobAnalysisStatsService);
    }

    @Test
    @WithMockUser(roles = "USER")
    void malformedJsonBodyReturns400() throws Exception {
        // The endpoint has no bean validation (@Valid); only an unreadable body yields 400.
        mockMvc.perform(post("/api/job-analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(jobAnalysisService, jobAnalysisStatsService);
    }
}
