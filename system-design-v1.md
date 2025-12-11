### 1. High-Level Architecture

- **System spine & runtime**
  - **Backend**: Spring Boot modular monolith, structured into logical modules: `interview`, `skillmatrix`, `softskillmerger`, `growthcoach`, `behaviour`, `shared`.
  - **Frontend**: Angular SPA with feature modules matching backend domains: `interview-session`, `skill-matrix`, `soft-skill-merger`, `growth-coach`, `multi-interviewer-panel`, `behaviour-lab`, plus `shared` libraries (models, services, components).
  - **AI/LLM access**: Pluggable LLM gateway (HTTP client + strategy interfaces) hidden behind orchestration services; later replaceable with dedicated LLM microservice.
  - **Data**: Relational DB (current H2/PostgreSQL) as system-of-record; analytic/event store added later for behaviour and experimentation.

- **Experience layer**
  - **Candidate App (Angular)**:
    - Interview chat UI (conversational, persona-labelled messages).
    - Phase indicator (HR → TECHNICAL → TEAM_FIT → COACHING/RETRO).
    - Skill and feedback views: per-skill rubric, evidence snippets, merged soft-skill radar, growth plan preview.
  - **Multi-Interviewer Panel / Interviewer App**:
    - Timeline of AI panel turns and candidate responses.
    - Persona-specific views (HR, Tech, Team Lead) with override tools: edit questions, adjust scores, add human notes.
    - Conflict surfacing: disagreement matrix and flags when personas diverge on soft skills.
  - **Coach & Admin Console**:
    - Growth plan management board (areas → steps → timeline).
    - Analytics and behaviour lab dashboards (signals, experiments, bias calibration).
    - Configuration for skill matrices, rubrics, persona profiles, and experiment variants.

- **AI Multi-Agent & Orchestration layer**
  - **Multi-Interviewer AI Panel**
    - Persona definitions (HR, TECH, TL, COACH) comprising: role description, tone, objectives, risk tolerances, skill focus.
    - **Panel Orchestrator Service**:
      - Manages interview phases and routes prompts to relevant personas.
      - Coordinates turn-taking between personas and candidate (chat loop).
      - Aggregates persona feedback and raises disagreements or bias alerts.
  - **Skill Matrix Interviewer Engine**
    - Uses `SkillMatrix` definitions and rubrics to:
      - Select next questions based on coverage, difficulty, and evidence gaps.
      - Generate persona-specific questions and follow-ups.
      - Capture structured `SkillEvaluation` objects with evidence snippets and confidence.
  - **Soft Skill Merger v0.1**
    - Collects soft-skill evaluations from HR, Tech, TL personas.
    - Merges into `SoftSkillMergeResult` and `MergedSoftSkill` records.
    - Flags disagreement, uncertainty, and potential bias.
    - Produces radar-ready representations and narrative summaries.
  - **Growth Coach Engine**
    - Transforms evaluations and merged skills into `GrowthPlan` with `GrowthArea` and `GrowthStep`.
    - Produces coaching messages (`CoachInteraction`) and longitudinal nudges.
  - **Behavioural Science Layer**
    - Extracts `BehaviourSignal` objects from raw interaction logs (latency, verbosity, sentiment v0.1).
    - Manages experimentation entities: `Experiment`, `ExperimentVariant`, `ExperimentAssignment`.
    - Produces `BiasCalibrationSnapshot` and `InterviewOutcome` for modelling.

- **Event & data layers**
  - **Domain model**:
    - Core objects: `Candidate`, `Position`, `SkillMatrix`, `Skill`, `InterviewSession`, `InterviewMessage`, `SkillEvaluation`, `SoftSkillMergeResult`, `GrowthPlan`, behaviour and experiment entities.
  - **Events (Phase 0 – modular monolith)**:
    - In-process domain events published via Spring `ApplicationEventPublisher` and/or a light event bus abstraction.
    - Events persisted to an `EventLog` table for replay and analytics.
  - **Event bus evolution**:
    - Phase 1: map internal events to external bus (Kafka/RabbitMQ) via adapter module.
    - Phase 2: extract `softskillmerger`, `growthcoach`, and panel orchestrator into independent services consuming/producing events.
    - Phase 3: dedicated behaviour/analytics pipeline and talent graph service.


