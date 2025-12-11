package ee.kerrete.ainterview.softskills.api;

import ee.kerrete.ainterview.auth.jwt.JwtAuthenticationFilter;
import ee.kerrete.ainterview.auth.jwt.JwtService;
import ee.kerrete.ainterview.softskills.dto.SoftSkillMergedDimensionView;
import ee.kerrete.ainterview.softskills.dto.SoftSkillMergedProfileView;
import ee.kerrete.ainterview.softskills.service.SoftSkillMergedProfileViewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SoftSkillMergedProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class SoftSkillMergedProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SoftSkillMergedProfileViewService mergedProfileViewService;

    // Security-related beans mocked to satisfy context
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private JwtService jwtService;

    @Test
    void getMergedProfile_returnsAggregatedView() throws Exception {
        SoftSkillMergedProfileView view = SoftSkillMergedProfileView.builder()
            .email("candidate@example.com")
            .dimensions(List.of(
                SoftSkillMergedDimensionView.builder()
                    .dimensionKey("communication")
                    .averageScore(3.5)
                    .ratingCount(2)
                    .build()
            ))
            .build();
        when(mergedProfileViewService.getMergedProfile(anyString())).thenReturn(view);

        mockMvc.perform(get("/api/soft-skill/merged-profile")
                .param("email", "candidate@example.com")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("candidate@example.com"))
            .andExpect(jsonPath("$.dimensions[0].dimensionKey").value("communication"))
            .andExpect(jsonPath("$.dimensions[0].averageScore").value(3.5))
            .andExpect(jsonPath("$.dimensions[0].ratingCount").value(2));
    }

    @Test
    void getMergedProfile_missingEmail_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/soft-skill/merged-profile"))
            .andExpect(status().isBadRequest());
    }
}


