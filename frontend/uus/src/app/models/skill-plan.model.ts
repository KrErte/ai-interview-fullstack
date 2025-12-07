export interface SkillPlanDay {
  dayNumber: number;
  title: string;
  description: string;
  practiceTask: string;
}

export interface SkillPlanResponse {
  overallGoal: string;
  days: SkillPlanDay[];
}

export interface SkillPlanRequest {
  email: string;
  jobMatcherSummary: string;
  focusSkills: string[];
}
