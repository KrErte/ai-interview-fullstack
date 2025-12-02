// src/app/models/user-progress.model.ts
export interface UserProgress {
  totalJobAnalyses: number;
  totalTrainingSessions: number;

  lastActive: string | null;
  lastMatchScore: number | null;
  lastMatchSummary: string | null;

  lastTrainerStrengths: string[];
  lastTrainerWeaknesses: string[];

  trainingProgressPercent: number | null;
}
