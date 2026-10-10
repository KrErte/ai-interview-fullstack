/**
 * Job match data model: result of comparing a CV with a job description.
 * matchPercent is a CV-to-job match score (higher = better match), not a
 * replaceability score. Type names (RiskAssessment, RiskState) are kept for
 * compatibility with the /risk route and services.
 */
export interface RiskAssessment {
  /**
   * CV-to-job match score, rounded to an integer 0-100; higher means a better match.
   * null when the backend returned no usable score (no analysis result yet).
   */
  matchPercent: number | null;

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
  /** 0-1 fraction or 0-100; may arrive as a number or a numeric string. */
  fitScore?: number | string | null;
  /** Same contract as fitScore; used when fitScore is missing. */
  matchScore?: number | string | null;
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
 * Job match page state for the UI
 */
export interface RiskState {
  assessmentReady: boolean;
  loading: boolean;
  error: string | null;
  assessment: RiskAssessment | null;
}
