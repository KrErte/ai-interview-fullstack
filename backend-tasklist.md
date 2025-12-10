### Backend Tasklist – ai-interview-fullstack

Organised by implementation waves aligned with the system design.


### Wave 1 – Interview & Skill Matrix Foundations

- **1.1 Core enums & shared types**
  - Define `InterviewPhase`, `SessionStatus`, `InterviewerType` enums in `shared` module.
  - Expose them to other modules via a shared package (e.g. `shared.domain`).

- **1.2 Core entities & persistence**
  - Create `Candidate` entity, repository, and basic CRUD.
  - Create `Position` entity, repository, and basic CRUD.
  - Create `SkillMatrix` aggregate:
    - `SkillMatrix` (root), `SkillCategory`, `Skill` entities.
    - JPA mappings (e.g. `@OneToMany` with cascading, ordering).
  - Create `InterviewSession` entity:
    - Fields: `id`, `candidate`, `position`, `currentPhase`, `status`, `activePersonaType`, `phaseHistoryJson`, timestamps.
  - Create `InterviewMessage` entity:
    - Fields: `id`, `session`, `senderType`, `personaType`, `phase`, `content`, `llmModel`, `metadataJson`, `timestamp`.
  - Create `SkillEvaluation` entity:
    - Fields: `id`, `session`, `skill`, `personaType`, `phase`, `score`, `evidence`, `confidence`, `tagsJson`, timestamps.
  - Create `InterviewerPersona` entity:
    - Fields: `id`, `type`, `displayName`, `description`, `goalsJson`, `tone`, `focusSkillsJson`, `promptTemplate`, `configJson`.

- **1.3 Repositories & migrations**
  - Add Spring Data repositories for all above entities.
  - Add Flyway/Liquibase migrations (tables, FKs, indices):
    - Index on `InterviewMessage.session_id`, `timestamp`.
    - Index on `SkillEvaluation.session_id`, `skill_id`.

- **1.4 Interview flow & orchestration services**
  - Implement `InterviewSessionService`:
    - Create session for candidate/position/skillMatrix.
    - Manage status, pause/resume, complete, cancel.
  - Implement `InterviewPhaseService`:
    - Transition `InterviewSession` through `InterviewPhase` enum with validation.
    - Emit `InterviewPhaseChanged` events.
  - Implement `InterviewMessageService`:
    - Append candidate and persona messages.
    - Query paginated chat history by session and phase.
    - Emit `InterviewMessageAppended` events.
  - Implement `PanelOrchestratorService` v0:
    - For each candidate message, decide which persona responds next.
    - Call LLM gateway with persona prompt and context.
    - Create persona `InterviewMessage` entries and publish over WebSocket.

- **1.5 Skill evaluation services**
  - Implement `SkillEvaluatorService`:
    - Use `InterviewSession` history as context.
    - For each `Skill` in relevant `SkillMatrix`, call LLM to obtain score, evidence, confidence.
    - Persist `SkillEvaluation` entries.
    - Emit `SkillEvaluated` events (per evaluation or batch).

- **1.6 REST controllers – Interview**
  - Implement `/api/v1/interview/sessions`:
    - POST create session.
    - GET by id, list for candidate/position.
  - Implement `/api/v1/interview/sessions/{id}/messages`:
    - GET list messages (with pagination).
    - POST candidate message (async AI response).
  - Implement `/api/v1/interview/sessions/{id}/phase-transition`:
    - POST transition to target phase.

- **1.7 REST controllers – Skill Matrix**
  - Implement CRUD for `/api/v1/skill-matrix/matrices`.
  - Implement `/api/v1/skill-matrix/sessions/{sessionId}/evaluate` using `SkillEvaluatorService`.

- **1.8 WebSocket infrastructure**
  - Configure WebSocket endpoint `ws://.../ws/interview/{sessionId}`:
    - Spring messaging config, STOMP or raw JSON.
    - Authentication and session binding.
  - Implement message broadcasting for:
    - New `InterviewMessage` (`MESSAGE_CREATED`).
    - Phase changes (`PHASE_CHANGED`).
    - Session status changes (`SESSION_STATUS_CHANGED`).

- **1.9 Domain event infrastructure**
  - Implement `DomainEvent` base type and `DomainEventPublisher` abstraction.
  - Implement in-process publisher using Spring `ApplicationEventPublisher`.
  - Create handlers where necessary:
    - E.g. behaviour signal extractor (stubbed) listening to `InterviewMessageAppended`.

