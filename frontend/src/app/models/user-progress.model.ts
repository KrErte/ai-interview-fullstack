export interface UserProgress {
  email: string;

  totalTasks: number;
  completedTasks: number;

  totalJobAnalyses: number;
  totalTrainingSessions: number;

  trainingProgressPercent: number;
  status: string;

  lastActivityAt: string | null;
  lastMatchScore: number | null;
  lastMatchSummary: string | null;
}
