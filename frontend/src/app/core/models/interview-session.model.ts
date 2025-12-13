export type InterviewerStyle = 'HR' | 'TECH' | 'TEAM_LEAD' | 'MIXED';

export type InterviewSeniority = 'JUNIOR' | 'MID' | 'SENIOR';

export interface InterviewSessionStartRequest {
  companyName: string;
  roleTitle: string;
  seniority: InterviewSeniority;
  interviewerStyle: InterviewerStyle;
}

export interface InterviewQuestionResponse {
  sessionId: string;
  questionNumber: number;
  totalQuestions: number;
  question: string;
  modelAnswerHint: string;
}

export interface InterviewAnswerRequest {
  sessionId: string;
  questionNumber: number;
  answer: string;
}

export interface InterviewLocalAnalysis {
  detectedStrengths: string[];
  detectedRisks: string[];
}

export interface InterviewNextQuestionResponse {
  sessionId: string;
  isFinished: false;
  questionNumber: number;
  totalQuestions: number;
  question: string;
  modelAnswerHint: string;
  localAnalysis?: InterviewLocalAnalysis;
}

export interface InterviewDimensionScore {
  dimension: string;
  score: number;
}

export interface InterviewSummaryResponse {
  sessionId: string;
  isFinished: true;
  fitScore: number;
  strengths: string[];
  weaknesses: string[];
  dimensionScores: InterviewDimensionScore[];
  verdict: string;
}

/**
 * Legacy / progress-based interview flow
 *
 * Backend contracts:
 * - Start session: POST /api/interview-sessions
 * - Next question: POST /api/interviews/{sessionUuid}/next-question
 */

export interface InterviewSessionCreateRequest {
  email: string;
}

export interface InterviewSessionCreateResponse {
  /**
   * Preferred identifier for progress API.
   */
  sessionUuid: string;
  /**
   * Kept for backwards compatibility if backend still exposes numeric ID.
   */
  sessionId?: string | number;
  email?: string;
}

export interface InterviewProgressUsedContext {
  lastAnswer: string;
  last3: string[];
  last5: string[];
}

export interface InterviewProgressStats {
  questionCount: number;
  currentDimension?: string;
  last1Average?: number;
  last3Average?: number;
  last5Average?: number;
}

export interface InterviewFitSnapshot {
  overall: number | null;
  currentDimension: number | null;
  trend: number | null;
  /**
   * Whether backend has computed the fit metrics for this turn.
   * If omitted or false, FE should treat it as "calculating".
   */
  computed?: boolean;
}

export interface InterviewFitInsight {
  type: 'STRENGTH' | 'RISK';
  text: string;
}

export interface InterviewFitDimensionBreakdown {
  key: string;
  label: string;
  scorePercent: number | null;
  band: string | null;
  insights: InterviewFitInsight[] | null;
}

export interface InterviewFitBreakdown {
  confidence: string | null; // LOW|MEDIUM|HIGH
  answeredCount: number;
  dimensions: InterviewFitDimensionBreakdown[] | null;
}

export interface InterviewProgressResponse {
  question: string;
  /**
   * Legacy field from earlier implementation. May be absent in newer contracts.
   */
  interviewerStyle?: InterviewerStyle;
  /**
   * Legacy field from earlier implementation. May be absent in newer contracts.
   */
  usedContext?: InterviewProgressUsedContext;
  /**
   * High-level decision about how the AI wants to steer the conversation next.
   */
  decision?: string;
  /**
   * Rolling progress stats including questionCount that should drive the UI step counter.
   */
  progress?: InterviewProgressStats;
  /**
   * Overall fit score for the session (0-100), if computed.
   */
  fitScore?: number | null;
  /**
   * High-level trend label for fit (e.g. "UP", "FLAT", "DOWN"), if provided.
   */
  fitTrend?: string | null;
  /**
   * Current fit snapshot for this answer / question.
   */
  fit?: InterviewFitSnapshot;
  /**
   * Detailed fit breakdown: dimensions, insights, confidence.
   */
  fitBreakdown?: InterviewFitBreakdown | null;
  /**
   * When true, the interview flow for this session is complete.
   */
  sessionComplete?: boolean;
}

