# Adaptive Multi-Role Interview Engine — Design Specification

## Table of Contents
1. [Role Profiles](#1-role-profiles)
2. [Question Engine Logic](#2-question-engine-logic)
3. [JSON API Schema](#3-json-api-schema)
4. [Prompt Templates](#4-prompt-templates)
5. [Edge Cases](#5-edge-cases)

---

## 1. Role Profiles

### 1.1 HR Interviewer

**Key Dimensions:**
| Dimension | Weight | Description |
|-----------|--------|-------------|
| Culture Fit | 25% | Alignment with company values, team dynamics |
| Motivation | 20% | Career goals, reasons for applying, drive |
| Communication | 20% | Clarity, structure, listening skills |
| Stakeholder Management | 15% | Managing expectations, influencing without authority |
| Adaptability | 10% | Handling change, ambiguity, feedback |
| Self-Awareness | 10% | Understanding strengths/weaknesses, growth mindset |

**Question Styles:**
- Behavioral (STAR format): "Tell me about a time when..."
- Situational: "How would you handle..."
- Values-based: "What matters most to you in a workplace?"
- Motivation probes: "Why this company? Why now?"

**Scrutiny Level:** Medium — empathetic but probing. Will ask clarifying questions if answers feel rehearsed or vague. Looks for authenticity.

**Follow-up Triggers:**
- Vague claims → "Can you give me a specific example?"
- Missing stakeholders → "Who else was involved? How did they react?"
- No outcome mentioned → "What was the result? What did you learn?"

---

### 1.2 Tech Interviewer

**Key Dimensions:**
| Dimension | Weight | Description |
|-----------|--------|-------------|
| System Design | 25% | Architecture decisions, scalability, trade-offs |
| Problem Solving | 25% | Breaking down problems, edge cases, debugging |
| Code Ownership | 15% | Code quality, testing, technical debt awareness |
| Technical Depth | 15% | Mastery of tools, languages, frameworks |
| Trade-off Reasoning | 10% | Justifying decisions, understanding constraints |
| Learning Velocity | 10% | Picking up new tech, staying current |

**Question Styles:**
- Design challenges: "How would you design..."
- Debugging scenarios: "The system is slow. Walk me through..."
- Trade-off questions: "Why X over Y?"
- Code ownership: "How do you ensure code quality in your team?"
- Depth probes: "Explain how X works under the hood"

**Scrutiny Level:** High — expects precision. Will challenge assumptions, ask about edge cases, and probe for depth. Comfortable with silence while candidate thinks.

**Follow-up Triggers:**
- Hand-wavy architecture → "What happens when this component fails?"
- No scale consideration → "How does this behave at 10x traffic?"
- Missing trade-offs → "What did you sacrifice for that choice?"
- Buzzword usage → "Can you explain what you mean by [term]?"

---

### 1.3 Team Lead Interviewer

**Key Dimensions:**
| Dimension | Weight | Description |
|-----------|--------|-------------|
| Leadership | 25% | Guiding teams, setting direction, accountability |
| Collaboration | 20% | Cross-functional work, building relationships |
| Conflict Resolution | 20% | Handling disagreements, difficult conversations |
| Mentoring | 15% | Developing others, giving feedback |
| Delivery | 15% | Meeting deadlines, managing scope, unblocking |
| Decision Making | 5% | Making calls under uncertainty |

**Question Styles:**
- Leadership scenarios: "Tell me about a time you led..."
- Conflict probes: "Describe a disagreement with a colleague..."
- Mentoring examples: "How have you helped someone grow?"
- Delivery challenges: "A project is at risk. What do you do?"
- Collaboration stories: "How do you work with [other function]?"

**Scrutiny Level:** Medium-High — looks for concrete examples of impact on people and outcomes. Skeptical of "I" statements without team context.

**Follow-up Triggers:**
- All credit taken → "What role did others play?"
- No conflict mentioned → "Was everyone always aligned? Really?"
- Vague leadership → "What specifically did you do as the lead?"
- Missing outcome → "What changed as a result of your actions?"

---

### 1.4 Role Combinations

#### HR + Tech
**Interleaving Strategy:** Alternate every 2-3 questions. Start with HR (warm-up, motivation) then shift to Tech. Return to HR for culture/collaboration questions after technical deep-dive.

**Overlap Handling:**
- Communication appears in both → HR owns "soft" communication; Tech owns "technical communication" (explaining complex topics, documentation)
- Collaboration appears in both → HR owns "interpersonal"; Tech owns "code review, pair programming, technical mentorship"

**Transition Signals:**
- After strong HR answer → "Great, let's shift gears to something more technical..."
- After technical deep-dive → "Stepping back from the technical side..."

#### Tech + Team Lead
**Interleaving Strategy:** Blend naturally since both care about delivery and technical decisions. Tech focuses on "how"; Team Lead focuses on "who and why."

**Overlap Handling:**
- Technical decisions → Tech probes architecture; Team Lead probes stakeholder buy-in
- Delivery → Tech probes technical blockers; Team Lead probes people/process blockers

#### HR + Team Lead
**Interleaving Strategy:** High overlap — can often combine into single questions. Focus on behavioral/situational with leadership lens.

**Overlap Handling:**
- Conflict → HR probes emotional intelligence; Team Lead probes resolution process
- Culture → HR probes personal values; Team Lead probes team culture building

#### HR + Tech + Team Lead (Full Panel)
**Interleaving Strategy:** Round-robin with adaptive weighting. Start HR (2 questions), then Tech (3 questions), then Team Lead (2 questions). Adjust based on seniority:
- Junior: 40% Tech, 35% HR, 25% Team Lead
- Mid: 35% Tech, 30% HR, 35% Team Lead
- Senior: 30% Tech, 25% HR, 45% Team Lead

**Deduplication Rules:**
1. Track covered dimensions in session state
2. If a dimension was strongly covered by one role, other roles skip or abbreviate
3. Use `coveredDimensions` array in analysis to prevent repetition

---

## 2. Question Engine Logic

### 2.1 Generating the First 3 Questions

```
ALGORITHM: GenerateInitialQuestions

INPUT:
  - company: string
  - roleTitle: string
  - seniority: Junior | Mid | Senior
  - interviewerRoles: Role[]

OUTPUT:
  - questions: Question[3]

STEPS:

1. DETERMINE QUESTION DISTRIBUTION
   Based on interviewerRoles and seniority, calculate how many questions per role:
   
   IF single role:
     All 3 questions from that role
   
   IF two roles:
     Primary role (first in array): 2 questions
     Secondary role: 1 question
   
   IF three roles:
     HR: 1, Tech: 1, Team Lead: 1

2. SELECT OPENING DIMENSIONS
   For each role, pick the "opener" dimension:
   - HR → Motivation (warm, easy start)
   - Tech → Problem Solving or System Design (depending on seniority)
   - Team Lead → Leadership or Collaboration
   
   Seniority adjustment:
   - Junior: Start with foundational questions
   - Mid: Start with scenario-based questions
   - Senior: Start with strategic/impact questions

3. GENERATE QUESTIONS
   For each allocated slot:
   a. Select dimension from role's dimension list (not yet used)
   b. Select question style appropriate to dimension
   c. Customize for company + roleTitle:
      - Inject company name where relevant
      - Reference industry-specific scenarios
      - Adjust complexity for seniority
   d. Generate model answer (what a strong response looks like)
   e. Generate candidate hint (non-revealing nudge)

4. ORDER QUESTIONS
   - Always start with softest question (typically HR/motivation)
   - Build intensity gradually
   - End first batch on an open-ended note to gather signal

5. RETURN questions with:
   - Unique IDs (UUID)
   - Role attribution
   - Display tags
   - Model answers
   - Hints
```

### 2.2 Answer Analysis Model

```
ALGORITHM: AnalyzeAnswer

INPUT:
  - question: Question
  - userAnswer: string
  - role: Role
  - seniority: Seniority
  - history: HistoryEntry[]

OUTPUT:
  - analysis: AnswerAnalysis

STEPS:

1. EXTRACT SIGNALS
   Parse userAnswer for presence/absence of expected elements:
   
   Signal Categories:
   ┌─────────────────┬────────────────────────────────────────┐
   │ Category        │ Signals to Detect                      │
   ├─────────────────┼────────────────────────────────────────┤
   │ Structure       │ STAR format, clear narrative, logical  │
   │ Specificity     │ Names, dates, metrics, concrete details│
   │ Ownership       │ "I did X" vs "We did X" balance        │
   │ Outcome         │ Results mentioned, lessons learned     │
   │ Relevance       │ Answer matches question asked          │
   │ Depth           │ Technical accuracy, nuance, trade-offs │
   │ Self-Awareness  │ Acknowledges mistakes, growth          │
   │ Red Flags       │ Blame-shifting, vagueness, arrogance   │
   └─────────────────┴────────────────────────────────────────┘

2. SCORE SIGNALS
   For each signal category, assign:
   - Score: -1 (concerning), 0 (neutral), 1 (present), 2 (strong)
   - Evidence: Quote or observation supporting score
   
   Aggregate into:
   - strengths[]: signals with score >= 1, with evidence
   - risks[]: signals with score <= 0, with evidence
   - missingSignals[]: expected signals not detected

3. DETERMINE ANSWER QUALITY TIER
   Based on aggregate scores:
   
   TIER 1 - STRONG (sum >= 8):
     → Ready to move to new topic
     → May skip planned follow-up
   
   TIER 2 - ADEQUATE (sum 4-7):
     → Proceed normally
     → One clarifying follow-up if time permits
   
   TIER 3 - WEAK (sum 1-3):
     → Must probe deeper
     → Generate targeted follow-up on weakest signal
   
   TIER 4 - CONCERNING (sum <= 0):
     → Gentle redirect
     → Offer chance to elaborate
     → Note as potential red flag

4. IDENTIFY FOLLOW-UP FOCUS
   IF weak signals exist:
     focusArea = weakest signal category
     followUpType = "PROBE_DEEPER"
   
   ELSE IF strong but surface-level:
     focusArea = area with most potential depth
     followUpType = "GO_DEEPER"
   
   ELSE:
     focusArea = next uncovered dimension
     followUpType = "NEW_TOPIC"

5. RETURN analysis:
   {
     overallSummary: string,
     tier: 1|2|3|4,
     strengths: Evidence[],
     risks: Evidence[],
     missingSignals: string[],
     suggestedFocusForNextQuestion: string,
     followUpType: "PROBE_DEEPER" | "GO_DEEPER" | "NEW_TOPIC"
   }
```

### 2.3 Generating the Next Question

```
ALGORITHM: GenerateNextQuestion

INPUT:
  - analysis: AnswerAnalysis
  - interviewerRoles: Role[]
  - history: HistoryEntry[]
  - company, roleTitle, seniority

OUTPUT:
  - nextQuestion: Question

STEPS:

1. SELECT QUESTION TYPE
   Based on analysis.followUpType:
   
   PROBE_DEEPER:
     - Stay on same dimension
     - Ask for specifics, examples, clarification
     - Use phrases: "Can you elaborate...", "What specifically...", "Walk me through..."
   
   GO_DEEPER:
     - Stay on same dimension but increase complexity
     - Challenge assumptions, ask about edge cases
     - Use phrases: "What would happen if...", "How did you handle [edge case]..."
   
   NEW_TOPIC:
     - Move to next uncovered dimension
     - Select from role that has been underrepresented
     - Fresh question, no reference to previous answer

2. SELECT ROLE FOR NEXT QUESTION
   Calculate role balance:
   
   roleQuestionCount = COUNT questions per role in history
   targetDistribution = based on interviewerRoles and seniority
   
   underrepresentedRole = role furthest below target %
   
   IF followUpType == "PROBE_DEEPER" or "GO_DEEPER":
     Use same role as previous question
   ELSE:
     Use underrepresentedRole (unless previous 2 questions were same role)

3. SELECT DIMENSION
   IF followUpType in ["PROBE_DEEPER", "GO_DEEPER"]:
     Use analysis.suggestedFocusForNextQuestion
   ELSE:
     Select highest-priority uncovered dimension for chosen role

4. GENERATE QUESTION
   Based on:
   - Selected dimension
   - Question type
   - Role's question style
   - Seniority-appropriate complexity
   - Company/role context
   
   IF followUpType == "PROBE_DEEPER":
     Reference the specific weakness:
     "You mentioned [X]. Can you tell me more about [weak area]?"
   
   IF followUpType == "GO_DEEPER":
     Build on strength:
     "That's interesting. What about [more complex aspect]?"

5. GENERATE SUPPORTING CONTENT
   - Model answer (adjusted for follow-up context)
   - Hint (helpful but not revealing)
   - Display tag

6. RETURN nextQuestion
```

### 2.4 Session State Model

```typescript
interface SessionState {
  // Configuration
  company: string;
  roleTitle: string;
  seniority: 'Junior' | 'Mid' | 'Senior';
  interviewerRoles: Role[];
  
  // Progress tracking
  questionCount: number;
  questionsPerRole: Record<Role, number>;
  coveredDimensions: Record<Role, string[]>;
  
  // Signal aggregation
  aggregateStrengths: string[];
  aggregateRisks: string[];
  overallTier: number; // Running average
  
  // History
  history: HistoryEntry[];
}
```

---

## 3. JSON API Schema

### 3.1 Request Schema (Input)

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "InterviewRequest",
  "type": "object",
  "required": ["company", "roleTitle", "seniority", "interviewerRoles", "history"],
  "properties": {
    "company": {
      "type": "string",
      "description": "Target company name",
      "minLength": 1,
      "maxLength": 100,
      "examples": ["Google", "Stripe", "Airbnb"]
    },
    "roleTitle": {
      "type": "string",
      "description": "Job title being interviewed for",
      "minLength": 1,
      "maxLength": 200,
      "examples": ["Senior Software Engineer", "Product Manager", "Engineering Manager"]
    },
    "seniority": {
      "type": "string",
      "enum": ["Junior", "Mid", "Senior"],
      "description": "Candidate seniority level"
    },
    "interviewerRoles": {
      "type": "array",
      "items": {
        "type": "string",
        "enum": ["HR", "TECH", "TEAM_LEAD"]
      },
      "minItems": 1,
      "maxItems": 3,
      "uniqueItems": true,
      "description": "Selected interviewer role(s)"
    },
    "history": {
      "type": "array",
      "items": {
        "$ref": "#/definitions/HistoryEntry"
      },
      "description": "Previous Q&A pairs in this session"
    },
    "sessionConfig": {
      "type": "object",
      "properties": {
        "maxQuestions": {
          "type": "integer",
          "minimum": 5,
          "maximum": 20,
          "default": 10
        },
        "language": {
          "type": "string",
          "default": "en"
        },
        "strictMode": {
          "type": "boolean",
          "default": false,
          "description": "If true, interviewer is more challenging"
        }
      }
    }
  },
  "definitions": {
    "HistoryEntry": {
      "type": "object",
      "required": ["questionId", "role", "questionText", "modelAnswer", "userAnswer"],
      "properties": {
        "questionId": {
          "type": "string",
          "format": "uuid"
        },
        "role": {
          "type": "string",
          "enum": ["HR", "TECH", "TEAM_LEAD"]
        },
        "dimension": {
          "type": "string",
          "description": "The dimension this question targeted",
          "examples": ["Motivation", "System Design", "Conflict Resolution"]
        },
        "questionText": {
          "type": "string"
        },
        "modelAnswer": {
          "type": "string"
        },
        "userAnswer": {
          "type": "string"
        },
        "answerTimestamp": {
          "type": "string",
          "format": "date-time"
        }
      }
    }
  }
}
```

### 3.2 Response Schema (Output)

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "InterviewResponse",
  "type": "object",
  "required": ["nextQuestions", "analysis"],
  "properties": {
    "nextQuestions": {
      "type": "array",
      "items": {
        "$ref": "#/definitions/Question"
      },
      "minItems": 1,
      "maxItems": 3,
      "description": "Next question(s) to ask. Usually 1, but can be up to 3 for initial batch."
    },
    "analysis": {
      "$ref": "#/definitions/Analysis"
    },
    "sessionMetadata": {
      "type": "object",
      "properties": {
        "questionsRemaining": {
          "type": "integer"
        },
        "coveredDimensions": {
          "type": "object",
          "additionalProperties": {
            "type": "array",
            "items": { "type": "string" }
          }
        },
        "roleBalance": {
          "type": "object",
          "properties": {
            "HR": { "type": "number" },
            "TECH": { "type": "number" },
            "TEAM_LEAD": { "type": "number" }
          }
        },
        "overallProgress": {
          "type": "string",
          "enum": ["EARLY", "MIDDLE", "CLOSING"]
        }
      }
    }
  },
  "definitions": {
    "Question": {
      "type": "object",
      "required": ["id", "role", "questionText", "displayTag", "hintForCandidate"],
      "properties": {
        "id": {
          "type": "string",
          "format": "uuid"
        },
        "role": {
          "type": "string",
          "enum": ["HR", "TECH", "TEAM_LEAD"]
        },
        "dimension": {
          "type": "string",
          "description": "Which dimension this question probes"
        },
        "questionText": {
          "type": "string",
          "description": "The actual question to display"
        },
        "displayTag": {
          "type": "string",
          "description": "UI label like 'HR – Culture Fit'",
          "examples": ["HR – Motivation", "Tech – System Design", "Lead – Conflict"]
        },
        "hintForCandidate": {
          "type": "string",
          "description": "Non-revealing hint to help candidate structure answer"
        },
        "modelAnswer": {
          "type": "string",
          "description": "Example of a strong answer"
        },
        "followUpContext": {
          "type": "string",
          "description": "If this is a follow-up, explains why it was asked"
        },
        "difficulty": {
          "type": "string",
          "enum": ["EASY", "MEDIUM", "HARD"]
        }
      }
    },
    "Analysis": {
      "type": "object",
      "required": ["overallSummary", "strengths", "risks", "missingSignals", "suggestedFocusForNextQuestion"],
      "properties": {
        "overallSummary": {
          "type": "string",
          "description": "2-3 sentence summary of the answer quality"
        },
        "tier": {
          "type": "integer",
          "minimum": 1,
          "maximum": 4,
          "description": "1=Strong, 2=Adequate, 3=Weak, 4=Concerning"
        },
        "strengths": {
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "signal": { "type": "string" },
              "evidence": { "type": "string" }
            }
          }
        },
        "risks": {
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "signal": { "type": "string" },
              "evidence": { "type": "string" },
              "severity": {
                "type": "string",
                "enum": ["LOW", "MEDIUM", "HIGH"]
              }
            }
          }
        },
        "missingSignals": {
          "type": "array",
          "items": { "type": "string" },
          "description": "Expected signals that were not detected in the answer"
        },
        "suggestedFocusForNextQuestion": {
          "type": "string",
          "description": "What the next question should probe"
        },
        "followUpType": {
          "type": "string",
          "enum": ["PROBE_DEEPER", "GO_DEEPER", "NEW_TOPIC"],
          "description": "Recommended approach for next question"
        },
        "notableQuotes": {
          "type": "array",
          "items": { "type": "string" },
          "description": "Key phrases from the candidate's answer"
        }
      }
    }
  }
}
```

### 3.3 TypeScript Interfaces (for Angular)

```typescript
// interview.models.ts

export type Seniority = 'Junior' | 'Mid' | 'Senior';
export type InterviewerRole = 'HR' | 'TECH' | 'TEAM_LEAD';
export type FollowUpType = 'PROBE_DEEPER' | 'GO_DEEPER' | 'NEW_TOPIC';
export type AnswerTier = 1 | 2 | 3 | 4;
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';
export type Severity = 'LOW' | 'MEDIUM' | 'HIGH';
export type SessionProgress = 'EARLY' | 'MIDDLE' | 'CLOSING';

export interface HistoryEntry {
  questionId: string;
  role: InterviewerRole;
  dimension?: string;
  questionText: string;
  modelAnswer: string;
  userAnswer: string;
  answerTimestamp?: string;
}

export interface SessionConfig {
  maxQuestions?: number;
  language?: string;
  strictMode?: boolean;
}

export interface InterviewRequest {
  company: string;
  roleTitle: string;
  seniority: Seniority;
  interviewerRoles: InterviewerRole[];
  history: HistoryEntry[];
  sessionConfig?: SessionConfig;
}

export interface Question {
  id: string;
  role: InterviewerRole;
  dimension?: string;
  questionText: string;
  displayTag: string;
  hintForCandidate: string;
  modelAnswer?: string;
  followUpContext?: string;
  difficulty?: Difficulty;
}

export interface StrengthSignal {
  signal: string;
  evidence: string;
}

export interface RiskSignal {
  signal: string;
  evidence: string;
  severity: Severity;
}

export interface Analysis {
  overallSummary: string;
  tier?: AnswerTier;
  strengths: StrengthSignal[];
  risks: RiskSignal[];
  missingSignals: string[];
  suggestedFocusForNextQuestion: string;
  followUpType?: FollowUpType;
  notableQuotes?: string[];
}

export interface RoleBalance {
  HR?: number;
  TECH?: number;
  TEAM_LEAD?: number;
}

export interface SessionMetadata {
  questionsRemaining?: number;
  coveredDimensions?: Record<InterviewerRole, string[]>;
  roleBalance?: RoleBalance;
  overallProgress?: SessionProgress;
}

export interface InterviewResponse {
  nextQuestions: Question[];
  analysis: Analysis;
  sessionMetadata?: SessionMetadata;
}
```

### 3.4 Java DTOs (for Spring)

```java
// InterviewRequest.java
package com.example.interview.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record InterviewRequest(
    @NotBlank @Size(max = 100) String company,
    @NotBlank @Size(max = 200) String roleTitle,
    @NotNull Seniority seniority,
    @NotEmpty @Size(max = 3) List<InterviewerRole> interviewerRoles,
    @NotNull List<HistoryEntry> history,
    SessionConfig sessionConfig
) {}

// Enums
public enum Seniority { Junior, Mid, Senior }
public enum InterviewerRole { HR, TECH, TEAM_LEAD }
public enum FollowUpType { PROBE_DEEPER, GO_DEEPER, NEW_TOPIC }
public enum AnswerTier { STRONG(1), ADEQUATE(2), WEAK(3), CONCERNING(4);
    private final int value;
    AnswerTier(int value) { this.value = value; }
    public int getValue() { return value; }
}

// HistoryEntry.java
public record HistoryEntry(
    @NotBlank String questionId,
    @NotNull InterviewerRole role,
    String dimension,
    @NotBlank String questionText,
    @NotBlank String modelAnswer,
    @NotBlank String userAnswer,
    String answerTimestamp
) {}

// InterviewResponse.java
public record InterviewResponse(
    List<Question> nextQuestions,
    Analysis analysis,
    SessionMetadata sessionMetadata
) {}

// Question.java
public record Question(
    String id,
    InterviewerRole role,
    String dimension,
    String questionText,
    String displayTag,
    String hintForCandidate,
    String modelAnswer,
    String followUpContext,
    String difficulty
) {}

// Analysis.java
public record Analysis(
    String overallSummary,
    Integer tier,
    List<StrengthSignal> strengths,
    List<RiskSignal> risks,
    List<String> missingSignals,
    String suggestedFocusForNextQuestion,
    FollowUpType followUpType,
    List<String> notableQuotes
) {}

public record StrengthSignal(String signal, String evidence) {}
public record RiskSignal(String signal, String evidence, String severity) {}
```

---

## 4. Prompt Templates

### 4.1 Generate First Questions Template

```
SYSTEM PROMPT:
---
You are an expert interview question generator for a multi-role interview simulation system.

Your task is to generate the FIRST 3 questions for an interview session. These questions should:
1. Warm up the candidate (start easy, build intensity)
2. Cover multiple dimensions based on selected interviewer roles
3. Be tailored to the company, role, and seniority level

INTERVIEWER ROLE PROFILES:

**HR Interviewer** focuses on:
- Culture Fit: Alignment with company values
- Motivation: Career goals, reasons for applying
- Communication: Clarity, structure
- Stakeholder Management: Managing expectations
- Adaptability: Handling change
- Self-Awareness: Understanding strengths/weaknesses

**TECH Interviewer** focuses on:
- System Design: Architecture, scalability
- Problem Solving: Breaking down problems
- Code Ownership: Quality, testing
- Technical Depth: Mastery of tools
- Trade-off Reasoning: Justifying decisions
- Learning Velocity: Staying current

**TEAM_LEAD Interviewer** focuses on:
- Leadership: Guiding teams, accountability
- Collaboration: Cross-functional work
- Conflict Resolution: Difficult conversations
- Mentoring: Developing others
- Delivery: Meeting deadlines
- Decision Making: Calls under uncertainty

SENIORITY CALIBRATION:
- Junior: Focus on learning, potential, foundational skills
- Mid: Focus on independence, ownership, growing impact
- Senior: Focus on strategy, influence, technical leadership

OUTPUT FORMAT:
You must respond with valid JSON matching this structure:
{
  "questions": [
    {
      "id": "<uuid>",
      "role": "HR" | "TECH" | "TEAM_LEAD",
      "dimension": "<dimension name>",
      "questionText": "<the question>",
      "displayTag": "<Role – Dimension>",
      "hintForCandidate": "<helpful hint>",
      "modelAnswer": "<example strong answer>",
      "difficulty": "EASY" | "MEDIUM" | "HARD"
    }
  ]
}
---

USER PROMPT:
---
Generate the first 3 interview questions for this session:

**Company:** {{company}}
**Role Title:** {{roleTitle}}
**Seniority:** {{seniority}}
**Interviewer Roles:** {{interviewerRoles | join(", ")}}

Requirements:
1. Question 1 should be an easy warm-up (typically HR/motivation if HR is selected)
2. Question 2 should be medium difficulty
3. Question 3 should be medium-hard, open-ended
4. Distribute questions across selected roles appropriately
5. Each question must include a model answer showing what "great" looks like
6. Hints should help structure the answer without giving it away

Generate the questions now.
---
```

### 4.2 Analyze Answer and Generate Next Question Template

```
SYSTEM PROMPT:
---
You are an expert interview analyst and question generator. You have two tasks:
1. Analyze the candidate's most recent answer for strengths, risks, and missing signals
2. Generate the next interview question based on your analysis

ANALYSIS FRAMEWORK:

**Signal Categories to Evaluate:**
- Structure: Did they use STAR format? Clear narrative?
- Specificity: Names, dates, metrics, concrete details?
- Ownership: Appropriate "I" vs "We" balance?
- Outcome: Results mentioned? Lessons learned?
- Relevance: Did they answer the question asked?
- Depth: Technical accuracy? Nuance? Trade-offs?
- Self-Awareness: Acknowledge mistakes? Growth?
- Red Flags: Blame-shifting? Vagueness? Arrogance?

**Scoring Guide:**
For each signal: -1 (concerning), 0 (neutral), 1 (present), 2 (strong)

**Answer Tiers:**
- Tier 1 (Strong): Ready to move to new topic
- Tier 2 (Adequate): Proceed normally
- Tier 3 (Weak): Must probe deeper
- Tier 4 (Concerning): Gentle redirect, note red flag

**Follow-up Types:**
- PROBE_DEEPER: Ask for specifics on weak areas
- GO_DEEPER: Challenge a strong answer with complexity
- NEW_TOPIC: Move to uncovered dimension

ROLE BALANCE RULES:
- Track questions per role
- If one role is underrepresented, prioritize it for NEW_TOPIC
- For PROBE_DEEPER/GO_DEEPER, stay with same role as previous question

OUTPUT FORMAT:
Respond with valid JSON:
{
  "analysis": {
    "overallSummary": "<2-3 sentences>",
    "tier": 1|2|3|4,
    "strengths": [{"signal": "...", "evidence": "..."}],
    "risks": [{"signal": "...", "evidence": "...", "severity": "LOW|MEDIUM|HIGH"}],
    "missingSignals": ["..."],
    "suggestedFocusForNextQuestion": "...",
    "followUpType": "PROBE_DEEPER|GO_DEEPER|NEW_TOPIC",
    "notableQuotes": ["..."]
  },
  "nextQuestion": {
    "id": "<uuid>",
    "role": "HR|TECH|TEAM_LEAD",
    "dimension": "...",
    "questionText": "...",
    "displayTag": "...",
    "hintForCandidate": "...",
    "modelAnswer": "...",
    "followUpContext": "...",
    "difficulty": "EASY|MEDIUM|HARD"
  }
}
---

USER PROMPT:
---
Analyze this answer and generate the next question.

**Session Context:**
- Company: {{company}}
- Role Title: {{roleTitle}}
- Seniority: {{seniority}}
- Interviewer Roles: {{interviewerRoles | join(", ")}}

**Previous Questions & Answers:**
{% for entry in history %}
---
Question {{loop.index}} ({{entry.role}} – {{entry.dimension}}):
{{entry.questionText}}

Model Answer:
{{entry.modelAnswer}}

Candidate's Answer:
{{entry.userAnswer}}
---
{% endfor %}

**Questions per Role so far:**
- HR: {{hrCount}}
- TECH: {{techCount}}
- TEAM_LEAD: {{teamLeadCount}}

**Dimensions Already Covered:**
{{coveredDimensions | join(", ")}}

**Instructions:**
1. Analyze the MOST RECENT answer (Question {{history | length}})
2. Determine the answer tier and identify strengths/risks
3. Decide: PROBE_DEEPER, GO_DEEPER, or NEW_TOPIC
4. Generate the next question accordingly
5. If NEW_TOPIC, choose from uncovered dimensions and balance roles
6. The question should feel like a natural continuation of the interview

Generate your analysis and next question now.
---
```

### 4.3 Alternative: Single Combined Template

For simpler implementation, here's a unified template that handles both initial and follow-up cases:

```
SYSTEM PROMPT:
---
You are an adaptive interview engine that generates realistic, probing interview questions and analyzes candidate responses.

{{#if isInitial}}
MODE: INITIAL QUESTIONS
Generate the first 3 questions to start the interview.
{{else}}
MODE: ANALYZE & CONTINUE  
Analyze the latest answer and generate the next question.
{{/if}}

[... Include full role profiles and analysis framework from above ...]

OUTPUT FORMAT:
{
  {{#unless isInitial}}
  "analysis": { ... },
  {{/unless}}
  "nextQuestions": [ ... ]
}
---

USER PROMPT:
---
{{#if isInitial}}
Start a new interview session.
{{else}}
Continue the interview by analyzing the latest answer.
{{/if}}

**Company:** {{company}}
**Role:** {{roleTitle}}
**Seniority:** {{seniority}}
**Interviewers:** {{interviewerRoles}}

{{#unless isInitial}}
**History:**
{{#each history}}
Q{{@index}}: {{this.questionText}}
A: {{this.userAnswer}}
{{/each}}
{{/unless}}
---
```

---

## 5. Edge Cases

### 5.1 Very Short / Low-Effort Answers

**Detection Criteria:**
- Answer length < 50 characters
- Answer length < 20% of model answer length
- Single sentence responses to complex questions
- Generic responses: "I don't know", "Yes", "It depends"

**Interviewer Reaction:**

```json
{
  "analysis": {
    "tier": 4,
    "risks": [{
      "signal": "Low Effort",
      "evidence": "Answer was only {{length}} characters for a question requiring detailed response",
      "severity": "HIGH"
    }],
    "suggestedFocusForNextQuestion": "Give candidate another chance with scaffolding",
    "followUpType": "PROBE_DEEPER"
  },
  "nextQuestion": {
    "questionText": "I'd like to understand this better. Can you walk me through a specific example? What was the situation, what did you do, and what was the outcome?",
    "followUpContext": "Previous answer was too brief - offering structured guidance",
    "hintForCandidate": "Try using the STAR method: Situation, Task, Action, Result"
  }
}
```

**Prompt Addition for Edge Case:**
```
HANDLING SHORT ANSWERS:
If the candidate's answer is under 50 characters or clearly low-effort:
1. Do NOT mark as complete - they need another chance
2. Generate a supportive follow-up that:
   - Acknowledges the answer without judgment
   - Provides structure (e.g., "Let's break this down...")
   - Asks for ONE specific element (not everything at once)
3. If this is the SECOND short answer in a row:
   - Note it as a risk pattern
   - Consider moving to an easier question
   - Explicitly offer: "Would you like me to rephrase the question?"
```

### 5.2 Overlong Rambling Answers

**Detection Criteria:**
- Answer length > 2000 characters
- Answer length > 300% of model answer length
- Multiple topic switches within single answer
- Repetition detected (similar phrases appear multiple times)
- Low signal-to-noise ratio (lots of words, few concrete details)

**Interviewer Reaction:**

```json
{
  "analysis": {
    "tier": 2,
    "risks": [{
      "signal": "Lack of Conciseness",
      "evidence": "Answer was {{length}} characters with significant tangents",
      "severity": "MEDIUM"
    }],
    "missingSignals": ["Clear structure", "Prioritized information"],
    "suggestedFocusForNextQuestion": "Test ability to be concise under constraint",
    "followUpType": "PROBE_DEEPER"
  },
  "nextQuestion": {
    "questionText": "That's helpful context. If you had to summarize the single most important decision you made and its impact in 2-3 sentences, what would it be?",
    "followUpContext": "Testing conciseness after lengthy response",
    "hintForCandidate": "Focus on the ONE thing that mattered most"
  }
}
```

**Prompt Addition for Edge Case:**
```
HANDLING LONG/RAMBLING ANSWERS:
If the candidate's answer exceeds 2000 characters or lacks focus:
1. Extract the most relevant signals despite the noise
2. Note "conciseness" as a risk if appropriate for the role
3. Generate a follow-up that:
   - Acknowledges content: "You've covered a lot of ground..."
   - Asks for prioritization: "What was the MOST critical aspect?"
   - Sets an explicit constraint: "In one sentence..." or "If you had 30 seconds..."
4. Do NOT penalize if the role is senior/strategic (more context may be appropriate)
5. DO flag for customer-facing or time-sensitive roles
```

### 5.3 Skipped Questions

**Detection Criteria:**
- `userAnswer` is empty string, null, or whitespace only
- Answer is exactly "skip", "pass", "next", or similar
- Answer is "I don't have experience with this"

**Interviewer Reaction:**

**Case A: Legitimate Skip (No Experience)**
```json
{
  "analysis": {
    "tier": 2,
    "risks": [{
      "signal": "Gap in Experience",
      "evidence": "Candidate indicated no experience with {{dimension}}",
      "severity": "LOW"
    }],
    "suggestedFocusForNextQuestion": "Pivot to related dimension or hypothetical",
    "followUpType": "NEW_TOPIC"
  },
  "nextQuestion": {
    "questionText": "That's fine - not everyone has that exact experience. How would you APPROACH {{dimension}} if you encountered it in this role?",
    "followUpContext": "Pivoting to hypothetical after experience gap",
    "hintForCandidate": "Think about your learning process and how you'd tackle unfamiliar challenges"
  }
}
```

**Case B: Blank Skip (No Explanation)**
```json
{
  "analysis": {
    "tier": 4,
    "risks": [{
      "signal": "Skipped Without Explanation",
      "evidence": "Candidate provided no answer and no reason",
      "severity": "MEDIUM"
    }],
    "suggestedFocusForNextQuestion": "Offer alternative angle on same dimension",
    "followUpType": "PROBE_DEEPER"
  },
  "nextQuestion": {
    "questionText": "I noticed you passed on that one. Would it help if I asked about {{dimension}} from a different angle? For instance, {{alternative_framing}}",
    "followUpContext": "Offering alternative after skip",
    "hintForCandidate": "It's okay to not have direct experience - think about related situations"
  }
}
```

**Prompt Addition for Edge Case:**
```
HANDLING SKIPPED QUESTIONS:
If the candidate skips a question (empty answer or explicit skip):

1. Check if they explained why:
   - "No experience" → Legitimate, pivot to hypothetical or related area
   - "Don't know" → Offer to rephrase or provide context
   - No explanation → Gently acknowledge and offer alternative

2. Do NOT:
   - Penalize harshly for one skip
   - Ask the exact same question again
   - Make the candidate feel judged

3. DO:
   - Track skipped dimensions (may indicate pattern)
   - Offer hypothetical framing: "If you were to encounter..."
   - After 2+ skips in same role area, consider that dimension not assessable

4. Special handling by role:
   - HR skip: May indicate discomfort with self-disclosure (note, don't push)
   - Tech skip: May indicate knowledge gap (explore adjacent areas)
   - Team Lead skip: May indicate lack of leadership experience (appropriate for level?)
```

### 5.4 Summary: Edge Case Decision Matrix

| Scenario | Detection | Tier | Follow-Up Type | Interviewer Tone |
|----------|-----------|------|----------------|------------------|
| Short answer (1st time) | < 50 chars | 4 | PROBE_DEEPER | Supportive, scaffold |
| Short answer (2nd time) | < 50 chars, repeat | 4 | NEW_TOPIC | Concerned, pivot |
| Rambling answer | > 2000 chars | 2-3 | PROBE_DEEPER | Redirecting, constraint |
| Off-topic answer | Low relevance score | 3 | PROBE_DEEPER | Clarifying, redirect |
| Skip with reason | "No experience" | 2 | NEW_TOPIC | Understanding, pivot |
| Skip without reason | Empty/blank | 4 | PROBE_DEEPER | Curious, alternative |
| Contradicts earlier | Conflict detected | 3 | PROBE_DEEPER | Probing, clarify |
| Overly rehearsed | Generic, no specifics | 3 | GO_DEEPER | Challenging, dig |

---

## Appendix A: Dimension Reference

### Complete Dimension List by Role

**HR Dimensions:**
1. `CULTURE_FIT` - Values alignment, team dynamics
2. `MOTIVATION` - Career goals, drive, "why here"
3. `COMMUNICATION` - Clarity, listening, presentation
4. `STAKEHOLDER_MGMT` - Influence, expectation management
5. `ADAPTABILITY` - Change handling, resilience
6. `SELF_AWARENESS` - Strengths/weaknesses, feedback reception

**TECH Dimensions:**
1. `SYSTEM_DESIGN` - Architecture, scalability, reliability
2. `PROBLEM_SOLVING` - Decomposition, algorithms, debugging
3. `CODE_OWNERSHIP` - Quality, testing, documentation
4. `TECHNICAL_DEPTH` - Language/framework mastery
5. `TRADEOFF_REASONING` - Decision justification, constraints
6. `LEARNING_VELOCITY` - New tech adoption, curiosity

**TEAM_LEAD Dimensions:**
1. `LEADERSHIP` - Vision, accountability, delegation
2. `COLLABORATION` - Cross-functional, relationship building
3. `CONFLICT_RESOLUTION` - Difficult conversations, mediation
4. `MENTORING` - Coaching, feedback, development
5. `DELIVERY` - Execution, unblocking, scope management
6. `DECISION_MAKING` - Judgment under uncertainty

---

## Appendix B: Sample API Flow

```
1. User selects: Company="Stripe", Role="Senior Engineer", Seniority="Senior", Roles=["TECH", "TEAM_LEAD"]

2. Frontend calls: POST /api/interview/start
   Request: { company, roleTitle, seniority, interviewerRoles, history: [] }

3. Backend calls LLM with "Generate First Questions" template
   Response: 3 questions (2 TECH, 1 TEAM_LEAD)

4. User answers Question 1

5. Frontend calls: POST /api/interview/next
   Request: { ...same config, history: [Q1 + A1] }

6. Backend calls LLM with "Analyze & Generate Next" template
   Response: Analysis of A1 + Question 4

7. Repeat steps 4-6 until interview complete

8. Frontend calls: POST /api/interview/summary
   Request: { full history }
   Response: Final report with all analyses aggregated
```

---

*End of Design Specification*