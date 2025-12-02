// src/app/models/user-progress.model.ts

export interface UserProgress {
  email: string;

  // Kokku treeningülesandeid (kui sul on vaja kuskil näidata)
  totalTasks: number;

  // Tehtud treeningülesanded
  completedTasks: number;

  // Viimane aktiivsus (võib olla null)
  lastActivityAt: string | null;

  // Treeningu staatus backendi enumist
  // (jätame stringiks, et ei hakkaks hetkel enumitega võitlema)
  status: string;

  // Job matcheri analüüside arv
  totalJobAnalyses: number;

  // Treening sessioonide arv
  totalTrainingSessions: number;

  // Üldine progress protsentides (0–100)
  trainingProgressPercent: number;
}
