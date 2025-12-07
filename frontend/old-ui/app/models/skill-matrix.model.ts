export interface SkillMatrixResponse {
  skills: SkillItem[];
  overallCompletionPercent: number;
  currentLevel: string;
  nextLevel: string;
  nextLevelGoals: string[];
}

export interface SkillItem {
  key: string;
  label: string;
  totalTasks: number;
  completedTasks: number;
  completionPercent: number;
}
