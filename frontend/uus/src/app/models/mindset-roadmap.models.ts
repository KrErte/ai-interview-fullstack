export interface MindsetRoadmapSummary {
  roadmapKey: string;
  title: string;
  totalTasks: number;
  completedTasks: number;
  progressPercent: number;
}

export interface MindsetTaskDto {
  taskKey: string;
  completed: boolean;
  score?: number | null;
  updatedAt?: string | null;
}

export interface MindsetRoadmapDetail {
  summary: MindsetRoadmapSummary;
  tasks: MindsetTaskDto[];
}

export interface TrainingTaskRequest {
  email: string;
  taskKey: string;
  completed: boolean;
}
