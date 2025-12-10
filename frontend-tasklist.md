### Frontend Tasklist – ai-interview-fullstack (Angular)

Organised by feature waves aligned with backend capabilities.


### Wave 1 – Interview Session Foundations

- **1.1 Shared foundations**
  - Create `shared/models` for:
    - `Candidate`, `Position`, `InterviewSession`, `InterviewMessage`, enums (`InterviewPhase`, `SessionStatus`, `InterviewerType`).
    - `SkillMatrix`, `SkillCategory`, `Skill`, `SkillEvaluation`.
  - Create `shared/services/api-interview.service.ts`:
    - Methods:
      - `createSession(payload)`, `getSession(id)`, `listMessages(sessionId)`, `sendCandidateMessage(sessionId, content)`, `transitionPhase(sessionId, targetPhase)`.
  - Create `shared/services/api-skill-matrix.service.ts`:
    - Methods:
      - `getMatrix(id)`, `listMatrices()`, `evaluateSession(sessionId)`.
  - Define shared `HttpInterceptor` for auth headers and error handling.

- **1.2 WebSocket client**
  - Implement `shared/services/interview-websocket.service.ts`:
    - Connect to `ws://.../ws/interview/{sessionId}`.
    - Expose RxJS streams for:
      - `messageCreated$`, `phaseChanged$`, `sessionStatusChanged$`, `errors$`.
    - Reconnect logic and cleanup on navigation.

- **1.3 `interview-session` feature module**
  - Create Angular module `interview-session` with routing:
    - Route: `/interview/:sessionId`.
  - Components:
    - `InterviewShellComponent`:
      - Overall layout (chat + side panels + header).
    - `InterviewChatComponent`:
      - Conversation view with message bubbles and persona avatars.
      - Input box with send, typing indicator, disabled state on session complete.
    - `PhaseIndicatorComponent`:
      - Visual timeline of phases (HR, TECHNICAL, TEAM_FIT, COACHING, RETRO).
      - Shows current phase and completed phases.
    - `PersonaBadgeComponent`:
      - Renders persona type with color/icon and description tooltip.
    - `SkillCoverageSummaryComponent` (minimal v0):
      - Displays basic coverage (e.g. list of skills touched so far).
  - Wiring:
    - On route init:
      - Load session via `api-interview.service`.
      - Connect WebSocket.
      - Subscribe to message and phase streams.
    - On send:
      - Call `sendCandidateMessage`, optimistic UI update, wait for persona replies.

- **1.4 Candidate entry and session bootstrap**
  - Create lightweight `candidate-home` view (or integrate into existing landing) with:
    - Form to select a position (mock or from backend).
    - Button to "Start AI Interview".
  - On submit:
    - Call `createSession`.
    - Navigate to `/interview/:sessionId`.

- **1.5 Basic styling and UX**
  - Use existing Tailwind/SCSS setup to:
    - Make chat UI clean and readable.
    - Color-code personas.
    - Show phase badges clearly.

- **1.6 Tests**
  - Unit tests for WebSocket service (mocks).
  - Component tests for:
    - `InterviewChatComponent` message rendering.
    - `PhaseIndicatorComponent` state transitions.


### Wave 2 – Soft Skill Merger UI

- **2.1 Shared models & service**
  - Extend `shared/models` with:
    - `SoftSkillDefinition`, `MergedSoftSkill`, `SoftSkillMergeResult`.
  - Implement `shared/services/soft-skill-merger.service.ts`:
    - Methods:
      - `getMergedForSession(sessionId)`.
      - `triggerMerge(sessionId)`.
      - `listDefinitions()`, `createDefinition()`, `updateDefinition()`, `deleteDefinition()`.

- **2.2 `soft-skill-merger` feature module**
  - Create module with routing:
    - `/soft-skill-merger/session/:sessionId`.
  - Components:
    - `SoftSkillProfilePageComponent`:
      - High-level page layout; loads merged result and definitions.
    - `SoftSkillRadarComponent`:
      - Visualizes `radarData` from backend using chart library.
    - `DisagreementMatrixComponent`:
      - Table or heatmap showing persona scores vs merged score for each soft skill.
      - Highlights skills with high disagreement.
    - `SoftSkillSummaryComponent`:
      - Shows narrative summary and key flags.
  - Actions:
    - "Re-run merge" button (calls `triggerMerge` and refreshes data).

- **2.3 Admin views for soft skill definitions**
  - Optional admin route `/admin/soft-skills`:
    - List, create, edit, archive `SoftSkillDefinition`.

- **2.4 Integration with interview flow**
  - From `interview-session` module:
    - Add link/button after TEAM_FIT phase completion to open soft skill profile.

