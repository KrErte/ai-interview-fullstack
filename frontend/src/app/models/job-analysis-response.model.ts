// Peab ühtima backend DTO-ga JobAnalysisResponse (väljad "score" jne)
export interface JobAnalysisResponse {
  /**
   * Backendilt tulev toorskoor.
   * Võib olla 0–1 (proportsioon) või 0–100 (protsent).
   */
  score: number | null;

  /**
   * Frontendis arvutatud protsent (0–100).
   * Seda kasutame template'is sobivuse protsendi näitamiseks.
   */
  scorePercent?: number | null;

  summary: string;
  missingSkills: string[];
  roadmap: string[];
  suggestedImprovements: string[];
}