### 2. Domain Model Diagram + JSON Schemas

#### 2.1 Domain model overview (textual diagram)

- **Core interview**
  - `Candidate` 1..* `InterviewSession`
  - `Position` 1..* `InterviewSession`
  - `InterviewSession` 1..* `InterviewPhase` (conceptual via `currentPhase` + history)
  - `InterviewSession` 1..* `InterviewMessage` (candidate and persona messages)
  - `InterviewSession` 1..* `SkillEvaluation`
  - `InterviewSession` 0..1 `SoftSkillMergeResult`
  - `InterviewSession` 0..1 `GrowthPlan`

- **Skill matrix**
  - `SkillMatrix` 1..* `SkillCategory` 1..* `Skill`
  - `Skill` 1..* `SkillEvaluation`

- **Soft-skill merger**
  - `SoftSkillDefinition` 1..* `MergedSoftSkill`
  - `SoftSkillMergeResult` 1..* `MergedSoftSkill`

- **Growth coach**
  - `GrowthPlan` 1..* `GrowthArea` 1..* `GrowthStep`
  - `GrowthPlan` 0..* `CoachInteraction`

- **Behaviour & experimentation**
  - `InterviewSession` 1..* `BehaviourSignal`
  - `InterviewSession` 0..1 `InterviewOutcome`
  - `Experiment` 1..* `ExperimentVariant`
  - `Experiment` 1..* `ExperimentAssignment` (linked to `InterviewSession`/`Candidate`)
  - `BiasCalibrationSnapshot` references aggregates of experiments and outcomes.

#### 2.2 JSON Schemas (simplified)

- **Candidate**

```json
{
  "type": "object",
  "required": ["id", "fullName", "email"],
  "properties": {
    "id": { "type": "string", "format": "uuid" },
    "fullName": { "type": "string" },
    "email": { "type": "string", "format": "email" },
    "yearsOfExperience": { "type": "number" },
    "primaryStack": { "type": "string" },
    "location": { "type": "string" },
    "metadata": { "type": "object", "additionalProperties": true }
  }
}
```

- **Position**

```json
{
  "type": "object",
  "required": ["id", "title"],
  "properties": {
    "id": { "type": "string", "format": "uuid" },
    "title": { "type": "string" },
    "level": { "type": "string" },
    "department": { "type": "string" },
    "description": { "type": "string" },
    "skillMatrixId": { "type": "string", "format": "uuid" }
  }
}
```

- **SkillMatrix, SkillCategory, Skill**

```json
{
  "SkillMatrix": {
    "type": "object",
    "required": ["id", "name", "categories"],
    "properties": {
      "id": { "type": "string", "format": "uuid" },
      "name": { "type": "string" },
      "description": { "type": "string" },
      "version": { "type": "string" },
      "categories": {
        "type": "array",
        "items": { "$ref": "#/SkillCategory" }
      }
    }
  },
  "SkillCategory": {
    "type": "object",
    "required": ["id", "name", "skills"],
    "properties": {
      "id": { "type": "string", "format": "uuid" },
      "name": { "type": "string" },
      "weight": { "type": "number" },
      "skills": {
        "type": "array",
        "items": { "$ref": "#/Skill" }
      }
    }
  },
  "Skill": {
    "type": "object",
    "required": ["id", "name"],
    "properties": {
      "id": { "type": "string", "format": "uuid" },
      "name": { "type": "string" },
      "description": { "type": "string" },
      "weight": { "type": "number" },
      "rubric": { "type": "object", "additionalProperties": true }
    }
  }
}
```

- **Enums**

```json
{
  "InterviewPhase": {
    "enum": ["HR", "TECHNICAL", "TEAM_FIT", "COACHING", "RETRO"]
  },
  "SessionStatus": {
    "enum": ["PENDING", "ACTIVE", "PAUSED", "COMPLETED", "CANCELLED"]
  },
  "InterviewerType": {
    "enum": ["HR", "TECH", "TEAM_LEAD", "COACH", "SYSTEM"]
  }
}
```

