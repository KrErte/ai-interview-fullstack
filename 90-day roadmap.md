### 90-Day Roadmap – ai-interview-fullstack

Timeline assumes ~2-week iterations (6 sprints) with overlapping backend/frontend work.


### Phase 1 (Days 1–15) – Foundations & Interview Spine

- **Goals**
  - Establish core interview and skill-matrix domain.
  - Deliver end-to-end candidate interview chat with a single AI persona.

- **Backend milestones**
  - Implement enums: `InterviewPhase`, `SessionStatus`, `InterviewerType`.
  - Implement entities and repositories:
    - `Candidate`, `Position`, `InterviewSession`, `InterviewMessage`.
    - `SkillMatrix`, `SkillCategory`, `Skill`, `SkillEvaluation`.
    - `InterviewerPersona`.
  - Implement services:
    - `InterviewSessionService`, `InterviewPhaseService`, `InterviewMessageService`.
    - `SkillEvaluatorService` (LLM integration stub or basic).
  - Implement REST endpoints:
    - `/api/v1/interview/sessions`, `/api/v1/interview/sessions/{id}/messages`, `/api/v1/interview/sessions/{id}/phase-transition`.
    - `/api/v1/skill-matrix/matrices`, `/api/v1/skill-matrix/sessions/{sessionId}/evaluate`.
  - Configure WebSocket endpoint for interview chat and broadcast minimal events.

- **Frontend milestones**
  - Set up `shared/models` and `shared/services` for interview and skill matrix.
  - Implement WebSocket client for interview sessions.
  - Implement `interview-session` module:
    - Chat UI with persona tagging.
    - Phase indicator basic timeline.
  - Create simple candidate entry page to start an interview.

- **Validation**
  - Manual E2E: start interview, send messages, see AI persona replies, basic skill evaluations available via API.


### Phase 2 (Days 16–30) – Multi-Persona Panel & Robust Interview Flow

- **Goals**
  - Upgrade from single AI persona to multi-interviewer panel.
  - Strengthen phase routing and evidence capture.

- **Backend milestones**
  - Enhance `PanelOrchestratorService`:
    - Support HR/TECH/TEAM_LEAD personas with configurable prompts.
    - Route prompts based on `InterviewPhase`.
  - Extend `InterviewMessage` and `SkillEvaluation` to capture persona and phase metadata.
  - Improve `SkillEvaluatorService` to use full conversation context and persona evaluations.
  - Harden WebSocket broadcasts:
    - `MESSAGE_CREATED`, `PHASE_CHANGED`, `SESSION_STATUS_CHANGED`.

- **Frontend milestones**
  - Improve `interview-session` UI:
    - Persona badges and clear separation of voices.
    - Better loading/error states for WebSocket and API.
  - Add basic side panel summarising which skills have been touched.
  - Prepare shared components and styling foundations for later modules.

- **Validation**
  - Demonstrate HR → TECHNICAL → TEAM_FIT progression with different persona tones.
  - Capture and view per-persona `SkillEvaluation` outputs via dev/admin tools.


### Phase 3 (Days 31–45) – Soft Skill Merger v0.1

- **Goals**
  - Implement soft-skill merger backend and corresponding UI for merged profiles.

- **Backend milestones**
  - Implement soft-skill merger module:
    - Entities: `SoftSkillDefinition`, `SoftSkillMergeResult`, `MergedSoftSkill`.
    - Services: `SoftSkillCollector`, `MergeAlgorithm`, `DisagreementDetector`, `SummaryGenerator`, `RadarDataBuilder`.
    - Orchestrator: `SoftSkillMergerService` with `mergeForSession`.
  - Expose REST:
    - `/api/v1/soft-skill-merger/sessions/{sessionId}/merged`.
    - `/api/v1/soft-skill-merger/sessions/{sessionId}/merge`.
    - CRUD on `/api/v1/soft-skill-merger/definitions`.
  - Emit `SoftSkillMerged` events.

- **Frontend milestones**
  - Implement `soft-skill-merger` module:
    - `SoftSkillProfilePage` with radar chart, disagreement matrix, and summary.
    - Hook into backend via `soft-skill-merger.service.ts`.
  - Add navigation from the interview session (post TEAM_FIT phase) to the soft-skill profile page.

- **Validation**
  - Run full interview; see HR/TECH/TL soft-skill scores merged into a radar with disagreements highlighted and text summary displayed.


