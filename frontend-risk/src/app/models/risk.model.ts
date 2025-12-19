/**
 * Risk assessment data model
 */
export interface RiskAssessment {
  /** Risk percentage (0-100) */
  riskPercent: number;

  /** Confidence level (0-1), displayed as percentage */
  confidence: number;

  /** Number of signals analyzed */
  signalsCount: number;

  /** Coverage percentage of assessment */
  coverage: number;

  /** List of identified strengths */
  strengths: string[];

  /** List of identified weaknesses */
  weaknesses: string[];

  /** Whether roadmap generation is allowed based on assessment depth */
  roadmapAllowedByDepth: boolean;

  /** Precision level of the roadmap: 'LOW' | 'MEDIUM' | 'HIGH' */
  roadmapPrecision?: 'LOW' | 'MEDIUM' | 'HIGH';

  /** Summary text */
  summary?: string;

  /** Roadmap steps if generated */
  roadmap?: string[];
}

/**
 * Job match result from backend
 */
export interface JobMatchResult {
  jobTitle?: string;
  jobDescription?: string;
  fitScore?: number;
  matchScore?: number;
  strengths?: string[];
  weaknesses?: string[];
  missingSkills?: string[];
  gaps?: string[];
  roadmap?: string[];
  suggestedImprovements?: string;
  summary?: string;
}

/**
 * CV Upload response
 */
export interface CvUploadResponse {
  text: string;
  headline?: string;
  skills?: string[];
  experienceSummary?: string;
}

/**
 * Risk state for the UI
 */
export interface RiskState {
  assessmentReady: boolean;
  loading: boolean;
  error: string | null;
  assessment: RiskAssessment | null;
}