- **InterviewerPersona**

```json
{
  "type": "object",
  "required": ["id", "type", "displayName"],
  "properties": {
    "id": { "type": "string", "format": "uuid" },
    "type": { "$ref": "#/InterviewerType" },
    "displayName": { "type": "string" },
    "description": { "type": "string" },
    "goals": { "type": "array", "items": { "type": "string" } },
    "tone": { "type": "string" },
    "focusSkills": { "type": "array", "items": { "type": "string" } },
    "promptTemplate": { "type": "string" },
    "config": { "type": "object", "additionalProperties": true }
  }
}
```

- **InterviewSession**

```json
{
  "type": "object",
  "required": ["id", "candidateId", "positionId", "currentPhase", "status"],
  "properties": {
    "id": { "type": "string", "format": "uuid" },
    "candidateId": { "type": "string", "format": "uuid" },
    "positionId": { "type": "string", "format": "uuid" },
    "currentPhase": { "$ref": "#/InterviewPhase" },
    "status": { "$ref": "#/SessionStatus" },
    "activePersonaType": { "$ref": "#/InterviewerType" },
    "phaseHistory": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "phase": { "$ref": "#/InterviewPhase" },
          "startedAt": { "type": "string", "format": "date-time" },
          "endedAt": { "type": "string", "format": "date-time" }
        }
      }
    },
    "createdAt": { "type": "string", "format": "date-time" },
    "updatedAt": { "type": "string", "format": "date-time" }
  }
}
```

- **InterviewMessage**

```json
{
  "type": "object",
  "required": ["id", "sessionId", "senderType", "phase", "content", "timestamp"],
  "properties": {
    "id": { "type": "string", "format": "uuid" },
    "sessionId": { "type": "string", "format": "uuid" },
    "senderType": {
      "enum": ["CANDIDATE", "PERSONA", "SYSTEM"]
    },
    "personaType": { "$ref": "#/InterviewerType" },
    "phase": { "$ref": "#/InterviewPhase" },
    "content": { "type": "string" },
    "llmModel": { "type": "string" },
    "metadata": { "type": "object", "additionalProperties": true },
    "timestamp": { "type": "string", "format": "date-time" }
  }
}
```

- **SkillEvaluation**

```json
{
  "type": "object",
  "required": ["id", "sessionId", "skillId", "personaType", "score"],
  "properties": {
    "id": { "type": "string", "format": "uuid" },
    "sessionId": { "type": "string", "format": "uuid" },
    "skillId": { "type": "string", "format": "uuid" },
    "personaType": { "$ref": "#/InterviewerType" },
    "phase": { "$ref": "#/InterviewPhase" },
    "score": { "type": "number", "minimum": 0, "maximum": 5 },
    "evidence": { "type": "string" },
    "confidence": { "type": "number", "minimum": 0, "maximum": 1 },
    "tags": { "type": "array", "items": { "type": "string" } },
    "createdAt": { "type": "string", "format": "date-time" }
  }
}
```

- **SoftSkillDefinition, MergedSoftSkill, SoftSkillMergeResult**

```json
{
  "SoftSkillDefinition": {
    "type": "object",
    "required": ["id", "name"],
    "properties": {
      "id": { "type": "string", "format": "uuid" },
      "name": { "type": "string" },
      "description": { "type": "string" },
      "rubric": { "type": "object", "additionalProperties": true }
    }
  },
  "MergedSoftSkill": {
    "type": "object",
    "required": ["id", "mergeResultId", "softSkillId", "mergedScore"],
    "properties": {
      "id": { "type": "string", "format": "uuid" },
      "mergeResultId": { "type": "string", "format": "uuid" },
      "softSkillId": { "type": "string", "format": "uuid" },
      "mergedScore": { "type": "number", "minimum": 0, "maximum": 5 },
      "scoreByPersona": {
        "type": "object",
        "additionalProperties": { "type": "number" }
      },
      "disagreementLevel": { "type": "number", "minimum": 0, "maximum": 1 },
      "flags": { "type": "array", "items": { "type": "string" } }
    }
  },
  "SoftSkillMergeResult": {
    "type": "object",
    "required": ["id", "sessionId", "mergedSoftSkills"],
    "properties": {
      "id": { "type": "string", "format": "uuid" },
      "sessionId": { "type": "string", "format": "uuid" },
      "mergedSoftSkills": {
        "type": "array",
        "items": { "$ref": "#/MergedSoftSkill" }
      },
      "overallSummary": { "type": "string" },
      "radarData": { "type": "object", "additionalProperties": true },
      "disagreementSummary": { "type": "string" },
      "createdAt": { "type": "string", "format": "date-time" }
    }
  }
}
```

