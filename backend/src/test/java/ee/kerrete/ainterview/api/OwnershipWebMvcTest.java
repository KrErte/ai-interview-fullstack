package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.auth.JwtService;
import ee.kerrete.ainterview.config.SecurityConfig;
import ee.kerrete.ainterview.dto.JobAnalysisRequest;
import ee.kerrete.ainterview.dto.JobAnalysisResponse;
import ee.kerrete.ainterview.dto.LearningPlanDto;
import ee.kerrete.ainterview.dto.RoadmapTaskDto;
import ee.kerrete.ainterview.dto.SaveLearningPlanRequest;
import ee.kerrete.ainterview.dto.TrainingProgressResponse;
import ee.kerrete.ainterview.dto.TrainingTaskRequest;
import ee.kerrete.ainterview.dto.UpdateRoadmapTaskRequest;
import ee.kerrete.ainterview.model.AppUser;
import ee.kerrete.ainterview.model.UserRole;
import ee.kerrete.ainterview.repository.AppUserRepository;
import ee.kerrete.ainterview.service.JobAnalysisService;
import ee.kerrete.ainterview.service.JobAnalysisStatsService;
import ee.kerrete.ainterview.service.LearningPlanService;
import ee.kerrete.ainterview.service.RoadmapService;
import ee.kerrete.ainterview.service.TrainerQuestionService;
import ee.kerrete.ainterview.service.TrainingService;
import org.junit.jupiter.api.Nested;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ownership (FP-8): user-owned endpoints always act on the authenticated principal's email.
 * Uses the application's real SecurityConfig and JwtAuthenticationFilter.
 */
@WebMvcTest(controllers = {
        RoadmapController.class,
        JobAnalysisController.class,
        TrainingProgressController.class,
        LearningPlanController.class
})
@Import(SecurityConfig.class)
class OwnershipWebMvcTest {

    private static final String ME = "me@example.com";
    private static final String OTHER = "victim@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoadmapService roadmapService;
    @MockBean
    private JobAnalysisService jobAnalysisService;
    @MockBean
    private JobAnalysisStatsService jobAnalysisStatsService;
    @MockBean
    private TrainingService trainingService;
    @MockBean
    private TrainerQuestionService trainerQuestionService;
    @MockBean
    private LearningPlanService learningPlanService;
    // dependencies of JwtAuthenticationFilter
    @MockBean
    private JwtService jwtService;
    @MockBean
    private AppUserRepository appUserRepository;

    private static TrainingProgressResponse progress(String email) {
        return TrainingProgressResponse.builder().email(email).totalTasks(1).completedTasks(0).build();
    }

    // ------------------------------------------------------------------ roadmap
    @Nested
    class Roadmap {