### Phase 4 (Days 46–60) – Growth Coach Engine v0.1

- **Goals**
  - Turn merged and per-skill evaluations into a coherent growth plan.

- **Backend milestones**
  - Implement growth coach domain:
    - `GrowthPlan`, `GrowthArea`, `GrowthStep`, `CoachInteraction`.
  - Implement services:
    - `GrowthPlanBuilderService` (rule-based v0.1).
    - `CoachInteractionService`.
  - Orchestrate on `SoftSkillMerged`:
    - `GrowthCoachOrchestrator` creates a plan if not existing.
    - Emit `GrowthPlanCreated`.
  - REST:
    - `/api/v1/growth-coach/sessions/{sessionId}/plan`.
    - `/api/v1/growth-coach/plans/{planId}`.
    - `/api/v1/growth-coach/plans/{planId}/interactions`.

- **Frontend milestones**
  - Implement `growth-coach` module:
    - Plan board (areas & steps) and step modal.
    - Coaching timeline for `CoachInteraction`.
  - Integrate into candidate and coach/admin views:
    - Link from post-interview experience to view growth plan.

- **Validation**
  - After running an interview and merger, auto-generated growth plan visible in UI and editable; interactions can be added and displayed.


### Phase 5 (Days 61–75) – Behaviour Signals & Experimentation

- **Goals**
  - Start building the behavioural data layer and experimentation framework.

- **Backend milestones**
  - Implement behaviour extraction:
    - `BehaviourSignal` entity and repository.
    - `BehaviourSignalExtractor` listening to `InterviewMessageAppended`.
  - Implement `InterviewOutcome` entity and service.
  - Implement experimentation domain:
    - `Experiment`, `ExperimentVariant`, `ExperimentAssignment`.
    - `ExperimentService` and `ExperimentAssignmentService`.
  - REST:
    - `/api/v1/behaviour/sessions/{sessionId}/signals`.
    - `/api/v1/behaviour/experiments` and `/api/v1/behaviour/experiments/{id}`.
    - `/api/v1/behaviour/experiments/{id}/assignments`.
  - Implement `BiasCalibrationSnapshot` entity and `BiasCalibrationService` with GET `/api/v1/behaviour/bias-snapshots`.

- **Frontend milestones**
  - Implement `behaviour-lab` module:
    - Initial dashboards for behaviour signals and experiment lists.
  - Extend shared models and `behaviour.service.ts` for new APIs.

- **Validation**
  - Visualise latency/length/sentiment distributions for sample sessions.
  - Create an experiment and see assignments recorded for new sessions.


### Phase 6 (Days 76–90) – Multi-Interviewer Panel, Hardening & Event Bus Prep

- **Goals**
  - Provide rich panel dashboard, harden the system, and prepare for future microservices.

- **Backend milestones**
  - Refine panel orchestrator to expose more structured signals to frontend (e.g. persona reasoning snippets).
  - Implement `EventBusAdapter` for key events:
    - `SoftSkillMerged`, `GrowthPlanCreated`, `BehaviourSignalCaptured`, `InterviewOutcomeRecorded`, `ExperimentAssigned`.
  - Add structured logging and metrics for:
    - LLM usage, latency, and failures.
    - Event throughput.
  - Security review:
    - Role-based API protection, sensitive data redaction in logs/events.

- **Frontend milestones**
  - Implement `multi-interviewer-panel` module:
    - Persona views, conflict highlighting, override tools.
  - Improve admin navigation:
    - Shared admin shell linking soft-skill definitions, behaviour lab, experiments, and settings.
  - UX and performance polish across chat, merger, and growth coach flows.

- **Validation**
  - Demonstrate full system:
    - Candidate completes multi-phase interview.
    - Panel views persona activity and conflicts.
    - Soft-skill profile and growth plan generated.
    - Behaviour signals and experiments visible in behaviour lab.


### Risks & Dependencies (Ongoing)

- **LLM integration**
  - Mitigate by keeping a stable gateway abstraction and fallbacks (stub/scored heuristics) for early testing.
- **Data privacy and bias**
  - Implement anonymisation where possible.
  - Use `BiasCalibrationSnapshot` and behaviour lab views to monitor.
- **Scope control**
  - Treat visual polish and advanced analytics as stretch goals, not blockers, for each phase.


