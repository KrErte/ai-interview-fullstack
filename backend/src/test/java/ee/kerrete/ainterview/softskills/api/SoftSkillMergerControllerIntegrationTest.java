package ee.kerrete.ainterview.softskills.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.service.OpenAiClient;
import ee.kerrete.ainterview.softskills.dto.MergedDimensionScore;
import ee.kerrete.ainterview.softskills.dto.SoftSkillMergeRequest;
import ee.kerrete.ainterview.softskills.dto.SoftSkillMergeResponse;
import ee.kerrete.ainterview.softskills.dto.SoftSkillMergedProfileDto;
import ee.kerrete.ainterview.softskills.dto.SoftSkillSourceDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class SoftSkillMergerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @org.springframework.boot.test.mock.mockito.MockBean
    private OpenAiClient openAiClient;

    @Test
    void mergeSoftSkills_returnsStructuredMergedProfile() throws Exception {
        when(openAiClient.createChatCompletion(anyString(), anyString()))
            .thenReturn(sampleAiPayload());

        SoftSkillMergeRequest request = SoftSkillMergeRequest.builder()
            .email("candidate@example.com")
            .saveMerged(false)
            .sources(List.of(
                SoftSkillSourceDto.builder()
                    .sourceType("HR")
                    .label("Hiring manager notes")
                    .content("Communicates clearly and keeps stakeholders informed.")
                    .build(),
                SoftSkillSourceDto.builder()
                    .sourceType("TECH_LEAD")
                    .label("Tech screen")
                    .content("Strong ownership, solid collaboration, could tighten conciseness.")
                    .build()
            ))
            .build();

        MvcResult result = mockMvc.perform(post("/api/soft-skills/merge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mergedProfile.summary").isNotEmpty())
            .andExpect(jsonPath("$.mergedProfile.strengths").isArray())
            .andExpect(jsonPath("$.mergedProfile.risks").isArray())
            .andExpect(jsonPath("$.mergedProfile.dimensionScoresMerged").isArray())
            .andExpect(jsonPath("$.mergedProfile.dimensionScoresMerged.length()", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.mergedProfile.dimensionScoresMerged[0].dimension").value("communication"))
            .andExpect(jsonPath("$.mergedProfile.dimensionScoresMerged[0].mergedScore").isNumber())
            .andExpect(jsonPath("$.mergedProfile.dimensionScoresMerged[0].confidence").isNumber())
            .andReturn();

        SoftSkillMergeResponse response = objectMapper.readValue(
            result.getResponse().getContentAsByteArray(),
            SoftSkillMergeResponse.class
        );

        SoftSkillMergedProfileDto profile = response.getMergedProfile();
        assertThat(profile).isNotNull();
        assertThat(profile.getSummary()).isNotBlank();
        assertThat(profile.getStrengths()).isNotEmpty();
        assertThat(profile.getDimensionScoresMerged()).isNotEmpty();

        MergedDimensionScore first = profile.getDimensionScoresMerged().get(0);
        assertThat(first.getMergedScore()).isBetween(1.0, 5.0);
        assertThat(first.getConfidence()).isBetween(0.0, 1.0);
        assertThat(confidenceTone(first.getConfidence()))
            .isIn(Set.of("low", "medium", "high"));
    }

    @Test
    void mergeSoftSkills_missingSources_returnsBadRequest() throws Exception {
        SoftSkillMergeRequest request = SoftSkillMergeRequest.builder()
            .email("candidate@example.com")
            .sources(List.of())
            .build();

        mockMvc.perform(post("/api/soft-skills/merge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
            .andExpect(status().isBadRequest());

        verify(openAiClient, never()).createChatCompletion(anyString(), anyString());
    }

    private String sampleAiPayload() {
        SoftSkillMergedProfileDto dto = SoftSkillMergedProfileDto.builder()
            .summary("Clear communicator who follows through on commitments.")
            .strengths(List.of("Concise updates", "Collaborates well"))
            .risks(List.of("Occasionally over-commits"))
            .communicationStyle("Direct, concise, keeps stakeholders updated.")
            .collaborationStyle("Prefers pairing and async notes.")
            .growthAreas(List.of("Deepen system design breadth"))
            .dimensionScoresMerged(List.of(
                MergedDimensionScore.builder()
                    .dimension("communication")
                    .mergedScore(4.2)
                    .confidence(0.82)
                    .rationale("Consistent strong feedback across sources.")
                    .build(),
                MergedDimensionScore.builder()
                    .dimension("ownership")
                    .mergedScore(4.0)
                    .confidence(0.64)
                    .rationale("Delivers outcomes and escalates early.")
                    .build()
            ))
            .meta(ee.kerrete.ainterview.softskills.dto.Meta.builder()
                .overallConfidence(0.76)
                .mainDisagreements(List.of("Tone in async updates"))
                .notesForCoach(List.of("Probe on scalability stories"))
                .build())
            .build();

        try {
            return objectMapper.writeValueAsString(dto);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize sample AI payload", ex);
        }
    }

    private String confidenceTone(Double confidence) {
        double value = confidence == null ? 0 : confidence;
        if (value >= 0.67) {
            return "high";
        }
        if (value >= 0.34) {
            return "medium";
        }
        return "low";
    }
}


