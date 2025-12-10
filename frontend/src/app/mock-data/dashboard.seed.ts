import { DashboardStats } from '../services/ai.service';
import { TrainingProgress } from '../services/training.service';

export interface DashboardSeed {
  stats: DashboardStats;
  trainingProgress: TrainingProgress;
}

export const DASHBOARD_STATS_SEED: DashboardStats = {
  email: 'seed.candidate@example.com',
  fullName: 'Seed Candidate',
  targetRole: 'Senior Frontend Engineer',
  profileCompleteness: 82,
  latestFitScore: 88,
  totalTrainingTasks: 24,
  completedTrainingTasks: 16,
  trainingProgressPercent: 67,
  cvUploaded: true,
  lastAnalysisSummary:
    'Strong overall fit for senior frontend roles; biggest gaps are advanced accessibility and performance tuning.',
  lastActive: '2025-01-15T18:42:00Z',
};

export const DASHBOARD_TRAINING_PROGRESS_SEED: TrainingProgress = {
  email: 'seed.candidate@example.com',
  totalTasks: 24,
  completedTasks: 16,
  totalJobAnalyses: 5,
  totalTrainingSessions: 3,
  trainingProgressPercent: 67,
  status: 'On track',
  lastActivityAt: '2025-01-15T18:42:00Z',
  lastMatchScore: 86,
  lastMatchSummary:
    'Latest match: 86% fit for Senior Frontend Engineer at Aurora Systems. Strong React/Angular experience; deepen system design stories.',
};

export const DASHBOARD_SEED_DATA: DashboardSeed = {
  stats: DASHBOARD_STATS_SEED,
  trainingProgress: DASHBOARD_TRAINING_PROGRESS_SEED,
};


