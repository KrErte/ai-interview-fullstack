import { RoadmapTask } from '../services/ai.service';
import { TrainingProgress } from '../services/training.service';

export interface TrainingSeed {
  tasks: RoadmapTask[];
  progress: TrainingProgress;
}

export const TRAINING_ROADMAP_SEED_TASKS: RoadmapTask[] = [
  {
    taskKey: 'cv-refresh',
    title: 'Refresh your CV headline',
    description: 'Tighten your CV headline to clearly describe your target role and strengths.',
    completed: true,
    dayNumber: 1,
  },
  {
    taskKey: 'star-stories',
    title: 'Draft 3 STAR stories',
    description:
      'Write 3 short STAR stories covering ownership, debugging, and collaborating with difficult stakeholders.',
    completed: false,
    dayNumber: 2,
  },
  {
    taskKey: 'system-design-outline',
    title: 'Outline a small system design',
    description:
      'Prepare a 15-minute walkthrough of designing a feature flag service or notification system.',
    completed: false,
    dayNumber: 3,
  },
  {
    taskKey: 'frontend-performance',
    title: 'Review frontend performance basics',
    description:
      'Revise core performance concepts: bundle splitting, code-splitting, lazy loading, and caching.',
    completed: true,
    dayNumber: 4,
  },
];

export const TRAINING_PROGRESS_SEED: TrainingProgress = {
  email: 'seed.candidate@example.com',
  totalTasks: TRAINING_ROADMAP_SEED_TASKS.length,
  completedTasks: TRAINING_ROADMAP_SEED_TASKS.filter(t => t.completed).length,
  totalJobAnalyses: 5,
  totalTrainingSessions: 3,
  trainingProgressPercent: Math.round(
    (TRAINING_ROADMAP_SEED_TASKS.filter(t => t.completed).length /
      TRAINING_ROADMAP_SEED_TASKS.length) *
      100,
  ),
  status: 'On track',
  lastActivityAt: '2025-01-15T18:42:00Z',
  lastMatchScore: 86,
  lastMatchSummary:
    'Latest session focused on storytelling and impact; follow up with a short performance case study.',
};

export const TRAINING_SEED_DATA: TrainingSeed = {
  tasks: TRAINING_ROADMAP_SEED_TASKS,
  progress: TRAINING_PROGRESS_SEED,
};


