export interface ProfessionSummary {
  id: string;
  name: string;
  category: string;
  /** Hand-written profession (shown without search); O*NET professions come from search. */
  featured?: boolean;
}

/** Optional features this server has configured (the AI comment needs an LLM API key). */
export interface Features {
  aiComment: boolean;
}

/** SOC major group with the number of professions in it, for browsing. */
export interface CategorySummary {
  name: string;
  count: number;
}

export interface QuestionOption {
  id: string;
  label: string;
}

export interface Question {
  id: string;
  text: string;
  help: string;
  options: QuestionOption[];
}

export type RiskBand = 'LOW' | 'MODERATE' | 'HIGH' | 'VERY_HIGH';
export type StageStatus = 'DONE' | 'CURRENT' | 'UPCOMING';

export interface TaskExposure {
  name: string;
  share: number;
  automationPercent: number;
  /** AI is already used for this task in practice (Anthropic Economic Index). */
  observedUse?: boolean;
}

export interface Resource {
  title: string;
  url: string;
}

export interface GeneralStage {
  id: string;
  title: string;
  goal: string;
  status: StageStatus;
  weeks: number;
  items: string[];
  resources: Resource[];
}

export interface RoadmapPhase {
  title: string;
  items: string[];
  project: string;
}

export interface AssessmentRequest {
  professionId: string;
  answers: Record<string, string>;
  personalize: boolean;
}

/** The answers behind the last saved result, kept so it can be recalculated with the current catalog. */
export interface SavedAnswers {
  professionId: string;
  answers: Record<string, string>;
}

/** A returning visitor's saved plan, for the "continue your plan" card. */
export interface SavedPlan {
  professionName: string;
  score: number;
  done: number;
  total: number;
}

export interface AssessmentResult {
  professionId: string;
  professionName: string;
  replaceabilityScore: number;
  taskExposureScore: number;
  personalAdjustment: number;
  band: RiskBand;
  headline: string;
  readinessScore: number;
  readinessLevel: string;
  tasks: TaskExposure[];
  humanEdge: string[];
  aiTools: string[];
  pivotRoles: string[];
  generalRoadmap: GeneralStage[];
  professionRoadmap: RoadmapPhase[];
  totalWeeks: number;
  personalNote: string | null;
}
