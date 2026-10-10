export interface ProfessionSummary {
  id: string;
  name: string;
  category: string;
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
