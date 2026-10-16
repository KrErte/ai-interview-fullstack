/** Returned once when a team is created; the report token is the only key to the report. */
export interface CreatedTeam {
  name: string;
  code: string;
  adminToken: string;
  inviteUrl: string;
  reportUrl: string;
  maxMembers: number;
}

export interface TeamInvite {
  name: string;
  full: boolean;
}

/** What happened to an anonymous team answer: added, updated, full or not-found. */
export interface TeamStatus {
  status: 'added' | 'updated' | 'full' | 'not-found';
  teamName?: string;
  editToken?: string;
}

export interface Percent {
  key: string;
  label: string;
  percent: number;
}

export interface Count {
  key: string;
  label: string;
  count: number;
}

export interface TeamTask {
  name: string;
  /** Share of the whole team's working time AI could take on through this task. */
  teamTimePercent: number;
  averageAutomation: number;
  respondents: number;
  observedUse: boolean;
}

export interface AnswerDistribution {
  questionId: string;
  question: string;
  options: Percent[];
}

export interface ProfessionGroup {
  name: string;
  respondents: number;
  averageReplaceability: number;
  averageReadiness: number;
}

/** Team aggregate. Until `ready` (5 respondents) only the header and the counter are filled. */
export interface TeamReport {
  teamName: string;
  code: string;
  inviteUrl: string;
  respondents: number;
  minRespondents: number;
  maxMembers: number;
  ready: boolean;
  firstDate?: string;
  lastDate?: string;
  averageReplaceability?: number;
  averageTaskExposure?: number;
  averageReadiness?: number;
  bands: Percent[];
  topTasks: TeamTask[];
  aiUse: AnswerDistribution[];
  readinessLevels: Percent[];
  stages: Count[];
  focus: string[];
  professions: ProfessionGroup[];
}
