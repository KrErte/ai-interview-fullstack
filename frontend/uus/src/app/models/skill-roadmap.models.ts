// src/app/shared/models/skill-roadmap.models.ts

export type RoleLevel = 'Junior' | 'Mid' | 'Senior' | 'Architect';
export type RoadmapStatus = 'done' | 'in-progress' | 'next';

export interface RoadmapItem {
  title: string;
  subtitle: string;
  status: RoadmapStatus;
}