- **GrowthArea, GrowthPlan, GrowthStep, CoachInteraction**

```json
{
  "GrowthArea": {
    "type": "object",
    "required": ["id", "planId", "name"],
    "properties": {
      "id": { "type": "string", "format": "uuid" },
      "planId": { "type": "string", "format": "uuid" },
      "name": { "type": "string" },
      "description": { "type": "string" },
      "priority": { "type": "string", "enum": ["LOW", "MEDIUM", "HIGH"] }
    }
  },
  "GrowthStep": {
    "type": "object",
    "required": ["id", "areaId", "title"],
    "properties": {
      "id": { "type": "string", "format": "uuid" },
      "areaId": { "type": "string", "format": "uuid" },
      "title": { "type": "string" },
      "description": { "type": "string" },
      "status": {
        "type": "string",
        "enum": ["PLANNED", "IN_PROGRESS", "DONE"]
      },
      "dueDate": { "type": "string", "format": "date-time" }
    }
  },
  "GrowthPlan": {
    "type": "object",
    "required": ["id", "sessionId", "areas"],
    "properties": {
      "id": { "type": "string", "format": "uuid" },
      "sessionId": { "type": "string", "format": "uuid" },
      "summary": { "type": "string" },
      "areas": {
        "type": "array",
        "items": { "$ref": "#/GrowthArea" }
      },
      "createdAt": { "type": "string", "format": "date-time" }
    }
  },
  "CoachInteraction": {
    "type": "object",
    "required": ["id", "planId", "type", "content"],
    "properties": {
      "id": { "type": "string", "format": "uuid" },
      "planId": { "type": "string", "format": "uuid" },
      "type": {
        "type": "string",
        "enum": ["Nudge", "Reflection", "CheckIn"]
      },
      "content": { "type": "string" },
      "channel": { "type": "string", "enum": ["IN_APP", "EMAIL", "OTHER"] },
      "timestamp": { "type": "string", "format": "date-time" }
    }
  }
}
```

- **BehaviourSignal, InterviewOutcome**

```json
{
  "BehaviourSignal": {
    "type": "object",
    "required": ["id", "sessionId", "type", "value"],
    "properties": {
      "id": { "type": "string", "format": "uuid" },
      "sessionId": { "type": "string", "format": "uuid" },
      "messageId": { "type": "string", "format": "uuid" },
      "type": {
        "type": "string",
        "enum": ["LATENCY", "LENGTH", "SENTIMENT"]
      },
      "value": { "type": "number" },
      "metadata": { "type": "object", "additionalProperties": true },
      "timestamp": { "type": "string", "format": "date-time" }
    }
  },
  "InterviewOutcome": {
    "type": "object",
    "required": ["id", "sessionId", "label"],
    "properties": {
      "id": { "type": "string", "format": "uuid" },
      "sessionId": { "type": "string", "format": "uuid" },
      "label": {
        "type": "string",
        "enum": ["STRONG_HIRE", "HIRE", "NO_HIRE", "UNCLEAR"]
      },
      "reason": { "type": "string" },
      "decidedBy": { "type": "string" },
      "decidedAt": { "type": "string", "format": "date-time" }
    }
  }
}
```

- **Experiment, ExperimentVariant, ExperimentAssignment, BiasCalibrationSnapshot**

