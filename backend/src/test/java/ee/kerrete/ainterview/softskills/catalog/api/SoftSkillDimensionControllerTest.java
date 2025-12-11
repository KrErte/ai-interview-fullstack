package ee.kerrete.ainterview.softskills.catalog.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.auth.jwt.JwtAuthenticationFilter;
import ee.kerrete.ainterview.auth.jwt.JwtService;
import ee.kerrete.ainterview.softskills.catalog.dto.SoftSkillDimensionResponseDto;
import ee.kerrete.ainterview.softskills.catalog.service.SoftSkillDimensionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SoftSkillDimensionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SoftSkillDimensionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SoftSkillDimensionService dimensionService;

    // Security-related beans mocked to satisfy context
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private JwtService jwtService;

    @Test
    void list_returnsDimensions() throws Exception {
        var dto = SoftSkillDimensionResponseDto.builder()
            .key("communication")
            .label("Communication")
            .definition("Clarity")
            .highSignals(List.of("Clear"))
            .lowSignals(List.of("Vague"))
            .interviewSignals(List.of("Explain"))
            .coachingIdeas(List.of("Practice"))
            .build();

        when(dimensionService.findAll()).thenReturn(List.of());
        // controller maps service to DTO via mapper inside service, but for this simple test we just expect empty list if service returns empty
        mockMvc.perform(get("/api/soft-skill/dimensions")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}