- **2.5 Tests**
  - Component tests for:
    - Radar rendering with sample data.
    - Disagreement matrix highlighting thresholds.


### Wave 3 – Growth Coach UI

- **3.1 Shared models & service**
  - Extend `shared/models` with:
    - `GrowthArea`, `GrowthPlan`, `GrowthStep`, `CoachInteraction`.
  - Implement `shared/services/growth-coach.service.ts`:
    - Methods:
      - `generatePlan(sessionId)`, `getPlan(planId | sessionId)`, `updatePlan(planId, patch)`.
      - `addInteraction(planId, payload)`, `listInteractions(planId)`.

- **3.2 `growth-coach` feature module**
  - Routing:
    - `/growth-coach/session/:sessionId`.
  - Components:
    - `GrowthCoachBoardComponent`:
      - Columnar layout (e.g. areas as columns, steps as cards).
    - `GrowthAreaColumnComponent`:
      - Lists steps grouped by status.
    - `GrowthStepCardComponent`:
      - Shows step details, status, due date; allows status toggle.
    - `CoachTimelineComponent`:
      - Timeline of `CoachInteraction` entries.
    - `GrowthStepModalComponent`:
      - View/edit step details, notes, and due dates.
  - Flows:
    - On route init:
      - If no plan exists, call `generatePlan`.
      - Then load plan and interactions.

- **3.3 Integration with candidate experience**
  - Add link/button from post-interview view to open growth coach plan.
  - Show summary of key growth areas on candidate dashboard.

- **3.4 Tests**
  - Component tests for:
    - Rendering plan board.
    - Status transitions and interaction creation.


### Wave 4 – Multi-Interviewer Panel & Behaviour Lab

- **4.1 `multi-interviewer-panel` module**
  - Routing:
    - `/panel/session/:sessionId`.
  - Components:
    - `PanelShellComponent`:
      - Layout showing:
        - Central chat timeline.
        - Side-by-side persona panes.
        - Top bar with session and phase info.
    - `PersonaViewComponent`:
      - Shows persona-specific perspective:
        - Their questions, scores, notes, overrides.
    - `ConflictHighlightComponent`:
      - Summarizes disagreements (pulled from soft skill merger and skill evaluations).
    - `OverrideToolsComponent`:
      - Allow interviewer to override scores, add notes, or adjust prompts.
  - Services:
    - Reuse `api-interview`, `soft-skill-merger`, `growth-coach` services.
    - Add `panel-state.service.ts` to orchestrate combined data (messages, scores, merged soft skills).

- **4.2 `behaviour-lab` admin module**
  - Routing:
    - `/behaviour-lab`.
  - Views:
    - `SignalsDashboardComponent`:
      - Charts showing distributions of `BehaviourSignal` metrics (latency, length, sentiment).
    - `ExperimentsListComponent`:
      - List experiments, statuses, and key metrics.
    - `ExperimentDetailComponent`:
      - Variants, assignments, and simple outcome summaries.
    - `BiasSnapshotListComponent`:
      - List `BiasCalibrationSnapshot` entries; click-through to metrics view.
  - Services:
    - `behaviour.service.ts`:
      - Methods for:
        - Listing signals per session.
        - CRUD for experiments and variants.
        - Reading bias snapshots.

- **4.3 Shared admin layout**
  - Create `admin-shell` layout component used by soft-skill definitions, behaviour lab, and other admin views.
  - Include sidebar navigation between:
    - Soft Skills, Behaviour Lab, Experiments, Settings.

- **4.4 Access control**
  - Guard routes:
    - Candidate-only vs interviewer vs admin modules.
  - Implement simple `AuthGuard` and `RoleGuard` (use existing auth if present).

- **4.5 Tests**
  - Component tests for:
    - Conflict highlighting logic in panel.
    - Behaviour lab charts using mock data.


### Cross-cutting Frontend Tasks

- **Design system & shared components**
  - Build shared UI elements in `shared/components`:
    - Buttons, tags, chips, cards, modal, table, icons.
    - Persona avatar component (used in chat and panel).
  - Ensure visual consistency across candidate and admin UIs.

- **State management**
  - Use RxJS-based services for local state; consider NgRx or NGXS only if complexity increases.
  - Keep interview and plan state scoped to feature modules to reduce coupling.

- **Error handling & loading states**
  - Standardize loading spinners and error banners.
  - For WebSocket disconnects, show non-intrusive notification and retry.

- **Internationalisation / copy**
  - Centralise user-facing messages (prompts, labels) for easy experimentation.

- **Analytics hooks**
  - Add lightweight analytics events for:
    - Session start/completion.
    - Major UI actions (open soft skill profile, view growth plan, open behaviour lab).


