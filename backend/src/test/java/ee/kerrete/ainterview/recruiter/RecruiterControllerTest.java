package ee.kerrete.ainterview.recruiter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import ee.kerrete.ainterview.model.AppUser;
import ee.kerrete.ainterview.model.UserRole;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RecruiterController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecruiterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RecruiterOverviewService recruiterOverviewService;

    // Security-related beans mocked to satisfy context
    @MockBean
    private ee.kerrete.ainterview.auth.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private ee.kerrete.ainterview.auth.jwt.JwtService jwtService;

    @Test
    void overview_returnsCandidateSummary() throws Exception {
        RecruiterCandidateSummaryDto candidate = RecruiterCandidateSummaryDto.builder()
                .id("1")
                .name("Admin User")
                .email("admin@local.test")
                .matchScore(85.0)
                .latestScoreLabel("Strong fit")
                .keySkills(List.of("Java", "Spring"))
                .analysesRun(5)
                .trainingSessions(3)
                .trainingProgressPercent(50)
                .build();

        RecruiterOverviewResponse response = RecruiterOverviewResponse.builder()
                .jobDescriptionEcho("Backend role")
                .candidates(List.of(candidate))
                .build();

        when(recruiterOverviewService.getOverview(any(), any())).thenReturn(response);

        RecruiterOverviewRequest request = new RecruiterOverviewRequest();
        request.setJobDescription("Backend role");

        AppUser principal = AppUser.builder()
                .id(1L)
                .email("admin@local.test")
                .fullName("Admin User")
                .role(UserRole.ADMIN)
                .enabled(true)
                .build();
        TestingAuthenticationToken auth = new TestingAuthenticationToken(principal, null);

        mockMvc.perform(post("/api/recruiter/overview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobDescriptionEcho").value("Backend role"))
                .andExpect(jsonPath("$.candidates[0].name").value("Admin User"));
    }
}

