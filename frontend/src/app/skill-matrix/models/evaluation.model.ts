export type RecommendationLevel = 'STRONGLY_RECOMMEND' | 'RECOMMEND' | 'NEUTRAL' | 'NOT_RECOMMEND';

export interface SkillScore {
  skillKey: string;
  label: string;
  category?: string;
  score: number; // 0-100
}

export interface CategoryAggregate {
  category: string;
  averageScore: number;
}

export interface SkillMatrix {
  skills: SkillScore[];
  overallScore?: number;
  categories?: CategoryAggregate[];
}

export interface AggregatedResult {
  sessionId: string;
  matrix: SkillMatrix;
  recommendation: RecommendationLevel;
  summary?: string;
}