        @Test
        @WithMockUser(username = ME)
        void getOwnRoadmapUsesPrincipal() throws Exception {
            when(roadmapService.getTasksForEmail(ME))
                    .thenReturn(List.of(RoadmapTaskDto.builder().taskKey("step-1").title("Step 1").build()));

            mockMvc.perform(get("/api/roadmap"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].taskKey").value("step-1"));
        }

        @Test
        @WithMockUser(username = ME)
        void legacyPathWithOwnEmailIsAllowed() throws Exception {
            mockMvc.perform(get("/api/roadmap/" + ME)).andExpect(status().isOk());
            verify(roadmapService).getTasksForEmail(ME);
        }

        @Test
        @WithMockUser(username = ME)
        void legacyPathWithOtherUsersEmailIsForbidden() throws Exception {
            mockMvc.perform(get("/api/roadmap/" + OTHER)).andExpect(status().isForbidden());
            verifyNoInteractions(roadmapService);
        }

        @Test
        @WithMockUser(username = ME)
        void updateIgnoresBodyEmailAndUsesPrincipal() throws Exception {
            mockMvc.perform(post("/api/roadmap/update")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + OTHER + "\",\"taskKey\":\"step-1\",\"completed\":true}"))
                    .andExpect(status().isOk());

            ArgumentCaptor<UpdateRoadmapTaskRequest> captor = ArgumentCaptor.forClass(UpdateRoadmapTaskRequest.class);
            verify(roadmapService).updateTask(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo(ME);
            assertThat(captor.getValue().getTaskKey()).isEqualTo("step-1");
            assertThat(captor.getValue().isCompleted()).isTrue();
        }

        @Test
        void unauthenticatedIs401() throws Exception {
            mockMvc.perform(get("/api/roadmap")).andExpect(status().isUnauthorized());
            mockMvc.perform(get("/api/roadmap/" + OTHER)).andExpect(status().isUnauthorized());
            mockMvc.perform(post("/api/roadmap/update").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(roadmapService);
        }
    }

    // ------------------------------------------------------------------ job analysis
    @Nested
    class JobAnalysis {

        @Test
        @WithMockUser(username = ME)
        void principalEmailWinsOverBodyEmail() throws Exception {
            JobAnalysisResponse res = new JobAnalysisResponse();
            res.setMatchScore(50);
            when(jobAnalysisService.analyze(any())).thenReturn(res);

            mockMvc.perform(post("/api/job-analysis")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + OTHER + "\",\"cvText\":\"cv\",\"jobDescription\":\"jd\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matchScore").value(50));

            ArgumentCaptor<JobAnalysisRequest> captor = ArgumentCaptor.forClass(JobAnalysisRequest.class);
            verify(jobAnalysisService).analyze(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo(ME);
            verify(jobAnalysisStatsService).recordAnalysis(captor.getValue(), res);
        }

        @Test
        void realJwtPrincipalIsUsed() throws Exception {
            // goes through the real JwtAuthenticationFilter: the principal is the email String
            when(jwtService.extractEmail("valid-token")).thenReturn(ME);
            when(appUserRepository.findByEmail(ME)).thenReturn(Optional.of(
                    AppUser.builder().email(ME).role(UserRole.USER).enabled(true).build()));
            when(jobAnalysisService.analyze(any())).thenReturn(new JobAnalysisResponse());

            mockMvc.perform(post("/api/job-analysis")
                            .header("Authorization", "Bearer valid-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + OTHER + "\",\"cvText\":\"cv\",\"jobDescription\":\"jd\"}"))
                    .andExpect(status().isOk());

            ArgumentCaptor<JobAnalysisRequest> captor = ArgumentCaptor.forClass(JobAnalysisRequest.class);
            verify(jobAnalysisService).analyze(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo(ME);
        }

        @Test
        void unauthenticatedIs401() throws Exception {
            mockMvc.perform(post("/api/job-analysis")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + OTHER + "\",\"cvText\":\"cv\",\"jobDescription\":\"jd\"}"))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(jobAnalysisService, jobAnalysisStatsService);
        }

        @Test
        void invalidJwtIs401() throws Exception {
            when(jwtService.extractEmail(anyString())).thenThrow(new RuntimeException("bad token"));
            mockMvc.perform(post("/api/job-analysis")
                            .header("Authorization", "Bearer forged")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + OTHER + "\"}"))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(jobAnalysisService);
        }
    }

    // ------------------------------------------------------------------ training progress
    @Nested
    class Training {