- **1.10 Tests**
  - Unit tests for:
    - Enum-based transitions and validation logic.
    - `InterviewSessionService`, `InterviewPhaseService`, `InterviewMessageService`.
    - `SkillEvaluatorService` (with mocked LLM gateway).
  - Integration tests for:
    - REST endpoints, JSON contracts.
    - WebSocket message flow (happy-path).


### Wave 2 – Soft Skill Merger Module

- **2.1 Domain & persistence**
  - Implement `SoftSkillDefinition` entity + repository.
  - Implement `SoftSkillMergeResult` entity:
    - FK to `InterviewSession`.
    - `overallSummary`, `radarDataJson`, `disagreementSummary`, timestamps.
  - Implement `MergedSoftSkill` entity:
    - FK to `SoftSkillMergeResult`.
    - FK to `SoftSkillDefinition`.
    - Fields: `mergedScore`, `scoreByPersonaJson`, `disagreementLevel`, `flagsJson`.
  - Migrations for all new tables, FKs, and indices.

- **2.2 Collector and algorithm services**
  - Implement `SoftSkillCollector`:
    - Pull soft-skill-related `SkillEvaluation` from HR, TECH, TEAM_LEAD personas.
  - Implement `MergeAlgorithm` v0.1:
    - Weighted average with persona-specific weights.
    - Basic disagreement metric (e.g. score variance).
  - Implement `DisagreementDetector`:
    - Flags skills with variance above threshold.
  - Implement `SummaryGenerator`:
    - LLM-assisted summary of merged soft-skill profile and disagreements.
  - Implement `RadarDataBuilder`:
    - Transform merged results into radar-ready structure for frontend.

- **2.3 Orchestration service**
  - Implement `SoftSkillMergerService`:
    - Public API: `mergeForSession(sessionId)` returning `SoftSkillMergeResult`.
    - Uses collector, merge algorithm, disagreement detector, summary, radar builder.
    - Persists `SoftSkillMergeResult` and `MergedSoftSkill` list.
    - Emits `SoftSkillMerged` event after success.

- **2.4 REST controllers – Soft Skill Merger**
  - Implement `/api/v1/soft-skill-merger/sessions/{sessionId}/merged`:
    - GET returns latest `SoftSkillMergeResult` (404 if none).
  - Implement `/api/v1/soft-skill-merger/sessions/{sessionId}/merge`:
    - POST triggers (re-)merge.
  - Implement CRUD for `/api/v1/soft-skill-merger/definitions`:
    - Managing `SoftSkillDefinition` catalogue.

- **2.5 Event handling**
  - Subscribe `SoftSkillMergerService` to relevant events (initially in-process):
    - Option 1: On `InterviewPhaseChanged` to `TEAM_FIT` or `COACHING`, trigger merge.
    - Option 2: On `SkillEvaluated` completion for soft-skill subset.
  - Publish `SoftSkillMerged` with contract defined in system design.

- **2.6 Tests**
  - Unit tests for:
    - `MergeAlgorithm`, `DisagreementDetector`, `SummaryGenerator`, `RadarDataBuilder`.
  - Integration tests:
    - Flow from `SkillEvaluation` set to persisted `SoftSkillMergeResult`.
    - REST endpoints and JSON responses.


### Wave 3 – Growth Coach Module

- **3.1 Domain & persistence**
  - Implement `GrowthPlan` entity:
    - FK to `InterviewSession`.
    - `summary`, timestamps.
  - Implement `GrowthArea` entity:
    - FK to `GrowthPlan`.
    - Fields: `name`, `description`, `priority`.
  - Implement `GrowthStep` entity:
    - FK to `GrowthArea`.
    - Fields: `title`, `description`, `status`, `dueDate`.
  - Implement `CoachInteraction` entity:
    - FK to `GrowthPlan`.
    - Fields: `type`, `content`, `channel`, `timestamp`.
  - Add migrations for tables, FKs, and indices.

- **3.2 Services**
  - Implement `GrowthPlanBuilderService` (rule-based v0.1):
    - Input: `SoftSkillMergeResult`, `SkillEvaluation` set, possibly `Position`.
    - Output: initial `GrowthPlan` with `GrowthArea` and `GrowthStep` hierarchy.
    - Rules: e.g. low soft-skill scores → immediate growth areas; high technical strengths → stretch goals.
  - Implement `CoachInteractionService`:
    - Create and list `CoachInteraction` items.
    - Provide templated nudge generation (stub rule-based or LLM-backed).