```json
{
  "Experiment": {
    "type": "object",
    "required": ["id", "key", "name"],
    "properties": {
      "id": { "type": "string", "format": "uuid" },
      "key": { "type": "string" },
      "name": { "type": "string" },
      "description": { "type": "string" },
      "status": {
        "type": "string",
        "enum": ["DRAFT", "RUNNING", "PAUSED", "COMPLETED"]
      },
      "targetMetric": { "type": "string" }
    }
  },
  "ExperimentVariant": {
    "type": "object",
    "required": ["id", "experimentId", "key"],
    "properties": {
      "id": { "type": "string", "format": "uuid" },
      "experimentId": { "type": "string", "format": "uuid" },
      "key": { "type": "string" },
      "description": { "type": "string" },
      "config": { "type": "object", "additionalProperties": true }
    }
  },
  "ExperimentAssignment": {
    "type": "object",
    "required": ["id", "experimentId", "variantId", "subjectType", "subjectId"],
    "properties": {
      "id": { "type": "string", "format": "uuid" },
      "experimentId": { "type": "string", "format": "uuid" },
      "variantId": { "type": "string", "format": "uuid" },
      "subjectType": {
        "type": "string",
        "enum": ["CANDIDATE", "SESSION"]
      },
      "subjectId": { "type": "string", "format": "uuid" },
      "assignedAt": { "type": "string", "format": "date-time" }
    }
  },
  "BiasCalibrationSnapshot": {
    "type": "object",
    "required": ["id", "createdAt"],
    "properties": {
      "id": { "type": "string", "format": "uuid" },
      "createdAt": { "type": "string", "format": "date-time" },
      "notes": { "type": "string" },
      "metrics": { "type": "object", "additionalProperties": true }
    }
  }
}
```


### 3. API Contracts (REST + WebSocket)

#### 3.1 Interview API (`/api/v1/interview/**`)

- **Create interview session**
  - **Method**: POST `/api/v1/interview/sessions`
  - **Request body**:

```json
{
  "candidateId": "uuid",
  "positionId": "uuid",
  "skillMatrixId": "uuid",
  "experimentKeys": ["softskill_prompt_v1"]
}
```

  - **Response 201**:

```json
{
  "id": "uuid",
  "currentPhase": "HR",
  "status": "ACTIVE"
}
```

- **Get session**
  - **Method**: GET `/api/v1/interview/sessions/{sessionId}`

- **List messages**
  - **Method**: GET `/api/v1/interview/sessions/{sessionId}/messages`
  - **Response**: array of `InterviewMessage`.

- **Post candidate message & trigger AI response**
  - **Method**: POST `/api/v1/interview/sessions/{sessionId}/messages`
  - **Request**:

```json
{
  "content": "Candidate free-text answer",
  "clientMessageId": "optional-uuid"
}
```

  - **Response 202**: acknowledges; AI persona responses stream over WebSocket.

- **Advance phase**
  - **Method**: POST `/api/v1/interview/sessions/{sessionId}/phase-transition`
  - **Request**:

```json
{
  "targetPhase": "TECHNICAL",
  "reason": "HR coverage complete"
}
```

- **WebSocket – Interview chat**
  - **Endpoint**: `ws://{host}/ws/interview/{sessionId}`
  - **Subprotocol**: JSON messages with envelope:

```json
{
  "type": "MESSAGE_CREATED",
  "payload": {
    "id": "uuid",
    "senderType": "CANDIDATE|PERSONA|SYSTEM",
    "personaType": "HR|TECH|TEAM_LEAD|COACH|SYSTEM",
    "phase": "HR",
    "content": "string",
    "timestamp": "2025-01-01T10:00:00Z"
  }
}
```

  - Other event types: `PHASE_CHANGED`, `SESSION_STATUS_CHANGED`, `TYPING`, `ERROR`.

#### 3.2 Skill Matrix API (`/api/v1/skill-matrix/**`)

- **Create/Update Skill Matrix**
  - POST `/api/v1/skill-matrix/matrices`
  - PUT `/api/v1/skill-matrix/matrices/{id}`

- **Get Skill Matrix by id**
  - GET `/api/v1/skill-matrix/matrices/{id}`

- **List Skill Matrices**
  - GET `/api/v1/skill-matrix/matrices`

- **Evaluate skills for a session (LLM-driven)**
  - POST `/api/v1/skill-matrix/sessions/{sessionId}/evaluate`
  - **Response**: array of `SkillEvaluation`.

