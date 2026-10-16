import { TeamStatus } from './team.model';

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
  /** Task name -> share of this person's week (all tasks, 0..100, total 100); omitted = catalog shares. */
  taskShares?: Record<string, number>;
  /** Invite code: the result is added anonymously to that team's report. */
  teamCode?: string;
  /** Updates this browser's earlier team answer instead of adding a new one. */
  teamEditToken?: string;
}

/** The answers behind the last saved result, kept so it can be recalculated with the current catalog. */
export interface SavedAnswers {
  professionId: string;
  answers: Record<string, string>;
  /** The user's own task shares, when they moved the sliders; absent in older saves. */
  taskShares?: Record<string, number>;
  /** Team the result was added to, so later slider moves update that anonymous answer. */
  teamCode?: string;
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
  /** Only when the request had a team code or edit token. */
  team?: TeamStatus;
  /**
   * BILLING_ENABLED=true only: the full roadmaps are left out (empty or missing) and this short preview is
   * sent instead. Shape not final in the backend contract; a string, a list of strings or a list of
   * phases/stages with a title are all rendered (see roadmapTeaserLines).
   */
  roadmapTeaser?: RoadmapTeaser | null;
  /** BILLING_ENABLED=true and the roadmap is not unlocked: show the teaser and the price card. Absent/false = billing off. */
  roadmapLocked?: boolean;
}

export type RoadmapTeaser = string | Array<string | { title?: string }>;

/** POST /api/ai-readiness/roadmap (auth only) body. */
export interface FullRoadmapRequest {
  professionId: string;
  answers: Record<string, string>;
}

/** POST /api/ai-readiness/roadmap response; 402 {error:'payment_required'} when not entitled. */
export interface FullRoadmap {
  generalRoadmap: GeneralStage[];
  professionRoadmap: RoadmapPhase[];
  totalWeeks: number;
}

/** Saved in sessionStorage when "buy" sends the user to log in, so the paywall comes back with the same answers. */
export interface PendingPurchase {
  professionId: string;
  answers: Record<string, string>;
}
