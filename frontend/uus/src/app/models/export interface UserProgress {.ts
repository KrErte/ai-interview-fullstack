export interface UserProgress {
  totalJobAnalyses: number;
  totalTrainingSessions: number;
  lastActive: string | null;
  lastMatchScore: number | null;
  lastMatchSummary: string | null;
}
