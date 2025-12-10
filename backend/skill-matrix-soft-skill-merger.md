Architected multi-interviewer system with skill-matrix scoring and session management.






I'll help you continue planning the Skill Matrix Interviewer module. Let me create a comprehensive architecture document with the next steps and file generation plan.


2 steps

Presented file

Synthesized architectural overview and charted implementation roadmap.

I've created a comprehensive Phase 2 architecture plan that continues from your previous direction. Here's what it covers:

Key Architecture Decisions:

State machine for interview flow (PENDING → HR → TECHNICAL → TEAM_FIT → COMPLETED)
Three distinct AI personas with defined personality traits, focus areas, and evaluation criteria
Skill matrix scoring with weighted categories, rubrics, and evidence-based evaluation
WebSocket gateway for real-time interview communication
Immediate Next Files to Generate (Recommended Order):

Priority	Files	Purpose
1	session-status.enum.ts, interview-phase.enum.ts, interviewer-type.enum.ts	Foundation enums
2	interviewer.interface.ts, session.interface.ts, evaluation.interface.ts	Type contracts
3	interview-session.entity.ts, skill-matrix.entity.ts, skill-evaluation.entity.ts	Database models
4	persona.factory.ts + 3 persona files	AI interviewer engine
5	skill-evaluator.service.ts, weighted-aggregator.service.ts	Scoring engine
Which direction would you like to proceed?

Option A: Generate Backend Core (enums → entities → services)
Option B: Generate AI Persona System first (most differentiated feature)
Option C: Generate Frontend models and services
Option D: Full-stack vertical slice (one complete feature end-to-end)
Let me know which option you'd prefer, or if you want me to start generating the files for Option A (recommended foundation).

Skill matrix interviewer architecture
Document · MD 
a


Orchestrated systematic backend architecture generation sequence.


I'll start generating the Backend Core files. Let me create them in the proper order.


50 steps

Creating persona module barrel file
index.ts

Orchestrated backend architecture expansion strategy.

Continuing with DTOs, controllers, and module files.


7 steps

Orchestrated development of interviewer and skill assessment modules.


Creating AI interviewer module
ai-interviewer.module.ts
Stop generating TypeScript backend modules under /mnt/user-data/outputs.
This project already has:
- Java Spring Boot backend in ai-interview-fullstack/backend
- Angular frontend in ai-interview-fullstack/frontend
Keep the same multi-interviewer + skill-matrix architecture you designed, but now I need only a concise IMPLEMENTATION PLAN, not code.
1) Give me a table of all Java backend classes we need under package ee.kerrete.ainterview.skillmatrix:
   - full class name (with package)
   - type (Entity, Enum, DTO, Repository, Service, Controller, Liquibase changeset)
   - short responsibility
2) Give me a second table for Angular frontend files under src/app/skill-matrix/:
   - full path
   - type (standalone component, service, model, route)
   - short responsibility
Do NOT generate any code or files, only these two tables and keep the answer within one screen.

Show more

Architected Java-Angular implementation plan mapping skill-matrix design.