        @Test
        @WithMockUser(username = ME)
        void progressIgnoresEmailQueryParam() throws Exception {
            when(trainingService.getProgress(ME)).thenReturn(progress(ME));

            mockMvc.perform(get("/api/training/progress").param("email", OTHER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(ME));

            verify(trainingService).getProgress(ME);
        }

        @Test
        @WithMockUser(username = ME)
        void updateTaskUsesPrincipalAndDoesNotRequireBodyEmail() throws Exception {
            when(trainingService.updateTaskStatus(any())).thenReturn(progress(ME));

            mockMvc.perform(post("/api/training/task")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"taskKey\":\"t-1\",\"completed\":true}"))
                    .andExpect(status().isOk());

            ArgumentCaptor<TrainingTaskRequest> captor = ArgumentCaptor.forClass(TrainingTaskRequest.class);
            verify(trainingService).updateTaskStatus(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo(ME);
        }

        @Test
        @WithMockUser(username = ME)
        void updateTaskBodyEmailIsOverridden() throws Exception {
            when(trainingService.updateTaskStatus(any())).thenReturn(progress(ME));

            mockMvc.perform(post("/api/training/task")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + OTHER + "\",\"taskKey\":\"t-1\"}"))
                    .andExpect(status().isOk());

            ArgumentCaptor<TrainingTaskRequest> captor = ArgumentCaptor.forClass(TrainingTaskRequest.class);
            verify(trainingService).updateTaskStatus(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo(ME);
        }

        @Test
        @WithMockUser(username = ME)
        void updateTaskStillValidatesTaskKey() throws Exception {
            mockMvc.perform(post("/api/training/task")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"completed\":true}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(trainingService);
        }

        @Test
        @WithMockUser(username = ME)
        void startPlanSavesForPrincipal() throws Exception {
            when(trainingService.getTrainingProgress(ME)).thenReturn(progress(ME));

            mockMvc.perform(post("/api/training/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + OTHER + "\",\"targetRole\":\"dev\",\"tasks\":[{\"title\":\"A\"}]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.progress.email").value(ME));

            verify(roadmapService).savePlan(eq(ME), eq("dev"), any());
        }

        @Test
        @WithMockUser(username = ME)
        void completeRoadmapTaskUsesPrincipal() throws Exception {
            when(trainingService.getTrainingProgress(ME)).thenReturn(progress(ME));

            mockMvc.perform(post("/api/training/task/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + OTHER + "\",\"taskKey\":\"step-1\",\"completed\":true}"))
                    .andExpect(status().isOk());

            ArgumentCaptor<UpdateRoadmapTaskRequest> captor = ArgumentCaptor.forClass(UpdateRoadmapTaskRequest.class);
            verify(roadmapService).updateTask(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo(ME);
            verify(trainingService).getTrainingProgress(ME);
        }

        @Test
        void unauthenticatedIs401() throws Exception {
            mockMvc.perform(get("/api/training/progress").param("email", OTHER)).andExpect(status().isUnauthorized());
            mockMvc.perform(post("/api/training/start").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(trainingService, roadmapService);
        }
    }

    // ------------------------------------------------------------------ learning plans
    @Nested
    class LearningPlans {

        private static final String VALID_PLAN = "{\"email\":\"" + OTHER + "\",\"jobTitle\":\"Dev\","
                + "\"missingSkills\":[\"K8s\"],\"roadmap\":[\"Learn K8s\"]}";

        @Test
        @WithMockUser(username = ME)
        void saveUsesPrincipalEmail() throws Exception {
            mockMvc.perform(post("/api/learning-plans").contentType(MediaType.APPLICATION_JSON).content(VALID_PLAN))
                    .andExpect(status().isOk());

            ArgumentCaptor<SaveLearningPlanRequest> captor = ArgumentCaptor.forClass(SaveLearningPlanRequest.class);
            verify(learningPlanService).savePlan(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo(ME);
        }

        @Test
        @WithMockUser(username = ME)
        void saveStillValidatesOtherFields() throws Exception {
            mockMvc.perform(post("/api/learning-plans").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"jobTitle\":\"Dev\",\"missingSkills\":[],\"roadmap\":[\"x\"]}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(learningPlanService);
        }

        @Test
        @WithMockUser(username = ME)
        void getOwnPlan() throws Exception {
            LearningPlanDto dto = new LearningPlanDto();
            dto.setEmail(ME);
            dto.setJobTitle("Dev");
            when(learningPlanService.getLastPlan(ME)).thenReturn(Optional.of(dto));

            mockMvc.perform(get("/api/learning-plans"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(ME));
            mockMvc.perform(get("/api/learning-plans/" + ME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jobTitle").value("Dev"));
        }

        @Test
        @WithMockUser(username = ME)
        void otherUsersPlanIsForbidden() throws Exception {
            mockMvc.perform(get("/api/learning-plans/" + OTHER)).andExpect(status().isForbidden());
            verifyNoInteractions(learningPlanService);
        }

        @Test
        void unauthenticatedIs401() throws Exception {
            mockMvc.perform(get("/api/learning-plans/" + OTHER)).andExpect(status().isUnauthorized());
            mockMvc.perform(post("/api/learning-plans").contentType(MediaType.APPLICATION_JSON).content(VALID_PLAN))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(learningPlanService);
        }
    }
}
