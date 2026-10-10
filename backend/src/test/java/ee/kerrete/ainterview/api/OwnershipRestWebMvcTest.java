package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.api.controller.CandidateProfileController;
import ee.kerrete.ainterview.auth.JwtService;
import ee.kerrete.ainterview.config.SecurityConfig;
import ee.kerrete.ainterview.controller.WorkstyleSessionController;
import ee.kerrete.ainterview.dto.AdaptiveAnalysisRequest;
import ee.kerrete.ainterview.dto.CreateInterviewSessionRequest;
import ee.kerrete.ainterview.dto.EvaluateAnswerRequest;
import ee.kerrete.ainterview.dto.JobMatchRequest;
import ee.kerrete.ainterview.dto.SkillPlanRequest;
import ee.kerrete.ainterview.dto.SoftSkillQuestionRequest;
import ee.kerrete.ainterview.dto.TrainingTaskRequest;
import ee.kerrete.ainterview.model.TrainingTask;
import ee.kerrete.ainterview.model.WorkstyleSession;
import ee.kerrete.ainterview.repository.AppUserRepository;
import ee.kerrete.ainterview.repository.TrainingTaskRepository;
import ee.kerrete.ainterview.service.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FP-17: every remaining user-scoped endpoint uses the authenticated principal's email.
 * Emails in query params / bodies are ignored; another user's workstyle session gives 403.
 * Real SecurityConfig + JwtAuthenticationFilter; all services mocked.
 */
@WebMvcTest(controllers = {
        AdaptiveTrainerController.class,
        CandidatePlanController.class,
        CandidateProfileController.class,
        DashboardController.class,
        EvaluationController.class,
        InterviewController.class,
        InterviewSessionController.class,
        JobMatchController.class,
        MindsetRoadmapController.class,
        ProfileOverviewController.class,
        ProgressController.class,
        SkillCoachController.class,
        SkillMatrixController.class,
        SkillPlanController.class,
        SoftSkillMatrixController.class,
        TrainerController.class,
        TrainingTaskController.class,
        UserProfileController.class,
        WorkstyleSessionController.class
})
@Import(SecurityConfig.class)
class OwnershipRestWebMvcTest {

