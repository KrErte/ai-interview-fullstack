export interface AdaptiveAnalysisRequest {
  email: string | null;
  roadmapItemId: string | null;
  question: string;
  answer: string;
  currentSkillSnapshot?: Record<string, number> | null;
}

export interface AdaptiveAnalysisResponse {
  feedback: string | null;
  nextQuestion: string | null;
  weakestSkill: string | null;
  updatedSkills?: Record<string, number> | null;
  error?: string | null;
}