#### 3.3 Soft Skill Merger API (`/api/v1/soft-skill-merger/**`)

- **Get merged soft skills for a session**
  - GET `/api/v1/soft-skill-merger/sessions/{sessionId}/merged`
  - **Response**: `SoftSkillMergeResult`.

- **Trigger merge (manual or re-run)**
  - POST `/api/v1/soft-skill-merger/sessions/{sessionId}/merge`
  - Side effect: emits `SoftSkillMerged` domain event.

- **Manage soft skill definitions**
  - CRUD on `/api/v1/soft-skill-merger/definitions`.

#### 3.4 Growth Coach API (`/api/v1/growth-coach/**`)

- **Generate growth plan from session**
  - POST `/api/v1/growth-coach/sessions/{sessionId}/plan`
  - **Request**:

```json
{
  "strategy": "RULE_BASED_V0_1"
}
```

  - **Response**: `GrowthPlan`.

- **Get / update growth plan**
  - GET `/api/v1/growth-coach/plans/{planId}`
  - PATCH `/api/v1/growth-coach/plans/{planId}`

- **Add coach interaction**
  - POST `/api/v1/growth-coach/plans/{planId}/interactions`

#### 3.5 Behaviour & Experimentation API (`/api/v1/behaviour/**`)

- **List behaviour signals for a session**
  - GET `/api/v1/behaviour/sessions/{sessionId}/signals`

- **Create experiment**
  - POST `/api/v1/behaviour/experiments`

- **Assign experiment**
  - POST `/api/v1/behaviour/experiments/{id}/assignments`

- **List experiments, variants, assignments**
  - GET `/api/v1/behaviour/experiments`
  - GET `/api/v1/behaviour/experiments/{id}`

- **Bias calibration snapshots**
  - GET `/api/v1/behaviour/bias-snapshots`


### 4. Event Model & Event Bus Plan

#### 4.1 Core domain events (Phase 0 – in-process)

- **InterviewSessionCreated**
  - **Emitted by**: Interview creation service.
  - **Payload**:

```json
{
  "sessionId": "uuid",
  "candidateId": "uuid",
  "positionId": "uuid",
  "createdAt": "date-time"
}
```

- **InterviewPhaseChanged**

```json
{
  "sessionId": "uuid",
  "oldPhase": "HR",
  "newPhase": "TECHNICAL",
  "changedAt": "date-time"
}
```

- **InterviewMessageAppended**

```json
{
  "sessionId": "uuid",
  "messageId": "uuid",
  "senderType": "CANDIDATE|PERSONA|SYSTEM",
  "personaType": "HR|TECH|TEAM_LEAD|COACH|SYSTEM",
  "phase": "HR",
  "timestamp": "date-time"
}
```

- **SkillEvaluated**

```json
{
  "sessionId": "uuid",
  "evaluationId": "uuid",
  "skillId": "uuid",
  "personaType": "TECH",
  "score": 4.0
}
```

- **SoftSkillMerged**

```json
{
  "sessionId": "uuid",
  "mergeResultId": "uuid",
  "createdAt": "date-time"
}
```

- **GrowthPlanCreated**

```json
{
  "sessionId": "uuid",
  "planId": "uuid",
  "createdAt": "date-time"
}
```

- **BehaviourSignalCaptured**

```json
{
  "signalId": "uuid",
  "sessionId": "uuid",
  "messageId": "uuid",
  "type": "LATENCY|LENGTH|SENTIMENT",
  "value": 1.23
}
```

- **ExperimentAssigned**

```json
{
  "experimentId": "uuid",
  "assignmentId": "uuid",
  "subjectType": "CANDIDATE|SESSION",
  "subjectId": "uuid"
}
```

- **InterviewOutcomeRecorded**

```json
{
  "sessionId": "uuid",
  "outcomeId": "uuid",
  "label": "HIRE"
}
```

#### 4.2 Event bus evolution

- **Phase 0 – Modular monolith**
  - Use Spring application events and a thin `DomainEventPublisher` abstraction.
  - Persist events into `event_log` table for later replay and analytics.

