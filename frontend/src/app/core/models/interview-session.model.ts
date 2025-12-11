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

