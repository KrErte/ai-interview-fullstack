// Peab ühtima backend DTO-ga JobAnalysisResponse (väli "score")
export interface JobAnalysisResponse {
  score: number | null;           // 0–1 või 0–100

  summary: string;
  missingSkills: string[];
  roadmap: string[];
  suggestedImprovements: string[];
}
