package ee.kerrete.ainterview.softskills.api;

import ee.kerrete.ainterview.AbstractIntegrationTest;
import ee.kerrete.ainterview.model.UserRole;
import ee.kerrete.ainterview.softskills.dto.SoftSkillMergeRequest;
import ee.kerrete.ainterview.softskills.dto.SoftSkillSourceDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SoftSkillMergerControllerIT extends AbstractIntegrationTest {

    @Test
    void mergeSoftSkills_returnsStructuredProfileAndSaves() throws Exception {
        createUser("soft@example.com", "Password1!", true, UserRole.USER);
        String token = loginAndGetToken("soft@example.com", "Password1!");

        String aiJson = """
                {
                  "summary": "Merged soft skill profile",
                  "strengths": ["Collaboration", "Communication"],
                  "risks": ["Needs clearer prioritization"],
                  "communicationStyle": "Direct and concise",
                  "collaborationStyle": "Supportive teammate",
                  "growthAreas": ["Stakeholder updates"]
                }
                """;
        when(openAiClient.createChatCompletion(anyString(), anyString()))
                .thenReturn(aiJson);

        SoftSkillMergeRequest request = SoftSkillMergeRequest.builder()
                .email("soft@example.com")
                .sources(List.of(
                        SoftSkillSourceDto.builder()
                                .sourceType("HR")
                                .label("HR feedback")
                                .content("Great communicator and empathetic.")
                                .build(),
                        SoftSkillSourceDto.builder()
                                .sourceType("TECH")
                                .label("Tech lead")
                                .content("Needs clearer prioritization and status updates.")
                                .build()
                ))
                .saveMerged(true)
                .build();

        mockMvc.perform(post("/api/soft-skills/merge")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mergedProfile.summary").value("Merged soft skill profile"))
                .andExpect(jsonPath("$.mergedProfile.strengths[0]").value("Collaboration"))
                .andExpect(jsonPath("$.saved").value(true))
                .andExpect(jsonPath("$.savedProfileId").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }
}

