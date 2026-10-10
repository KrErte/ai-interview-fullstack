package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.auth.JwtService;
import ee.kerrete.ainterview.config.SecurityConfig;
import ee.kerrete.ainterview.model.RoadmapTask;
import ee.kerrete.ainterview.repository.AppUserRepository;
import ee.kerrete.ainterview.repository.JobAnalysisSessionRepository;
import ee.kerrete.ainterview.repository.RoadmapTaskRepository;
import ee.kerrete.ainterview.repository.TrainingProgressRepository;
import ee.kerrete.ainterview.service.ProfileOverviewService;
import ee.kerrete.ainterview.service.RoadmapService;
import ee.kerrete.ainterview.service.TrainerQuestionService;
import ee.kerrete.ainterview.service.TrainingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FP-25, with the real SecurityConfig, RoadmapService and ProfileOverviewService (repositories mocked):
 * unknown task keys and other users' task keys give the same 404 (not 500), and the profile overview
 * only counts the user's own analyses.
 */
@WebMvcTest(controllers = {RoadmapController.class, TrainingProgressController.class, ProfileOverviewController.class})
@Import({SecurityConfig.class, RoadmapService.class, ProfileOverviewService.class})
class TaskNotFoundAndOverviewWebMvcTest {

    private static final String ME = "me@example.com";
    private static final String OTHER = "victim@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoadmapTaskRepository roadmapTaskRepository;
    @MockBean
    private JobAnalysisSessionRepository jobAnalysisSessionRepository;
    @MockBean
    private TrainingProgressRepository trainingProgressRepository;
    @MockBean
    private TrainingService trainingService;
    @MockBean
    private TrainerQuestionService trainerQuestionService;
    // dependencies of JwtAuthenticationFilter
    @MockBean
    private JwtService jwtService;
    @MockBean
    private AppUserRepository appUserRepository;

    @BeforeEach
    void data() {
        RoadmapTask mine = RoadmapTask.builder().id(1L).email(ME).taskKey("my-task").orderIndex(1).build();
        RoadmapTask theirs = RoadmapTask.builder().id(2L).email(OTHER).taskKey("victim-task").orderIndex(1).build();
        when(roadmapTaskRepository.findByEmailAndTaskKey(anyString(), anyString())).thenReturn(Optional.empty());
        when(roadmapTaskRepository.findByEmailAndTaskKey(ME, "my-task")).thenReturn(Optional.of(mine));
        when(roadmapTaskRepository.findByEmailAndTaskKey(OTHER, "victim-task")).thenReturn(Optional.of(theirs));
        when(roadmapTaskRepository.findByEmail(ME)).thenReturn(List.of(mine));
    }

    private ResultActions postAs(String path, String taskKey) throws Exception {
        return mockMvc.perform(post(path).with(user(ME).roles("CANDIDATE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"taskKey\":\"" + taskKey + "\",\"completed\":true,\"email\":\"" + OTHER + "\"}"));
    }

    @Test
    void roadmapUpdate_unknownAndForeignKeysGiveSame404() throws Exception {
        String unknown = postAs("/api/roadmap/update", "no-such-task").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Roadmap task not found"))
                .andReturn().getResponse().getContentAsString();
        String foreign = postAs("/api/roadmap/update", "victim-task").andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(foreign).isEqualTo(unknown);
        verify(roadmapTaskRepository, never()).save(any());
    }

    @Test
    void trainingTaskComplete_unknownAndForeignKeysGiveSame404() throws Exception {
        String unknown = postAs("/api/training/task/complete", "no-such-task").andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        String foreign = postAs("/api/training/task/complete", "victim-task").andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(foreign).isEqualTo(unknown);
        verify(roadmapTaskRepository, never()).save(any());
        verify(trainingService, never()).getTrainingProgress(anyString());
    }

    @Test
    void missingTaskKeyGives404() throws Exception {
        mockMvc.perform(post("/api/roadmap/update").with(user(ME).roles("CANDIDATE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"completed\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownTaskStillUpdates() throws Exception {
        postAs("/api/roadmap/update", "my-task").andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskKey").value("my-task"))
                .andExpect(jsonPath("$[0].completed").value(true));
        postAs("/api/training/task/complete", "my-task").andExpect(status().isOk());
    }

    @Test
    void unauthenticatedGets401() throws Exception {
        mockMvc.perform(post("/api/roadmap/update").contentType(MediaType.APPLICATION_JSON)
                .content("{\"taskKey\":\"my-task\",\"completed\":true}")).andExpect(status().isUnauthorized());
    }

    @Test
    void overviewCountsOnlyOwnAnalyses() throws Exception {
        when(jobAnalysisSessionRepository.count()).thenReturn(1000L); // everyone's analyses
        when(jobAnalysisSessionRepository.countByEmail(ME)).thenReturn(3L);

        mockMvc.perform(get("/api/profile/overview").with(user(ME).roles("CANDIDATE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAnalyses").value(3))
                .andExpect(jsonPath("$.totalAnalysesForEmail").value(3));

        verify(jobAnalysisSessionRepository, never()).count();
        verify(jobAnalysisSessionRepository, never()).countByEmail(OTHER);
    }
}