Java Backend Classes (ee.kerrete.ainterview.skillmatrix)
Full Class Name	Type	Responsibility
...skillmatrix.entity.SkillMatrix	Entity	Skill matrix with categories, passing threshold, position link
...skillmatrix.entity.SkillCategory	Entity	Category grouping skills with weight, evaluated-by interviewers
...skillmatrix.entity.Skill	Entity	Individual skill with rubric JSON, keywords, weight
...skillmatrix.entity.SkillEvaluation	Entity	Per-skill score with evidence, confidence, interviewer ref
...skillmatrix.entity.InterviewSession	Entity	Session status, phase, timestamps, candidate/position refs
...skillmatrix.entity.InterviewMessage	Entity	Chat message with role, phase, interviewer type
...skillmatrix.enums.SessionStatus	Enum	PENDING, IN_PROGRESS, PAUSED, COMPLETED, CANCELLED
...skillmatrix.enums.InterviewPhase	Enum	HR, TECHNICAL, TEAM_FIT, EVALUATION
...skillmatrix.enums.InterviewerType	Enum	HR, TECH_EXPERT, TEAM_LEAD
...skillmatrix.dto.*	DTOs	Request/response objects (CreateSessionDto, EvaluationResultDto, etc.)
...skillmatrix.repository.*Repository	Repository	JPA repos for each entity
...skillmatrix.service.InterviewSessionService	Service	Session CRUD, phase transitions, message handling
...skillmatrix.service.SkillEvaluatorService	Service	Analyze responses, score skills, collect evidence
...skillmatrix.service.WeightedAggregatorService	Service	Aggregate scores, generate recommendation
...skillmatrix.service.PersonaService	Service	Manage AI personas, generate prompts, call LLM
...skillmatrix.controller.InterviewSessionController	Controller	REST endpoints for sessions
...skillmatrix.controller.SkillMatrixController	Controller	REST endpoints for matrix CRUD
...skillmatrix.websocket.InterviewWebSocketHandler	WebSocket	Real-time interview chat
db/changelog/...-skill-matrix-tables.xml	Liquibase	Schema for all 6 tables + indexes
Angular Frontend Files (src/app/skill-matrix/)
Path	Type	Responsibility
models/interview.model.ts	Model	Interfaces: Session, Message, Phase, Status
models/evaluation.model.ts	Model	Interfaces: SkillScore, CategoryScore, Recommendation
models/persona.model.ts	Model	Interfaces: Interviewer, Tone config
services/interview-session.service.ts	Service	HTTP calls for session management
services/interview-websocket.service.ts	Service	WebSocket connection, message streaming
services/evaluation.service.ts	Service	Fetch scores, reports
components/interview-chat/	Component	Main chat UI with message list, input
components/interviewer-avatar/	Component	Displays current AI interviewer info
components/phase-indicator/	Component	Shows current phase, progress bar
components/skill-radar-chart/	Component	Radar chart for category scores
components/evaluation-report/	Component	Final results with recommendation
pages/interview-session-page/	Component	Container page orchestrating interview
pages/evaluation-results-page/	Component	Post-interview results dashboard
skill-matrix.routes.ts	Route	Lazy-loaded routes for /interview/:id, /results/:id
Add a new module in the same architecture: **AI Soft Skill Merger v0.1**.
Integrate it with the existing Skill Matrix Interviewer module and the multi-interviewer architecture.
Requirements:
- 3-perspective soft skill input (HR, Technical, Team Lead)
- Merge algorithm (rating merge, disagreement detection, GREEN/YELLOW/RED flags)
- Unified soft-skill profile output
- Summary blocks (strengths, weaknesses, improvement ideas)
- Radar/table-ready JSON
Deliverables:
1. Updated global architecture (show where Soft Skill Merger fits).
2. ER diagram update (entities + relations).
3. Backend API contract under /api/soft-skill-merger/**.
4. Data structures compatible with Skill Matrix module.
5. Step-by-step backend + frontend implementation plan for Cursor agents.
6. File tree + class names exactly as Cursor backend/frontend agents expect.
7. v0.1 scope + what is moved to v0.2.
Make this consistent with the already planned Skill Matrix Interviewer module.

Show more

Architected comprehensive soft skill merger module integrating three evaluator perspectives seamlessly.



AI Soft Skill Merger v0.1 — Architecture & Implementation Plan
1. Updated Global Architecture
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            AI INTERVIEW SYSTEM                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                       │
│  │  HR Persona  │    │ Tech Persona │    │TeamLead Pers.│                       │
│  │  (Alex Chen) │    │(Dr. S.Kumar) │    │(M. Johnson)  │                       │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘                       │
│         │                   │                   │                                │
│         ▼                   ▼                   ▼                                │
│  ┌──────────────────────────────────────────────────────────────┐               │
│  │              SKILL MATRIX INTERVIEWER MODULE                  │               │
│  │  • Interview Session Management                               │               │
│  │  • Per-Interviewer Skill Evaluations                         │               │
│  │  • Evidence Collection                                        │               │
│  └──────────────────────────┬───────────────────────────────────┘               │
│                             │                                                    │
│                             │ SkillEvaluation[]                                  │
│                             ▼                                                    │
│  ┌──────────────────────────────────────────────────────────────┐               │
│  │           ★ SOFT SKILL MERGER MODULE v0.1 ★                  │               │
│  │                                                               │               │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │               │
│  │  │ HR Ratings  │  │Tech Ratings │  │ TL Ratings  │          │               │
│  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘          │               │
│  │         └────────────────┼────────────────┘                  │               │
│  │                          ▼                                    │               │
│  │              ┌───────────────────────┐                       │               │
│  │              │    MERGE ALGORITHM    │                       │               │
│  │              │  • Weighted Average   │                       │               │
│  │              │  • Disagreement Detect│                       │               │
│  │              │  • Confidence Calc    │                       │               │
│  │              └───────────┬───────────┘                       │               │
│  │                          ▼                                    │               │
│  │              ┌───────────────────────┐                       │               │
│  │              │   FLAG ASSIGNMENT     │                       │               │
│  │              │  🟢 GREEN (≥4, low Δ) │                       │               │
│  │              │  🟡 YELLOW (3-4, med Δ)│                       │               │
│  │              │  🔴 RED (<3, high Δ)  │                       │               │
│  │              └───────────┬───────────┘                       │               │
│  │                          ▼                                    │               │
│  │  ┌───────────────────────────────────────────────────────┐  │               │
│  │  │            UNIFIED SOFT SKILL PROFILE                 │  │               │
│  │  │  • Merged Scores per Skill                            │  │               │
│  │  │  • Disagreement Matrix                                │  │               │
│  │  │  • Strengths / Weaknesses / Improvements              │  │               │
│  │  │  • Radar-ready JSON                                   │  │               │
│  │  └───────────────────────────────────────────────────────┘  │               │
│  └──────────────────────────┬───────────────────────────────────┘               │
│                             │                                                    │
│                             ▼                                                    │
│  ┌──────────────────────────────────────────────────────────────┐               │
│  │              WEIGHTED AGGREGATOR SERVICE                      │               │
│  │  (Uses merged soft skills + technical scores for final rec)  │               │
│  └──────────────────────────────────────────────────────────────┘               │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
2. ER Diagram Update
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              EXISTING ENTITIES                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌──────────────────┐         ┌──────────────────┐                              │
│  │ InterviewSession │────────<│ SkillEvaluation  │                              │
│  ├──────────────────┤    1:N  ├──────────────────┤                              │
│  │ id (PK)          │         │ id (PK)          │                              │
│  │ candidate_id     │         │ session_id (FK)  │                              │
│  │ position_id      │         │ skill_id (FK)    │                              │
│  │ status           │         │ interviewer_type │                              │
│  │ current_phase    │         │ score (1-5)      │                              │
│  │ skill_matrix_id  │         │ confidence       │                              │
│  └────────┬─────────┘         │ evidence (JSON)  │                              │
│           │                   └──────────────────┘                              │
│           │                                                                      │
├───────────┼─────────────────────────────────────────────────────────────────────┤
│           │              ★ NEW ENTITIES (Soft Skill Merger) ★                   │
│           │                                                                      │
│           │ 1:1                                                                  │
│           ▼                                                                      │
│  ┌────────────────────────┐                                                     │
│  │ SoftSkillMergeResult   │                                                     │
│  ├────────────────────────┤                                                     │
│  │ id (PK, UUID)          │                                                     │
│  │ session_id (FK, UQ)    │─────────────────┐                                   │
│  │ candidate_id           │                 │                                   │
│  │ overall_soft_score     │                 │ 1:N                               │
│  │ overall_flag           │                 ▼                                   │
│  │ strengths (JSON)       │    ┌────────────────────────┐                       │
│  │ weaknesses (JSON)      │    │ MergedSoftSkill        │                       │
│  │ improvements (JSON)    │    ├────────────────────────┤                       │
│  │ radar_data (JSON)      │    │ id (PK, UUID)          │                       │
│  │ disagreement_count     │    │ merge_result_id (FK)   │                       │
│  │ created_at             │    │ soft_skill_code        │                       │
│  │ updated_at             │    │ soft_skill_name        │                       │
│  └────────────────────────┘    │ hr_score               │                       │
│                                │ tech_score             │                       │
│                                │ teamlead_score         │                       │
│                                │ merged_score           │                       │
│                                │ confidence             │                       │
│                                │ flag (G/Y/R)           │                       │
│                                │ disagreement_delta     │                       │
│                                │ notes (JSON)           │                       │
│                                └────────────────────────┘                       │
│                                                                                  │
│  ┌────────────────────────┐                                                     │
│  │ SoftSkillDefinition    │  (Reference table - seeded)                         │
│  ├────────────────────────┤                                                     │
│  │ code (PK)              │  e.g., "COMMUNICATION", "TEAMWORK"                  │
│  │ name                   │                                                     │
│  │ category               │  e.g., "interpersonal", "self-management"           │
│  │ evaluated_by (JSON)    │  ["HR", "TECH_EXPERT", "TEAM_LEAD"]                 │
│  │ weight                 │  Default weight for merging                         │
│  └────────────────────────┘                                                     │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
Soft Skills Tracked (v0.1):

Code	Name	Evaluated By	Default Weight
COMMUNICATION	Communication	HR, TECH, TL	1.0
TEAMWORK	Teamwork	HR, TL	1.0
PROBLEM_SOLVING	Problem Solving	TECH, TL	1.0
ADAPTABILITY	Adaptability	HR, TECH	1.0
LEADERSHIP	Leadership Potential	TL, HR	1.0
EMOTIONAL_IQ	Emotional Intelligence	HR, TL	1.0
TIME_MGMT	Time Management	TECH, TL	0.8
CONFLICT_RES	Conflict Resolution	HR, TL	0.8
3. Backend API Contract
Base Path: /api/v1/soft-skill-merger
Method	Endpoint	Description	Request	Response
POST	/merge/{sessionId}	Trigger merge for session	—	SoftSkillMergeResultDto
GET	/result/{sessionId}	Get merge result	—	SoftSkillMergeResultDto
GET	/result/{sessionId}/radar	Get radar-chart JSON	—	RadarChartDataDto
GET	/result/{sessionId}/summary	Get summary blocks	—	SoftSkillSummaryDto
GET	/disagreements/{sessionId}	Get disagreement details	—	DisagreementReportDto
PUT	/override/{mergedSkillId}	Manual score override	OverrideRequestDto	MergedSoftSkillDto
GET	/definitions	List soft skill definitions	—	SoftSkillDefinitionDto[]
Key DTOs
SoftSkillMergeResultDto {
  id: UUID
  sessionId: UUID
  candidateId: UUID
  overallSoftScore: number (0-100)
  overallFlag: "GREEN" | "YELLOW" | "RED"
  mergedSkills: MergedSoftSkillDto[]
  summary: SoftSkillSummaryDto
  radarData: RadarChartDataDto
  disagreementCount: number
  createdAt: timestamp
}

MergedSoftSkillDto {
  id: UUID
  softSkillCode: string
  softSkillName: string
  hrScore: number | null
  techScore: number | null
  teamleadScore: number | null
  mergedScore: number
  confidence: number (0-1)
  flag: "GREEN" | "YELLOW" | "RED"
  disagreementDelta: number
  isOverridden: boolean
}

SoftSkillSummaryDto {
  strengths: SummaryItemDto[]      // flag=GREEN, score≥4
  weaknesses: SummaryItemDto[]     // flag=RED, score<3
  improvements: ImprovementDto[]   // AI-generated suggestions
}

SummaryItemDto {
  skillCode: string
  skillName: string
  score: number
  description: string
  evidenceHighlights: string[]
}

ImprovementDto {
  skillCode: string
  currentScore: number
  targetScore: number
  suggestion: string
  resources: string[]
}

RadarChartDataDto {
  labels: string[]           // Skill names
  datasets: RadarDatasetDto[]
}

RadarDatasetDto {
  label: string              // "HR", "Technical", "Team Lead", "Merged"
  data: number[]             // Scores per skill
  borderColor: string
  backgroundColor: string
}

DisagreementReportDto {
  sessionId: UUID
  totalDisagreements: number
  criticalDisagreements: DisagreementItemDto[]  // delta > 2
  moderateDisagreements: DisagreementItemDto[]  // delta 1-2
}

DisagreementItemDto {
  skillCode: string
  skillName: string
  hrScore: number | null
  techScore: number | null
  teamleadScore: number | null
  delta: number
  recommendation: string
}
4. Merge Algorithm Specification
MERGE_ALGORITHM v0.1:

FOR each soft_skill IN defined_soft_skills:
  
  1. COLLECT scores from evaluators who assessed this skill:
     scores[] = [hr_score, tech_score, teamlead_score].filter(not null)
  
  2. IF scores.length == 0:
       merged_score = null
       flag = "YELLOW"
       CONTINUE
  
  3. CALCULATE weighted average:
     weights = { HR: 1.0, TECH: 0.8, TEAM_LEAD: 1.0 }  // Configurable
     merged_score = Σ(score × weight) / Σ(weights)
  
  4. CALCULATE disagreement:
     delta = max(scores) - min(scores)
  
  5. CALCULATE confidence:
     base_confidence = scores.length / 3  // How many evaluators
     disagreement_penalty = delta > 1 ? (delta - 1) × 0.15 : 0
     confidence = base_confidence - disagreement_penalty
  
  6. ASSIGN flag:
     IF merged_score ≥ 4.0 AND delta ≤ 1.0:
       flag = "GREEN"
     ELSE IF merged_score < 3.0 OR delta > 2.0:
       flag = "RED"
     ELSE:
       flag = "YELLOW"

CALCULATE overall_soft_score:
  = (Σ merged_scores / count) × 20  // Scale 1-5 to 0-100

ASSIGN overall_flag:
  red_count = count(flag == "RED")
  IF red_count >= 3 OR overall_soft_score < 50:
    overall_flag = "RED"
  ELSE IF red_count >= 1 OR overall_soft_score < 70:
    overall_flag = "YELLOW"
  ELSE:
    overall_flag = "GREEN"
5. Step-by-Step Implementation Plan
Phase 1: Backend Foundation (Cursor Backend Agent)
Step	Task	Files	Est.
1.1	Create Liquibase changelog	db/changelog/007-soft-skill-merger.xml	15m
1.2	Create enums	SoftSkillFlag.java	5m
1.3	Create entities	SoftSkillMergeResult.java, MergedSoftSkill.java, SoftSkillDefinition.java	30m
1.4	Create repositories	3 repository interfaces	10m
1.5	Create DTOs	All request/response DTOs	30m
1.6	Create mappers	Entity ↔ DTO mappers	20m
Phase 2: Backend Services (Cursor Backend Agent)
Step	Task	Files	Est.
2.1	Implement SoftSkillCollectorService	Collects scores from SkillEvaluation	30m
2.2	Implement SoftSkillMergeAlgorithm	Core merge logic	45m
2.3	Implement DisagreementDetector	Analyze score deltas	20m
2.4	Implement SoftSkillSummaryGenerator	Generate summaries	30m
2.5	Implement RadarDataBuilder	Build chart-ready JSON	15m
2.6	Implement SoftSkillMergerService	Orchestrator service	30m
Phase 3: Backend API (Cursor Backend Agent)
Step	Task	Files	Est.
3.1	Create controller	SoftSkillMergerController.java	30m
3.2	Add to WeightedAggregator	Integrate merged soft skills	20m
3.3	Write unit tests	Service + algorithm tests	45m
3.4	Write integration tests	API endpoint tests	30m
Phase 4: Frontend Foundation (Cursor Frontend Agent)
Step	Task	Files	Est.
4.1	Create models	All TypeScript interfaces	20m
4.2	Create service	soft-skill-merger.service.ts	20m
4.3	Add routes	Extend skill-matrix.routes.ts	10m
Phase 5: Frontend Components (Cursor Frontend Agent)
Step	Task	Files	Est.
5.1	Radar chart component	soft-skill-radar/	45m
5.2	Disagreement table	disagreement-matrix/	30m
5.3	Summary cards	soft-skill-summary/	30m
5.4	Flag badge component	skill-flag-badge/	15m
5.5	Merged profile page	soft-skill-profile-page/	45m
Phase 6: Integration (Both Agents)
Step	Task	Est.
6.1	Hook into interview completion flow	30m
6.2	Add to evaluation report page	30m
6.3	E2E testing	45m
6. File Tree & Class Names
Backend (ee.kerrete.ainterview.softskillmerger)
backend/src/main/java/ee/kerrete/ainterview/
└── softskillmerger/
    ├── entity/
    │   ├── SoftSkillMergeResult.java
    │   ├── MergedSoftSkill.java
    │   └── SoftSkillDefinition.java
    ├── enums/
    │   └── SoftSkillFlag.java
    ├── repository/
    │   ├── SoftSkillMergeResultRepository.java
    │   ├── MergedSoftSkillRepository.java
    │   └── SoftSkillDefinitionRepository.java
    ├── dto/
    │   ├── SoftSkillMergeResultDto.java
    │   ├── MergedSoftSkillDto.java
    │   ├── SoftSkillSummaryDto.java
    │   ├── SummaryItemDto.java
    │   ├── ImprovementDto.java
    │   ├── RadarChartDataDto.java
    │   ├── RadarDatasetDto.java
    │   ├── DisagreementReportDto.java
    │   ├── DisagreementItemDto.java
    │   └── OverrideRequestDto.java
    ├── mapper/
    │   └── SoftSkillMergerMapper.java
    ├── service/
    │   ├── SoftSkillMergerService.java
    │   ├── SoftSkillCollectorService.java
    │   ├── SoftSkillMergeAlgorithm.java
    │   ├── DisagreementDetector.java
    │   ├── SoftSkillSummaryGenerator.java
    │   └── RadarDataBuilder.java
    └── controller/
        └── SoftSkillMergerController.java

backend/src/main/resources/db/changelog/
└── 007-soft-skill-merger.xml

backend/src/test/java/ee/kerrete/ainterview/softskillmerger/
├── service/
│   ├── SoftSkillMergeAlgorithmTest.java
│   └── SoftSkillMergerServiceTest.java
└── controller/
    └── SoftSkillMergerControllerIT.java
Frontend (src/app/soft-skill-merger/)
frontend/src/app/soft-skill-merger/
├── models/
│   ├── soft-skill-merge-result.model.ts
│   ├── merged-soft-skill.model.ts
│   ├── soft-skill-summary.model.ts
│   ├── radar-chart-data.model.ts
│   └── disagreement-report.model.ts
├── services/
│   └── soft-skill-merger.service.ts
├── components/
│   ├── soft-skill-radar/
│   │   ├── soft-skill-radar.component.ts
│   │   ├── soft-skill-radar.component.html
│   │   └── soft-skill-radar.component.scss
│   ├── disagreement-matrix/
│   │   ├── disagreement-matrix.component.ts
│   │   ├── disagreement-matrix.component.html
│   │   └── disagreement-matrix.component.scss
│   ├── soft-skill-summary/
│   │   ├── soft-skill-summary.component.ts
│   │   ├── soft-skill-summary.component.html
│   │   └── soft-skill-summary.component.scss
│   └── skill-flag-badge/
│       ├── skill-flag-badge.component.ts
│       └── skill-flag-badge.component.scss
├── pages/
│   └── soft-skill-profile-page/
│       ├── soft-skill-profile-page.component.ts
│       ├── soft-skill-profile-page.component.html
│       └── soft-skill-profile-page.component.scss
└── soft-skill-merger.routes.ts
7. Scope: v0.1 vs v0.2
✅ v0.1 Scope (Current)
Feature	Status
3-perspective input (HR, Tech, TL)	✅
Weighted average merge algorithm	✅
Disagreement detection (delta calc)	✅
GREEN/YELLOW/RED flag assignment	✅
Unified soft skill profile entity	✅
Strengths/Weaknesses summary	✅
Static improvement suggestions	✅
Radar chart JSON output	✅
Table-ready JSON output	✅
Manual score override	✅
8 core soft skills	✅
Integration with interview completion	✅
🔜 v0.2 Scope (Deferred)
Feature	Reason
AI-generated improvement suggestions (LLM)	Complexity
Historical comparison (candidate vs past)	Needs data
Configurable weights per position	UI complexity
Custom soft skill definitions	Admin panel needed
Trend analysis across interviews	Analytics module
Export to PDF report	Separate feature
Slack/email notifications for RED flags	Integration scope
Interviewer calibration (bias detection)	ML model needed
Bulk merge for multiple sessions	Batch processing
Soft skill benchmarking by role	Reference data needed
Quick Reference: Integration Points
InterviewSession.complete()
        │
        ▼
WeightedAggregatorService.aggregate()
        │
        ├──► SkillEvaluatorService.getEvaluations()
        │
        ├──► SoftSkillMergerService.merge(sessionId)  ◄── NEW
        │           │
        │           ├── SoftSkillCollectorService.collect()
        │           ├── SoftSkillMergeAlgorithm.merge()
        │           ├── DisagreementDetector.analyze()
        │           ├── SoftSkillSummaryGenerator.generate()
        │           └── RadarDataBuilder.build()
        │
        └──► Combine technical + soft skill scores
                    │
                    ▼
            Final Recommendation