    private static final String ME = "me@example.com";
    private static final String OTHER = "victim@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockBean private AiAdaptiveService aiAdaptiveService;
    @MockBean private SoftSkillQuestionService softSkillQuestionService;
    @MockBean private CandidatePlanService candidatePlanService;
    @MockBean private SessionService sessionService;
    @MockBean private DashboardService dashboardService;
    @MockBean private EvaluationService evaluationService;
    @MockBean private AIExampleAnswerService aiExampleAnswerService;
    @MockBean private InterviewSessionService interviewSessionService;
    @MockBean private JobMatchService jobMatchService;
    @MockBean private MindsetRoadmapService mindsetRoadmapService;
    @MockBean private TrainingService trainingService;
    @MockBean private ProfileOverviewService profileOverviewService;
    @MockBean private SkillMatrixService skillMatrixService;
    @MockBean private SkillPlanService skillPlanService;
    @MockBean private SoftSkillMatrixService softSkillMatrixService;
    @MockBean private TrainingTaskRepository trainingTaskRepository;
    @MockBean private UserProfileService userProfileService;
    @MockBean private WorkstyleSessionService workstyleSessionService;
    // dependencies of JwtAuthenticationFilter
    @MockBean private JwtService jwtService;
    @MockBean private AppUserRepository appUserRepository;

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder b, String body) {
        return b.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    // ------------------------------------------------------------ query-param endpoints
    @Nested
    @WithMockUser(username = ME)
    class QueryParamEmailIsIgnored {

        @Test
        void mindsetRoadmaps() throws Exception {
            mockMvc.perform(get("/api/mindset/roadmaps").param("email", OTHER)).andExpect(status().isOk());
            verify(mindsetRoadmapService).getRoadmapsForEmail(ME);
        }

        @Test
        void mindsetRoadmapDetail() throws Exception {
            mockMvc.perform(get("/api/mindset/roadmaps/conflict").param("email", OTHER)).andExpect(status().isOk());
            verify(mindsetRoadmapService).getRoadmapDetail(ME, "conflict");
        }

        @Test
        void candidatePlanLast() throws Exception {
            mockMvc.perform(get("/api/plan/last").param("email", OTHER)).andExpect(status().isNotFound());
            verify(candidatePlanService).getLastPlan(ME);
        }

        @Test
        void candidateProgress() throws Exception {
            mockMvc.perform(get("/api/candidate/progress").param("email", OTHER)).andExpect(status().isOk());
            verify(sessionService).getProgressSummary(ME);
        }

        @Test
        void dashboard() throws Exception {
            mockMvc.perform(get("/api/dashboard").param("email", OTHER)).andExpect(status().isOk());
            verify(dashboardService).get(ME);
        }

        @Test
        void jobMatchList() throws Exception {
            mockMvc.perform(get("/api/job-match").param("email", OTHER).param("targetRole", "dev"))
                    .andExpect(status().isOk());
            ArgumentCaptor<JobMatchRequest> captor = ArgumentCaptor.forClass(JobMatchRequest.class);
            verify(jobMatchService).match(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo(ME);
            assertThat(captor.getValue().getTargetRole()).isEqualTo("dev");
        }

        @Test
        void profileOverview() throws Exception {
            mockMvc.perform(get("/api/profile/overview").param("email", OTHER)).andExpect(status().isOk());
            verify(profileOverviewService).getOverview(ME);
        }

        @Test
        void progress() throws Exception {
            mockMvc.perform(get("/api/progress").param("email", OTHER)).andExpect(status().isOk());
            verify(trainingService).getProgress(ME);
        }

        @Test
        void skillMatrix() throws Exception {
            mockMvc.perform(get("/api/skills/matrix").param("email", OTHER)).andExpect(status().isOk());
            verify(skillMatrixService).getSkillMatrix(ME);
        }

        @Test
        void softSkills() throws Exception {
            mockMvc.perform(get("/api/soft-skills").param("email", OTHER)).andExpect(status().isOk());
            verify(softSkillMatrixService).calculateFor(ME);
        }

        @Test
        void userProfileGetAndPut() throws Exception {
            mockMvc.perform(get("/api/user/me").param("email", OTHER)).andExpect(status().isOk());
            verify(userProfileService).getProfile(ME);

            mockMvc.perform(json(put("/api/user/me").param("email", OTHER), "{}")).andExpect(status().isOk());
            verify(userProfileService).saveProfile(eq(ME), any());
        }

        @Test
        void workstyleStart() throws Exception {
            mockMvc.perform(post("/api/workstyle/start").param("email", OTHER)).andExpect(status().isOk());
            verify(workstyleSessionService).startSession(ME);
        }

        @Test
        void endpointsWorkWithoutAnyEmailParam() throws Exception {
            mockMvc.perform(get("/api/progress")).andExpect(status().isOk());
            mockMvc.perform(get("/api/skills/matrix")).andExpect(status().isOk());
            mockMvc.perform(get("/api/soft-skills")).andExpect(status().isOk());
            mockMvc.perform(get("/api/mindset/roadmaps")).andExpect(status().isOk());
        }
    }

    // ------------------------------------------------------------ body endpoints
    @Nested
    @WithMockUser(username = ME)
    class BodyEmailIsOverridden {

        private final String otherEmail = "\"email\":\"" + OTHER + "\"";

        @Test
        void mindsetTask() throws Exception {
            mockMvc.perform(json(post("/api/mindset/task"), "{" + otherEmail + ",\"taskKey\":\"t-1\",\"completed\":true}"))
                    .andExpect(status().isOk());
            ArgumentCaptor<TrainingTaskRequest> captor = ArgumentCaptor.forClass(TrainingTaskRequest.class);
            verify(trainingService).updateTaskStatus(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo(ME);
            verify(mindsetRoadmapService).getRoadmapDetail(eq(ME), isNull());
        }

        @Test
        void mindsetTaskWithoutBodyEmailIsAcceptedButTaskKeyIsStillRequired() throws Exception {
            mockMvc.perform(json(post("/api/mindset/task"), "{\"taskKey\":\"t-1\"}")).andExpect(status().isOk());
            mockMvc.perform(json(post("/api/mindset/task"), "{\"completed\":true}")).andExpect(status().isBadRequest());
            verify(trainingService).updateTaskStatus(any());
        }

        @ParameterizedTest
        @ValueSource(strings = {"/api/trainer/analyze", "/api/skill-coach/analyze"})
        void adaptiveAnalyze(String path) throws Exception {
            mockMvc.perform(json(post(path), "{" + otherEmail + ",\"question\":\"q\",\"answer\":\"a\"}"))
                    .andExpect(status().isOk());
            ArgumentCaptor<AdaptiveAnalysisRequest> captor = ArgumentCaptor.forClass(AdaptiveAnalysisRequest.class);
            verify(aiAdaptiveService).analyzeAnswer(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo(ME);
        }

        @Test
        void skillCoachAnswerSavesTrainingTaskForPrincipal() throws Exception {
            mockMvc.perform(json(post("/api/skill-coach/answer"),
                            "{" + otherEmail + ",\"roadmapItemId\":\"r-1\",\"question\":\"q\",\"answer\":\"a\"}"))
                    .andExpect(status().isOk());
            ArgumentCaptor<TrainingTaskRequest> captor = ArgumentCaptor.forClass(TrainingTaskRequest.class);
            verify(trainingService).updateTaskStatus(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo(ME);
            assertThat(captor.getValue().getTaskKey()).isEqualTo("r-1");
        }

        @Test
        void softQuestion() throws Exception {
            mockMvc.perform(json(post("/api/trainer/soft-question"), "{" + otherEmail + ",\"roadmapKey\":\"k\"}"))
                    .andExpect(status().isOk());
            ArgumentCaptor<SoftSkillQuestionRequest> captor = ArgumentCaptor.forClass(SoftSkillQuestionRequest.class);
            verify(softSkillQuestionService).generateQuestion(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo(ME);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/api/questions/evaluate", "/api/evaluate", "/api/trainer/evaluate-answer"})
        void evaluateAnswer(String path) throws Exception {
            mockMvc.perform(json(post(path), "{" + otherEmail + ",\"question\":\"q\",\"answer\":\"a\"}"))
                    .andExpect(status().isOk());
            ArgumentCaptor<EvaluateAnswerRequest> captor = ArgumentCaptor.forClass(EvaluateAnswerRequest.class);
            verify(evaluationService).evaluate(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo(ME);
        }

        @Test
        void interviewSession() throws Exception {
            mockMvc.perform(json(post("/api/interview-sessions"), "{" + otherEmail + "}")).andExpect(status().isOk());
            ArgumentCaptor<CreateInterviewSessionRequest> captor = ArgumentCaptor.forClass(CreateInterviewSessionRequest.class);
            verify(interviewSessionService).createSession(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo(ME);
        }

        @Test
        void jobMatchPost() throws Exception {
            mockMvc.perform(json(post("/api/job-match"), "{" + otherEmail + ",\"targetRole\":\"dev\"}"))
                    .andExpect(status().isOk());
            ArgumentCaptor<JobMatchRequest> captor = ArgumentCaptor.forClass(JobMatchRequest.class);
            verify(jobMatchService).match(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo(ME);
        }

        @Test
        void skillPlan() throws Exception {
            mockMvc.perform(json(post("/api/skill-plan"), "{" + otherEmail + "}")).andExpect(status().isOk());
            ArgumentCaptor<SkillPlanRequest> captor = ArgumentCaptor.forClass(SkillPlanRequest.class);
            verify(skillPlanService).buildPlan(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo(ME);
        }

        @Test
        void trainingTaskIsLookedUpAndSavedForPrincipal() throws Exception {
            when(trainingTaskRepository.findByEmailAndTaskKey(anyString(), anyString())).thenReturn(Optional.empty());

            mockMvc.perform(json(post("/api/training/tasks"), "{" + otherEmail + ",\"taskKey\":\"t-1\",\"answerText\":\"x\"}"))
                    .andExpect(status().isOk());

            verify(trainingTaskRepository).findByEmailAndTaskKey(ME, "t-1");
            verify(trainingTaskRepository, never()).findByEmailAndTaskKey(eq(OTHER), anyString());
            ArgumentCaptor<TrainingTask> saved = ArgumentCaptor.forClass(TrainingTask.class);
            verify(trainingTaskRepository).save(saved.capture());
            assertThat(saved.getValue().getEmail()).isEqualTo(ME);
        }
    }

    // ------------------------------------------------------------ workstyle sessions (id-based)
    @Nested
    @WithMockUser(username = ME)
    class WorkstyleSessionOwnership {

        private final UUID id = UUID.randomUUID();

        private WorkstyleSession sessionOf(String email) {
            WorkstyleSession s = new WorkstyleSession();
            s.setEmail(email);
            return s;
        }

        @Test
        void ownSessionCanBeReadAndAnswered() throws Exception {
            when(workstyleSessionService.getSession(id)).thenReturn(Optional.of(sessionOf(ME)));

            mockMvc.perform(get("/api/workstyle/session/" + id)).andExpect(status().isOk());
            mockMvc.perform(json(post("/api/workstyle/answer"), "{\"sessionId\":\"" + id + "\",\"answer\":\"a\"}"))
                    .andExpect(status().isOk());
            verify(workstyleSessionService).answer(id, "a");
        }

        @Test
        void otherUsersSessionIsForbidden() throws Exception {
            when(workstyleSessionService.getSession(id)).thenReturn(Optional.of(sessionOf(OTHER)));

            mockMvc.perform(get("/api/workstyle/session/" + id)).andExpect(status().isForbidden());
            mockMvc.perform(json(post("/api/workstyle/answer"), "{\"sessionId\":\"" + id + "\",\"answer\":\"a\"}"))
                    .andExpect(status().isForbidden());
            verify(workstyleSessionService, never()).answer(any(), any());
        }

        @Test
        void unknownSessionIs404() throws Exception {
            when(workstyleSessionService.getSession(id)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/workstyle/session/" + id)).andExpect(status().isNotFound());
            mockMvc.perform(json(post("/api/workstyle/answer"), "{\"sessionId\":\"" + id + "\",\"answer\":\"a\"}"))
                    .andExpect(status().isNotFound());
            verify(workstyleSessionService, never()).answer(any(), any());
        }
    }

    // ------------------------------------------------------------ unauthenticated
    @Nested
    class Unauthenticated {

        @ParameterizedTest
        @ValueSource(strings = {
                "/api/mindset/roadmaps", "/api/plan/last", "/api/candidate/progress", "/api/dashboard",
                "/api/job-match", "/api/profile/overview", "/api/progress", "/api/skills/matrix",
                "/api/soft-skills", "/api/user/me"
        })
        void getEndpointsReturn401(String path) throws Exception {
            mockMvc.perform(get(path).param("email", OTHER)).andExpect(status().isUnauthorized());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "/api/mindset/task", "/api/trainer/analyze", "/api/trainer/soft-question", "/api/skill-coach/analyze",
                "/api/skill-coach/answer", "/api/questions/evaluate", "/api/evaluate", "/api/trainer/evaluate-answer",
                "/api/interview-sessions", "/api/job-match", "/api/skill-plan", "/api/training/tasks",
                "/api/workstyle/start", "/api/workstyle/answer"
        })
        void postEndpointsReturn401(String path) throws Exception {
            mockMvc.perform(json(post(path).param("email", OTHER), "{\"email\":\"" + OTHER + "\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void noServiceIsCalled() throws Exception {
            mockMvc.perform(get("/api/progress").param("email", OTHER)).andExpect(status().isUnauthorized());
            mockMvc.perform(json(post("/api/training/tasks"), "{\"email\":\"" + OTHER + "\",\"taskKey\":\"t\"}"))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(trainingService, trainingTaskRepository);
        }
    }
}
