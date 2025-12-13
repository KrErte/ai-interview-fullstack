# Interview Intelligence Engine v1
## Deterministic Next-Question Generation Logic

> **Scope**: Backend logic for `POST /api/interviews/{sessionId}/next-question`  
> **Constraints**: No LLM calls — pure rule-based determinism  
> **Input**: Session state + lastAnswer + last3 + last5  
> **Output**: Next question + reasoning + metadata

---

# 1. CORE MENTAL MODEL

## 1.1 The Three Temporal Windows

| Window | Contains | Purpose | Decision Weight |
|--------|----------|---------|-----------------|
| **last1** | Most recent answer only | Immediate reaction — probe, clarify, or challenge | 60% of next-question decision |
| **last3** | Last 3 answers | Pattern detection — trend spotting | 30% of decision |
| **last5** | Last 5 answers | Behavioral baseline — overall trajectory | 10% of decision |

### Why This Split?

```
last1 → TACTICAL: "What did they just say? React to it."
last3 → PATTERN: "Is this a trend or a one-off?"
last5 → STRATEGIC: "Who is this person overall?"
```

**Key insight**: The interviewer should feel "present" and responsive (last1), but not whiplash-reactive (last3 smooths), while maintaining coherent coverage (last5 ensures we don't tunnel on one topic).

---

## 1.2 The Core Decision: DEEPEN vs CHALLENGE vs SWITCH

Every next-question decision is one of three actions:

| Action | Trigger | Goal | Feel |
|--------|---------|------|------|
| **DEEPEN** | Answer was good but shallow | Extract specifics, STAR details, impact | Curious, collaborative |
| **CHALLENGE** | Answer shows weakness signal | Stress-test, find the edge, probe inconsistency | Respectful pressure |
| **SWITCH** | Dimension sufficiently explored OR stuck | Move to new territory | Fresh, broader |

### Decision Tree (Deterministic)

```
INPUT: lastAnswer (scored 1-5 on depth, specificity, relevance)

IF lastAnswer.depth < 3 AND lastAnswer.relevance >= 3:
    → DEEPEN (they understand but gave surface-level response)
    
ELSE IF lastAnswer contains negative_signal (from dimension catalog):
    → CHALLENGE (probe the weakness)
    
ELSE IF lastAnswer.depth >= 4 AND current_dimension.question_count >= 3:
    → SWITCH (we've got good signal, move on)
    
ELSE IF last3.average_depth declining (d1 > d2 > d3):
    → SWITCH (candidate fatiguing on this topic)
    
ELSE IF last3 shows pattern_match (see Section 3):
    → CHALLENGE with pattern-specific follow-up
    
ELSE:
    → DEEPEN (default: get more detail before judging)
```

---

# 2. INTERVIEWER STYLE DIFFERENTIATION

## 2.1 Style Parameters

Each style has tunable behaviors:

```typescript
interface InterviewerStyle {
  styleKey: 'HR' | 'TECH' | 'TEAM_LEAD' | 'MIXED';
  
  // Behavioral tuning
  probeDepth: 1 | 2 | 3;        // How many follow-ups before switching
  challengeThreshold: number;   // Score below which we challenge (1-5)
  switchEagerness: number;      // 0.0-1.0, higher = switch faster
  
  // Dimension focus
  primaryDimensions: string[];  // Weight 2x
  secondaryDimensions: string[];// Weight 1x
  ignoredDimensions: string[];  // Skip these
  
  // Question bank priority
  questionTypes: ('behavioral' | 'situational' | 'technical' | 'values')[];
}
```

## 2.2 Style Profiles

### HR Style
```typescript
{
  styleKey: 'HR',
  probeDepth: 2,              // Moderate follow-up depth
  challengeThreshold: 2.5,    // Relatively gentle
  switchEagerness: 0.6,       // Moves across topics
  
  primaryDimensions: [
    'communication_clarity',
    'collaboration_style', 
    'stress_resilience',
    'learning_mindset',
    'cultural_alignment'
  ],
  secondaryDimensions: [
    'ownership',
    'stakeholder_management'
  ],
  ignoredDimensions: [
    'technical_initiative',
    'system_design_thinking'
  ],
  
  questionTypes: ['behavioral', 'values', 'situational']
}
```

**HR Behavior Rules**:
1. Opens with rapport-building questions
2. Focuses on motivation, culture fit, career trajectory
3. Challenges gently — reframes rather than confronts
4. Switches dimensions after 2-3 questions to cover breadth
5. Weights "how they communicate" as much as "what they say"

### TECH Style
```typescript
{
  styleKey: 'TECH',
  probeDepth: 3,              // Digs deep into technical claims
  challengeThreshold: 3.0,    // Stricter standard
  switchEagerness: 0.3,       // Stays on topic until satisfied
  
  primaryDimensions: [
    'technical_initiative',
    'ownership',
    'problem_solving_approach',
    'learning_mindset',
    'structured_communication'
  ],
  secondaryDimensions: [
    'collaboration_style',
    'stress_resilience'
  ],
  ignoredDimensions: [
    'cultural_alignment',
    'stakeholder_management'
  ],
  
  questionTypes: ['technical', 'situational', 'behavioral']
}
```

**TECH Behavior Rules**:
1. Opens with a concrete technical scenario
2. Probes for depth: "Walk me through exactly how..."
3. Challenges imprecise answers: "What alternatives did you consider?"
4. Stays on a technical thread until 3+ questions OR high confidence
5. Values precision over polish — rewards specific details

### TEAM_LEAD Style
```typescript
{
  styleKey: 'TEAM_LEAD',
  probeDepth: 2,
  challengeThreshold: 2.5,
  switchEagerness: 0.5,
  
  primaryDimensions: [
    'collaboration_style',
    'ownership',
    'leadership_signals',
    'conflict_management',
    'stakeholder_management'
  ],
  secondaryDimensions: [
    'technical_initiative',
    'learning_mindset'
  ],
  ignoredDimensions: [],      // Team lead considers everything
  
  questionTypes: ['behavioral', 'situational', 'values']
}
```

**TEAM_LEAD Behavior Rules**:
1. Opens with "Tell me about your team / your role on the team"
2. Looks for: team-first language, ownership signals, initiative
3. Challenges hero narratives: "Who else was involved?"
4. Probes for real collaboration: "How did you handle disagreement?"
5. Weights "would I want to work with them?" heavily

### MIXED Style
```typescript
{
  styleKey: 'MIXED',
  probeDepth: 2,
  challengeThreshold: 2.5,
  switchEagerness: 0.7,       // Highest — rotates between styles
  
  primaryDimensions: [
    // Rotates focus every 3-4 questions
  ],
  secondaryDimensions: [],
  ignoredDimensions: [],
  
  questionTypes: ['behavioral', 'situational', 'technical', 'values']
}
```

**MIXED Behavior Rules**:
1. Cycles through HR → TECH → TEAM_LEAD modes
2. Mode switch every 3-4 questions OR on dimension switch
3. Announces mode shifts subtly: "Now let's talk about the technical side..."
4. Combines scoring weights from all three styles
5. Best for comprehensive evaluation in limited time

---

# 3. PATTERN DETECTION RULES

## 3.1 Pattern → Follow-Up Mappings

These are the core "weakness hunting" rules. Each pattern detected in answers triggers a specific follow-up strategy.

### Universal Patterns (All Styles)

| Pattern ID | Detection Rule | Follow-Up Strategy | Target |
|------------|---------------|-------------------|--------|
| `vague_answer` | specificity_score < 3 | "Can you give me a specific example of that?" | Depth |
| `no_impact` | answer lacks metrics/outcomes | "What was the measurable impact?" | Ownership |
| `blame_shift` | uses "they" > "I" when describing failures | "What could YOU have done differently?" | Accountability |
| `rehearsed` | answer matches common templates exactly | "Tell me about a time that didn't go as planned" | Authenticity |
| `no_reflection` | describes events without lessons | "What would you do differently next time?" | Growth mindset |
| `solo_hero` | never mentions team in successes | "Who else contributed to that success?" | Collaboration |
| `avoids_conflict` | deflects conflict questions | "Tell me about a genuine disagreement you had" | Conflict mgmt |
| `always_perfect` | no failures or struggles mentioned in last3 | "What's been your biggest professional mistake?" | Self-awareness |

### HR-Specific Patterns

| Pattern ID | Detection Rule | Follow-Up Strategy | Target |
|------------|---------------|-------------------|--------|
| `motivation_unclear` | can't articulate why this role | "What specifically excites you about THIS position?" | Fit |
| `values_mismatch` | stated values contradict behaviors | "How does [stated value] show up in your daily work?" | Alignment |
| `salary_focus` | mentions comp 3+ times in last5 | "Beyond compensation, what matters most in your next role?" | Motivation |
| `past_negativity` | speaks negatively about past employers | "What did you appreciate about working at [past company]?" | Professionalism |

### TECH-Specific Patterns

| Pattern ID | Detection Rule | Follow-Up Strategy | Target |
|------------|---------------|-------------------|--------|
| `buzzword_heavy` | uses jargon without explanation | "Can you explain [term] as if to a junior developer?" | Depth |
| `no_tradeoffs` | presents decisions as obvious | "What did you sacrifice to achieve that?" | Systems thinking |
| `cant_debug` | vague about problem-solving process | "Walk me through your exact debugging steps" | Methodology |
| `no_alternatives` | doesn't mention options considered | "What alternatives did you evaluate?" | Rigor |
| `dismisses_testing` | minimizes QA importance | "Tell me about a bug that escaped your testing" | Quality mindset |

### TEAM_LEAD-Specific Patterns

| Pattern ID | Detection Rule | Follow-Up Strategy | Target |
|------------|---------------|-------------------|--------|
| `never_helps` | no mentions of helping colleagues | "When did you last stop your work to help a teammate?" | Team orientation |
| `never_stuck` | never mentions asking for help | "When were you truly stuck? How did you get unstuck?" | Humility |
| `unclear_ownership` | vague about responsibilities | "What was specifically YOUR responsibility vs others?" | Clarity |
| `no_pushback` | never disagreed with anything | "Tell me about a time you said no to a request" | Assertiveness |
| `avoids_process` | no process improvement suggestions | "What process would you change on your current team?" | Initiative |

---

## 3.2 Pattern Detection Algorithm

```typescript
interface PatternMatch {
  patternId: string;
  confidence: number;      // 0.0 - 1.0
  sourceWindow: 'last1' | 'last3' | 'last5';
  evidenceSnippets: string[];
}

function detectPatterns(
  lastAnswer: AnswerEvaluation,
  last3: AnswerEvaluation[],
  last5: AnswerEvaluation[],
  style: InterviewerStyle
): PatternMatch[] {
  
  const matches: PatternMatch[] = [];
  
  // Check last1 patterns (immediate)
  if (lastAnswer.specificity < 3) {
    matches.push({
      patternId: 'vague_answer',
      confidence: 1 - (lastAnswer.specificity / 5),
      sourceWindow: 'last1',
      evidenceSnippets: [lastAnswer.text.substring(0, 100)]
    });
  }
  
  // Check last3 patterns (trend)
  const pronounRatios = last3.map(a => calculatePronounRatio(a.text));
  if (pronounRatios.every(r => r.they > r.i * 2)) {
    matches.push({
      patternId: 'blame_shift',
      confidence: 0.8,
      sourceWindow: 'last3',
      evidenceSnippets: extractBlameSnippets(last3)
    });
  }
  
  // Check last5 patterns (baseline)
  const failureMentions = last5.filter(a => 
    containsFailureLanguage(a.text)
  ).length;
  if (failureMentions === 0 && last5.length >= 5) {
    matches.push({
      patternId: 'always_perfect',
      confidence: 0.9,
      sourceWindow: 'last5',
      evidenceSnippets: ['No failure/mistake mentions in 5 answers']
    });
  }
  
  // Add style-specific pattern checks
  matches.push(...detectStylePatterns(last3, style));
  
  return matches.sort((a, b) => b.confidence - a.confidence);
}
```

---

# 4. NEXT-QUESTION GENERATION ALGORITHM

## 4.1 Main Algorithm

```typescript
interface NextQuestionRequest {
  sessionId: string;
  lastAnswer: AnswerSubmission;
  last3: AnswerSubmission[];
  last5: AnswerSubmission[];
  sessionState: SessionState;
}

interface NextQuestionResponse {
  question: Question;
  action: 'DEEPEN' | 'CHALLENGE' | 'SWITCH';
  reasoning: ActionReasoning;
  metadata: QuestionMetadata;
}

function generateNextQuestion(req: NextQuestionRequest): NextQuestionResponse {
  
  const { lastAnswer, last3, last5, sessionState } = req;
  const style = sessionState.interviewerStyle;
  
  // Step 1: Evaluate the last answer
  const evaluation = evaluateAnswer(lastAnswer, sessionState.currentDimension);
  
  // Step 2: Detect patterns
  const patterns = detectPatterns(evaluation, 
    last3.map(evaluateAnswer), 
    last5.map(evaluateAnswer),
    style
  );
  
  // Step 3: Decide action
  const action = decideAction(evaluation, patterns, sessionState, style);
  
  // Step 4: Select question based on action
  const question = selectQuestion(action, patterns, sessionState, style);
  
  // Step 5: Build reasoning (for logging/debugging)
  const reasoning = buildReasoning(action, patterns, evaluation);
  
  return {
    question,
    action,
    reasoning,
    metadata: {
      dimension: question.targetDimension,
      questionIndex: sessionState.questionCount + 1,
      probeCount: action === 'SWITCH' ? 0 : sessionState.probeCount + 1,
      patternsTrigger: patterns.map(p => p.patternId)
    }
  };
}
```

## 4.2 Action Decision Logic

```typescript
function decideAction(
  evaluation: AnswerEvaluation,
  patterns: PatternMatch[],
  state: SessionState,
  style: InterviewerStyle
): 'DEEPEN' | 'CHALLENGE' | 'SWITCH' {
  
  const { depth, specificity, relevance } = evaluation;
  const { probeCount, currentDimension } = state;
  const dimensionScore = state.dimensionScores[currentDimension];
  
  // Rule 1: High-confidence negative pattern → CHALLENGE
  const strongPattern = patterns.find(p => p.confidence >= 0.7);
  if (strongPattern && probeCount < style.probeDepth) {
    return 'CHALLENGE';
  }
  
  // Rule 2: Shallow but relevant → DEEPEN
  if (depth < 3 && relevance >= 3 && probeCount < style.probeDepth) {
    return 'DEEPEN';
  }
  
  // Rule 3: Good answer + enough questions → SWITCH
  if (depth >= 4 && state.dimensionQuestionCount >= 2) {
    return 'SWITCH';
  }
  
  // Rule 4: Hit probe limit → SWITCH
  if (probeCount >= style.probeDepth) {
    return 'SWITCH';
  }
  
  // Rule 5: Score below threshold → CHALLENGE
  if (dimensionScore.provisional < style.challengeThreshold) {
    return 'CHALLENGE';
  }
  
  // Rule 6: Check coverage (last5) → might need to SWITCH
  const uncoveredDimensions = getUncoveredDimensions(state, style);
  if (uncoveredDimensions.length > 3 && state.questionCount > 5) {
    return 'SWITCH';
  }
  
  // Default: DEEPEN
  return 'DEEPEN';
}
```

## 4.3 Question Selection Logic

```typescript
function selectQuestion(
  action: 'DEEPEN' | 'CHALLENGE' | 'SWITCH',
  patterns: PatternMatch[],
  state: SessionState,
  style: InterviewerStyle
): Question {
  
  switch (action) {
    
    case 'DEEPEN': {
      // Use follow-up template from current question
      const currentQ = state.currentQuestion;
      if (currentQ.deepenFollowUps?.length > 0) {
        const unusedFollowUp = currentQ.deepenFollowUps
          .find(q => !state.askedQuestionIds.includes(q.id));
        if (unusedFollowUp) return unusedFollowUp;
      }
      // Fallback: generic deepen questions for this dimension
      return getGenericDeepenQuestion(state.currentDimension);
    }
    
    case 'CHALLENGE': {
      // Use pattern-specific challenge question
      const topPattern = patterns[0];
      if (topPattern) {
        const patternQuestion = getPatternChallengeQuestion(topPattern.patternId, style);
        if (patternQuestion) return patternQuestion;
      }
      // Fallback: dimension-specific challenge
      return getDimensionChallengeQuestion(state.currentDimension, style);
    }
    
    case 'SWITCH': {
      // Select next dimension based on coverage and style priority
      const nextDimension = selectNextDimension(state, style);
      // Get opening question for new dimension
      return getDimensionOpeningQuestion(nextDimension, style);
    }
  }
}
```

---

# 5. DIMENSION SELECTION ON SWITCH

## 5.1 Priority Algorithm

When switching dimensions, we need to pick the best next dimension:

```typescript
function selectNextDimension(
  state: SessionState,
  style: InterviewerStyle
): string {
  
  const candidates = getAllDimensions()
    .filter(d => !style.ignoredDimensions.includes(d))
    .map(dimension => ({
      dimension,
      priority: calculateDimensionPriority(dimension, state, style)
    }))
    .sort((a, b) => b.priority - a.priority);
  
  return candidates[0].dimension;
}

function calculateDimensionPriority(
  dimension: string,
  state: SessionState,
  style: InterviewerStyle
): number {
  
  let priority = 0;
  
  // Factor 1: Primary dimensions get 2x weight
  if (style.primaryDimensions.includes(dimension)) {
    priority += 20;
  } else if (style.secondaryDimensions.includes(dimension)) {
    priority += 10;
  }
  
  // Factor 2: Less coverage = higher priority
  const coverage = state.dimensionScores[dimension]?.questionCount || 0;
  priority += (5 - Math.min(coverage, 5)) * 5;  // 0 questions = +25, 5 questions = 0
  
  // Factor 3: Lower confidence = higher priority
  const confidence = state.dimensionScores[dimension]?.confidence || 0;
  priority += (1 - confidence) * 15;  // 0 confidence = +15, 1.0 = 0
  
  // Factor 4: Weakness signals = higher priority (we want to probe)
  const hasWeaknessSignal = state.dimensionScores[dimension]?.hasNegativeSignal;
  if (hasWeaknessSignal) {
    priority += 10;
  }
  
  // Factor 5: Recency penalty (avoid switching back immediately)
  const lastAsked = state.dimensionLastAskedIndex[dimension] || -100;
  const recencyPenalty = Math.max(0, 10 - (state.questionCount - lastAsked));
  priority -= recencyPenalty;
  
  return priority;
}
```

---

# 6. OUTPUT FORMATS

## 6.1 NextQuestionResponse (Full)

```json
{
  "question": {
    "id": "q_own_003",
    "text": "You mentioned the project succeeded — what could YOU have done differently to make it even better?",
    "type": "challenge",
    "targetDimension": "ownership",
    "targetSignals": ["self_reflection", "accountability"],
    "difficulty": 3,
    "expectedDuration": 90
  },
  "action": "CHALLENGE",
  "reasoning": {
    "trigger": "pattern_detected",
    "patternId": "blame_shift",
    "patternConfidence": 0.85,
    "sourceWindow": "last3",
    "explanation": "Candidate used 'they' 8x and 'I' 2x when describing project challenges"
  },
  "metadata": {
    "dimension": "ownership",
    "questionIndex": 7,
    "probeCount": 2,
    "patternsTriggered": ["blame_shift"],
    "sessionProgress": {
      "questionsAsked": 6,
      "dimensionsCovered": 3,
      "estimatedTimeRemaining": 18
    }
  }
}
```

## 6.2 Compact Response (for Frontend)

```json
{
  "questionId": "q_own_003",
  "questionText": "You mentioned the project succeeded — what could YOU have done differently to make it even better?",
  "dimension": "ownership",
  "questionNumber": 7,
  "interviewProgress": 0.35,
  "adaptationType": "challenge"
}
```

## 6.3 Internal Logging Format

```json
{
  "timestamp": "2025-01-15T14:32:01Z",
  "sessionId": "sess_abc123",
  "decisionLog": {
    "inputHash": "sha256:abc...",
    "lastAnswerScores": {
      "depth": 3,
      "specificity": 2,
      "relevance": 4
    },
    "patternsDetected": [
      {"id": "blame_shift", "confidence": 0.85, "source": "last3"}
    ],
    "actionDecision": "CHALLENGE",
    "ruleTriggered": "Rule 1: High-confidence negative pattern",
    "questionSelected": "q_own_003",
    "selectionReason": "Pattern-specific challenge question"
  }
}
```

---

# 7. EDGE CASES & GUARDRAILS

## 7.1 Stuck Detection

If we're getting nowhere on a dimension:

```typescript
function isStuck(state: SessionState): boolean {
  const recentScores = state.last5
    .filter(a => a.dimension === state.currentDimension)
    .map(a => a.depth);
  
  // Stuck if: 3+ questions on same dimension, all depth < 3
  return recentScores.length >= 3 && recentScores.every(s => s < 3);
}

// If stuck: force SWITCH, mark dimension as "needs_revisit"
```

## 7.2 Fatigue Detection

```typescript
function detectFatigue(last5: AnswerEvaluation[]): boolean {
  if (last5.length < 5) return false;
  
  // Fatigue signal: depth AND length both declining
  const depths = last5.map(a => a.depth);
  const lengths = last5.map(a => a.wordCount);
  
  const depthTrend = linearTrend(depths);
  const lengthTrend = linearTrend(lengths);
  
  return depthTrend < -0.3 && lengthTrend < -0.2;
}

// If fatigued: switch to easier/lighter questions, or consider ending
```

## 7.3 Question Repetition Prevention

```typescript
function ensureNoRepetition(
  question: Question,
  state: SessionState
): Question {
  
  if (state.askedQuestionIds.includes(question.id)) {
    // Find alternative in same category
    return findAlternativeQuestion(question, state);
  }
  
  // Also check for semantic similarity to recent questions
  const recentQuestions = state.last3.map(a => state.questionsAsked[a.questionId]);
  if (isTooSimilar(question, recentQuestions)) {
    return findAlternativeQuestion(question, state);
  }
  
  return question;
}
```

## 7.4 Style Consistency

For MIXED style, ensure smooth transitions:

```typescript
function getMixedStyleMode(state: SessionState): 'HR' | 'TECH' | 'TEAM_LEAD' {
  const cycleLength = 4;  // Questions per mode
  const cyclePosition = state.questionCount % (cycleLength * 3);
  
  if (cyclePosition < cycleLength) return 'HR';
  if (cyclePosition < cycleLength * 2) return 'TECH';
  return 'TEAM_LEAD';
}
```

---

# 8. IMPLEMENTATION CHECKLIST

## Phase 1: Core Logic
- [ ] Implement `AnswerEvaluation` scoring (depth, specificity, relevance)
- [ ] Implement `detectPatterns()` for universal patterns
- [ ] Implement `decideAction()` decision tree
- [ ] Implement `selectQuestion()` for each action type
- [ ] Create question bank structure with follow-ups

## Phase 2: Style Differentiation
- [ ] Implement style profiles (HR, TECH, TEAM_LEAD, MIXED)
- [ ] Add style-specific pattern detection
- [ ] Implement `selectNextDimension()` with style weights
- [ ] Add style-specific question filtering

## Phase 3: Guardrails
- [ ] Implement stuck detection
- [ ] Implement fatigue detection  
- [ ] Add question repetition prevention
- [ ] Add session time/count limits

## Phase 4: Output & Integration
- [ ] Define final JSON response schema
- [ ] Add internal logging format
- [ ] Create compact frontend response mapper
- [ ] Add reasoning/explanation generation for debugging

---

# 9. TESTING SCENARIOS

## Scenario 1: Vague First Answer
```
Input: lastAnswer.depth=2, lastAnswer.specificity=2, lastAnswer.relevance=4
Expected: action=DEEPEN, question=specific example follow-up
```

## Scenario 2: Blame Pattern Detected
```
Input: last3 all have they/I ratio > 2
Expected: action=CHALLENGE, question=ownership probe
```

## Scenario 3: Strong Answer, Good Coverage
```
Input: lastAnswer.depth=5, dimension.questionCount=3, dimension.confidence=0.8
Expected: action=SWITCH, new dimension selected by priority
```

## Scenario 4: TECH Style Deep Dive
```
Input: style=TECH, lastAnswer mentions "we chose microservices"
Expected: probe for alternatives, trade-offs, NOT switch quickly
```

## Scenario 5: Stuck Candidate
```
Input: last3 on same dimension, all depth<3
Expected: SWITCH forced, dimension marked for revisit
```

---

# Appendix A — Interview Intelligence v1.0.1 ADDENDUM (JSON)

```json
{
  "title": "Interview Intelligence v1.0.1 ADDENDUM",
  "status": "APPROVED",
  "purpose": "Clarifications to resolve deterministic implementation blockers in v1.0",
  "supersedes": null,
  "addendum_sections": [
    {
      "id": "A1",
      "resolves": "#1 Scale/Threshold Mismatch",
      "clarification": {
        "score_normalization": "All score comparisons use normalized values.",
        "formula": "(raw_score - scale.min) / (scale.max - scale.min)",
        "example": "Raw score 3 on 1-5 scale → (3-1)/(5-1) = 0.5"
      }
    },
    {
      "id": "A2",
      "resolves": "#2 Undefined 'flat' Trend",
      "clarification": {
        "flat_definition": "last3_answers.score.trend == 'flat' when (max - min) of last 3 normalized scores <= 0.15",
        "formula": "flat = (max(last3) - min(last3)) <= 0.15"
      }
    },
    {
      "id": "A3",
      "resolves": "#3 Rule Priority Order",
      "clarification": {
        "evaluation_mode": "first_match_wins",
        "priority_order": [
          "R6",
          "R1",
          "R5",
          "R3",
          "R4",
          "R2",
          "R_DEFAULT"
        ],
        "behavior": "Evaluate rules in priority order. Execute first rule whose condition is true. Stop evaluation."
      }
    },
    {
      "id": "A4",
      "resolves": "#4 History Window Guards",
      "clarification": {
        "R3_guard": "R3 condition only evaluates when question_count >= 3. Otherwise R3 is skipped.",
        "R4_guard": "R4 condition only evaluates when question_count >= 5. Otherwise R4 is skipped."
      }
    },
    {
      "id": "A5",
      "resolves": "#5 Initial Dimension Selection",
      "clarification": {
        "algorithm": "Select first dimension from style.focus_dimensions array."
      }
    },
    {
      "id": "A6",
      "resolves": "#6 probe_count Lifecycle",
      "clarification": {
        "initial_value": 0,
        "increment": "probe_count++ immediately after select_probe_question executes",
        "reset_on_dimension_switch": "probe_count = 0 immediately after switch_dimension executes",
        "reset_on_challenge": "probe_count = 0 immediately after select_challenge_question executes"
      }
    },
    {
      "id": "A7",
      "resolves": "#7 Escalate/Switch Ambiguity",
      "clarification": {
        "R5_action_replacement": "escalate_or_switch_dimension is replaced by switch_dimension",
        "rationale": "Escalation is out of scope for v1.0. R5 deterministically switches dimension."
      }
    },
    {
      "id": "A8",
      "resolves": "#8 Coverage Definition",
      "clarification": {
        "removal": "Remove 'all_focus_dimensions_covered' from completion.criteria.",
        "completion_criteria_v1.0.1": [
          "max_questions_reached"
        ],
        "rationale": "Dimension coverage tracking requires additional state not defined in v1.0. Deferred to v1.1."
      }
    },
    {
      "id": "A9",
      "resolves": "#9 Question Selection Strategy",
      "clarification": {
        "intra_type_selection": "sequential_no_repeat",
        "algorithm": "For each (dimension, question_type) pair, maintain index starting at 0. Select question at current index. Increment index after selection. Track used question IDs in session state.",
        "exhaustion_behavior": {
          "order": [
            "opening",
            "probe",
            "challenge"
          ],
          "rule": "If current question_type exhausted in current_dimension, select from next type in order.",
          "type_exhaustion": "If all types exhausted in current_dimension, execute switch_dimension.",
          "total_exhaustion": "If all questions in all focus_dimensions exhausted, execute complete_session."
        },
        "session_state_addition": {
          "field": "used_question_ids",
          "type": "array<string>",
          "init": []
        }
      }
    },
    {
      "id": "A10",
      "resolves": "#10 Boundary Condition",
      "clarification": {
        "threshold_equality": "Score exactly equal to challenge_threshold satisfies '>=' condition (R4), not '<' condition (R2)."
      }
    },
    {
      "id": "A11",
      "resolves": "#11 Default Fallback Rule",
      "clarification": {
        "new_rule": {
          "id": "R_DEFAULT",
          "if": "no_other_rule_matched",
          "then": "select_opening_question",
          "note": "Evaluated last per A3 priority order. Selects opening question in current_dimension."
        }
      }
    },
    {
      "id": "A12",
      "resolves": "#12 Threshold Source of Truth",
      "clarification": {
        "canonical_source": "interview-intelligence-v1-canonical.json → style_parameters",
        "question_bank_role": "question-bank-seed-v1.json → style_overrides.challenge_threshold values are advisory only. Implementations MUST read from canonical source.",
        "conflict_resolution": "If values differ, canonical source wins."
      }
    },
    {
      "id": "A13",
      "resolves": "Dimension Switch Target",
      "clarification": {
        "algorithm": "On switch_dimension: select next dimension in style.focus_dimensions array (circular).",
        "formula": "next_index = (current_index + 1) % focus_dimensions.length",
        "single_dimension_case": "If focus_dimensions.length == 1, switch_dimension is a no-op (remain in same dimension)."
      }
    },
    {
      "id": "A14",
      "resolves": "Weighted Average Specification",
      "clarification": {
        "last5_weights": [
          0.1,
          0.15,
          0.2,
          0.25,
          0.3
        ],
        "weight_order": "Oldest answer gets weight[0], newest gets weight[4].",
        "formula": "weighted_avg = sum(score[i] * weight[i]) for i in 0..4"
      }
    }
  ],
  "updated_rules_summary": {
    "evaluation_order": [
      "R6",
      "R1",
      "R5",
      "R3",
      "R4",
      "R2",
      "R_DEFAULT"
    ],
    "R1": {
      "if": "question_count == 0",
      "then": "select_opening_question"
    },
    "R2": {
      "if": "last_answer.score.normalized < challenge_threshold",
      "then": "select_probe_question"
    },
    "R3": {
      "if": "question_count >= 3 AND flat(last3)",
      "then": "switch_dimension"
    },
    "R4": {
      "if": "question_count >= 5 AND last5_weighted_avg >= challenge_threshold",
      "then": "select_challenge_question"
    },
    "R5": {
      "if": "probe_count >= style.probe_depth",
      "then": "switch_dimension"
    },
    "R6": {
      "if": "question_count >= max_questions",
      "then": "complete_session"
    },
    "R_DEFAULT": {
      "if": "no_other_rule_matched",
      "then": "select_opening_question"
    }
  },
  "state_lifecycle_summary": {
    "probe_count": {
      "init": 0,
      "increment_on": "select_probe_question",
      "reset_on": [
        "switch_dimension",
        "select_challenge_question"
      ]
    },
    "current_dimension": {
      "init": "focus_dimensions[0]",
      "update_on": "switch_dimension → focus_dimensions[(current_index + 1) % length]"
    },
    "question_count": {
      "init": 0,
      "increment_on": "any question selection (opening, probe, or challenge)"
    }
  }
}
```