- **3.3 Orchestration & event handling**
  - Implement `GrowthCoachOrchestrator`:
    - Listens to `SoftSkillMerged`.
    - Calls `GrowthPlanBuilderService` to create plan if none exists for the session.
    - Emits `GrowthPlanCreated` event.

- **3.4 REST controllers – Growth Coach**
  - Implement `/api/v1/growth-coach/sessions/{sessionId}/plan`:
    - POST generate or regenerate plan.
  - Implement `/api/v1/growth-coach/plans/{planId}`:
    - GET plan with nested areas and steps.
    - PATCH to update plan metadata or statuses.
  - Implement `/api/v1/growth-coach/plans/{planId}/interactions`:
    - POST new `CoachInteraction`.
    - GET list interactions.

- **3.5 Tests**
  - Unit tests for `GrowthPlanBuilderService` and `CoachInteractionService`.
  - Integration tests for:
    - End-to-end flow from `SoftSkillMerged` to stored `GrowthPlan`.
    - REST endpoints and JSON structures.


### Wave 4 – Behaviour & Experimentation

- **4.1 BehaviourSignal extraction**
  - Implement `BehaviourSignal` entity + repository.
  - Implement `BehaviourSignalExtractor`:
    - On `InterviewMessageAppended`, compute:
      - Latency: difference from previous candidate response or persona question.
      - Length: token/character counts.
      - Sentiment (placeholder or simple heuristic/LLM call).
    - Persist `BehaviourSignal` records.
    - Emit `BehaviourSignalCaptured` events.

- **4.2 InterviewOutcome**
  - Implement `InterviewOutcome` entity:
    - FK to `InterviewSession`.
    - Fields: `label`, `reason`, `decidedBy`, `decidedAt`.
  - Implement service to record outcomes (human reviewer or auto).
  - Emit `InterviewOutcomeRecorded` event.

- **4.3 Experimentation domain**
  - Implement `Experiment`, `ExperimentVariant`, `ExperimentAssignment` entities + repositories.
  - Implement `ExperimentService`:
    - CRUD for experiments and variants.
    - Assignment logic (simple randomisation with allocation ratios).
  - Implement `ExperimentAssignmentService`:
    - Assign candidates or sessions to variants on creation.
    - Emit `ExperimentAssigned` events.

- **4.4 REST controllers – Behaviour & experiments**
  - Implement `/api/v1/behaviour/sessions/{sessionId}/signals`:
    - GET behaviour signals for a session (filter by type/time range).
  - Implement `/api/v1/behaviour/experiments`:
    - POST create experiments.
    - GET list experiments.
  - Implement `/api/v1/behaviour/experiments/{id}`:
    - GET experiment details, variants, assignments.
  - Implement `/api/v1/behaviour/experiments/{id}/assignments`:
    - POST manual assignment (optional).
  - Implement `/api/v1/behaviour/bias-snapshots` (read-only for now, see below).

- **4.5 BiasCalibrationSnapshot**
  - Implement `BiasCalibrationSnapshot` entity:
    - `metricsJson`, `notes`, timestamps.
  - Implement `BiasCalibrationService`:
    - Aggregates experiments, outcomes, and behaviour data.
    - Creates snapshot records periodically or on demand.
  - Expose GET `/api/v1/behaviour/bias-snapshots` (list + details).

- **4.6 Event bus adapter prep**
  - Implement `EventBusAdapter` interface with default no-op or log-backed implementation.
  - Add configuration for mapping domain events to outbound event bus messages (topic names, payload schemas) to ease later extraction into microservices.

- **4.7 Tests**
  - Unit tests for:
    - `BehaviourSignalExtractor`.
    - `ExperimentService` and `ExperimentAssignmentService`.
    - `BiasCalibrationService` metrics logic.
  - Integration tests for:
    - Behaviour signals recorded from message flow.
    - Experiment assignments at session creation.


### Cross-cutting & Infrastructure Tasks

- **Logging & observability**
  - Add structured logging for:
    - LLM calls (model, latency, prompt template ids).
    - Domain events.
  - Expose basic metrics (e.g. via Micrometer) for:
    - Interview sessions created/completed.
    - LLM call counts and error rates.

- **Security**
  - Add authentication/authorisation middleware (if not present).
  - Role-based protection:
    - Candidate vs interviewer vs admin routes.
  - Ensure sensitive data is excluded or redacted from logs and events.

- **Configuration**
  - Centralise LLM gateway configuration (model, temperature, API keys).
  - Configure feature flags for:
    - Enabling/disabling experimentation.
    - Toggling behaviour signal extraction or growth coach auto-generation.


