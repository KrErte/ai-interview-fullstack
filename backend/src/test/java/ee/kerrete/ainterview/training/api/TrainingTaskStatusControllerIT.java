package ee.kerrete.ainterview.training.api;

import ee.kerrete.ainterview.AbstractIntegrationTest;
import ee.kerrete.ainterview.model.UserRole;
import ee.kerrete.ainterview.training.repository.TrainingTaskStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TrainingTaskStatusControllerIT extends AbstractIntegrationTest {

    @Autowired
    private TrainingTaskStatusRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void statusAndCompletionFlow() throws Exception {
        createUser("trainee@example.com", "Password1!", true, UserRole.USER);
        String token = loginAndGetToken("trainee@example.com", "Password1!");

        mockMvc.perform(get("/api/training/status")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalTasks").value(4))
            .andExpect(jsonPath("$.completedTasks").value(0))
            .andExpect(jsonPath("$.progressPercent").value(0))
            .andExpect(jsonPath("$.tasks").isArray())
            .andExpect(jsonPath("$.tasks.length()").value(4))
            .andExpect(jsonPath("$.tasks[0].completed").value(false));

        mockMvc.perform(post("/api/training/complete/cv-refresh")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"completed\": true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completedTasks").value(1))
            .andExpect(jsonPath("$.progressPercent").value(25))
            .andExpect(jsonPath("$.tasks[0].taskKey").value("cv-refresh"))
            .andExpect(jsonPath("$.tasks[0].completed").value(true));

        mockMvc.perform(post("/api/training/complete/cv-refresh")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"completed\": false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completedTasks").value(0))
            .andExpect(jsonPath("$.progressPercent").value(0))
            .andExpect(jsonPath("$.tasks[0].taskKey").value("cv-refresh"))
            .andExpect(jsonPath("$.tasks[0].completed").value(false));
    }

    @Test
    void unknownTaskKey_returnsBadRequest() throws Exception {
        createUser("trainee2@example.com", "Password1!", true, UserRole.USER);
        String token = loginAndGetToken("trainee2@example.com", "Password1!");

        mockMvc.perform(post("/api/training/complete/unknown-task")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"completed\": true}"))
            .andExpect(status().isBadRequest());
    }
}