- **Phase 1 – External bus integration**
  - Introduce `EventBusAdapter` that maps selected internal events (e.g. `SoftSkillMerged`, `BehaviourSignalCaptured`) to Kafka/RabbitMQ topics.
  - Serialise payloads as JSON with explicit versioning.

- **Phase 2 – Service extraction**
  - Extract `softskillmerger` and `growthcoach` into separate Spring Boot services consuming events from `interview` and `skillmatrix` topics.
  - Panel orchestrator may become its own real-time service subscribed to `InterviewMessageAppended`.

- **Phase 3 – Behaviour & analytics fabric**
  - Dedicated behaviour/analytics service consuming `BehaviourSignalCaptured`, `ExperimentAssigned`, and `InterviewOutcomeRecorded`.
  - Build a derived talent graph and behaviour feature store for downstream ML.


### 5. Backend Development Waves

- **Wave 1 – Interview & Skill Matrix foundations**
  - Implement enums: `InterviewPhase`, `SessionStatus`, `InterviewerType`.
  - Implement entities and repositories: `Candidate`, `Position`, `InterviewSession`, `InterviewMessage`, `SkillMatrix`, `SkillCategory`, `Skill`, `SkillEvaluation`, `InterviewerPersona`.
  - Implement services: Interview flow (session lifecycle, phase routing, panel orchestrator v0), skill evaluation service (LLM-based scoring from chat logs).
  - Expose REST endpoints under `/api/v1/interview/**` and `/api/v1/skill-matrix/**`.
  - Implement WebSocket endpoint for interview chat.

- **Wave 2 – Soft Skill Merger module**
  - Implement entities: `SoftSkillDefinition`, `SoftSkillMergeResult`, `MergedSoftSkill`.
  - Implement services: `SoftSkillCollector`, `MergeAlgorithm`, `DisagreementDetector`, `SummaryGenerator`, `RadarDataBuilder`.
  - Expose REST endpoints under `/api/v1/soft-skill-merger/**`.
  - Emit `SoftSkillMerged` event.

- **Wave 3 – Growth Coach module**
  - Implement entities: `GrowthArea`, `GrowthPlan`, `GrowthStep`, `CoachInteraction`.
  - Implement `GrowthPlanBuilderService` (rule-based v0.1) and `CoachInteractionService`.
  - Expose REST endpoints under `/api/v1/growth-coach/**`.
  - Hook plan generation on `SoftSkillMerged` events.

- **Wave 4 – Behaviour & experiments**
  - Implement `BehaviourSignal` extraction from `InterviewMessage`.
  - Implement experimentation entities (`Experiment`, `ExperimentVariant`, `ExperimentAssignment`) and minimal APIs.
  - Emit and consume `BehaviourSignalCaptured`, `ExperimentAssigned`, and `InterviewOutcomeRecorded`.


### 6. Frontend Development Waves

- **Wave 1 – Interview session foundations**
  - Implement `interview-session` module: chat UI, phase indicators, persona badges.
  - Implement shared models and services for interview and skill-matrix APIs.
  - Implement WebSocket client for real-time interview messages.

- **Wave 2 – Soft Skill Merger UI**
  - Implement `soft-skill-merger` module: radar chart, disagreement matrix, merged summary views.
  - Implement `soft-skill-merger.service.ts` and shared models.

- **Wave 3 – Growth Coach UI**
  - Implement `growth-coach` module: plan board, step details, timeline of `CoachInteraction`.
  - Implement `growth-coach.service.ts` and shared models.

- **Wave 4 – Multi-interviewer panel & Behaviour Lab**
  - Implement `multi-interviewer-panel` dashboard for persona views, conflict surfacing, overrides.
  - Implement `behaviour-lab` admin UI for signals, experiments, bias snapshots.


### 7. Non-functional requirements & constraints

- **Security & privacy**
  - Role-based access (candidate vs interviewer vs admin).
  - PII minimisation in logs and events.
- **Observability**
  - Structured logging for all domain events and LLM calls.
  - Metrics for interview throughput, LLM latency, and behaviour signal volume.
- **Extensibility**
  - All AI interactions behind interfaces to support model changes.
  - Event contracts versioned to support gradual evolution toward microservices.














