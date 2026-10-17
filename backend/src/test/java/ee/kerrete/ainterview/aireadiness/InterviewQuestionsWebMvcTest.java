package ee.kerrete.ainterview.aireadiness;

import ee.kerrete.ainterview.auth.JwtService;
import ee.kerrete.ainterview.config.SecurityConfig;
import ee.kerrete.ainterview.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/ai-readiness/professions/{id}/interview-questions: public, written questions or a set built from the tasks. */
@WebMvcTest(controllers = InterviewQuestionsController.class)
@Import({SecurityConfig.class, AiReadinessCatalog.class, AiReadinessScorer.class, JobPages.class})
class InterviewQuestionsWebMvcTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    JwtService jwtService;
    @MockBean
    AppUserRepository appUserRepository;

    @Test
    void handWrittenProfessionGetsItsWrittenQuestions() throws Exception {
        mvc.perform(get("/api/ai-readiness/professions/lawyer/interview-questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[2].strongAnswer", containsString("a citation or case that did not exist")));
    }

    @Test
    void onetProfessionGetsSixQuestionsBuiltFromItsOwnTasks() throws Exception {
        mvc.perform(get("/api/ai-readiness/professions/onet-35-2014-00/interview-questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[0].question").value("How do you use AI in your work today?"))
                .andExpect(jsonPath("$[1].question", containsString("Walk us through how you would use AI for")))
                .andExpect(jsonPath("$[2].strongAnswer", containsString("contained a factual error")));
    }

    @Test
    void unknownProfessionIs404() throws Exception {
        mvc.perform(get("/api/ai-readiness/professions/nope/interview-questions")).andExpect(status().isNotFound());
    }
}
