// src/app/models/personality.model.ts

export interface PersonalityQuestion {
  id: string;
  text: string;
  dimensionKey: string;
  scaleLabelLow: string;
  scaleLabelHigh: string;
}

export interface PersonalityInterviewStartResponse {
  questions: PersonalityQuestion[];
}

/**
 * Lühikese tööstiili vormi (Step 1) andmed.
 * Neid saab backendis kasutada profiili baasväärtuste kalibreerimiseks.
 */
export interface WorkstyleBaseline {
  structurePreference: number;      // 0–100
  speedPreference: number;          // 0–100
  asyncPreference: number;          // 0–100
  conflictDirectness: number;       // 0–100
  autonomyNeed: number;             // 0–100
  chaosTolerance: number;           // 0–100
  notes?: string;
}

export interface PersonalityAnswerItem {
  questionId: string;
  value: number; // 1–5
  note?: string;
}

/**
 * NB! baseline on [optional], nii et olemasolev personality-interview komponent
 * ei pea midagi muutma – ta võib saata { email, answers } ilma baseline'ita.
 */
export interface PersonalityAnswerRequest {
  email: string;
  answers: PersonalityAnswerItem[];
  baseline?: WorkstyleBaseline;
}

export interface PersonalityProfile {
  email: string;
  structureScore: number;
  speedScore: number;
  asyncPreferenceScore: number;
  conflictDirectnessScore: number;
  autonomyScore: number;
  toleranceForChaosScore: number;
  communicationDetailScore: number;
  codeQualityStrictnessScore: number;
  summary: string;
  updatedAt: string;
}

export interface PersonalityDimensionDelta {
  key: string;
  label: string;
  valueA: number;
  valueB: number;
  difference: number;
}

export interface PersonalityCompatibilityRequest {
  emailA: string;
  emailB: string;
}

export interface PersonalityCompatibilityResponse {
  profileA: PersonalityProfile;
  profileB: PersonalityProfile;
  deltas: PersonalityDimensionDelta[];
  aiSummary: string;
}
