export type SessionStatus = 'PENDING' | 'ACTIVE' | 'COMPLETED' | 'ERROR';

export type InterviewerType = 'TECHNICAL' | 'HR' | 'BEHAVIORAL' | 'SYSTEM_DESIGN' | 'AI_COACH';

export interface InterviewMessage {
  id?: string;
  role: 'INTERVIEWER' | 'CANDIDATE' | 'SYSTEM';
  content: string;
  createdAt?: string;
}

export interface InterviewPhase {
  id: string;
  name: string;
  description?: string;
  interviewerType: InterviewerType;
  order: number;
  isCurrent?: boolean;
}

export interface InterviewSession {
  id: string;
  candidateEmail: string;
  jobRole: string;
  status: SessionStatus;
  currentPhase?: InterviewPhase;
  phases?: InterviewPhase[];
  activeQuestion?: string;
  messages?: InterviewMessage[];
  createdAt?: string;
  updatedAt?: string;
}

export interface StartInterviewRequest {
  jobRole: string;
  candidateEmail: string;
}

export interface StartInterviewResponse {
  sessionId: string;
  session: InterviewSession;
}

export interface SubmitAnswerRequest {
  sessionId: string;
  answer: string;
}

export interface SubmitAnswerResponse {
  session: InterviewSession;
}


