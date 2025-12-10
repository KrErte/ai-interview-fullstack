import { JobMatchResult } from '../services/job.service';

export interface JobAnalysisHistoryItem {
  id: string;
  jobTitle: string;
  company: string;
  analyzedAt: string;
  result: JobMatchResult;
}

export interface JobAnalysisSeed {
  latest: JobMatchResult;
  history: JobAnalysisHistoryItem[];
}

export const JOB_ANALYSIS_SEED_LATEST: JobMatchResult = {
  jobTitle: 'Senior Frontend Engineer',
  jobDescription:
    'End-to-end ownership of a critical customer-facing Angular application, working closely with design and product.',
  fitScore: 0.88,
  strengths: [
    'Deep Angular / TypeScript experience',
    'Strong history of shipping features in fast-paced environments',
  ],
  weaknesses: ['Limited exposure to design systems at very large scale'],
  missingSkills: ['Storybook', 'ARIA authoring best practices'],
  roadmap: [
    'Prepare performance and UX case studies using concrete metrics',
    'Refresh knowledge on semantic HTML and ARIA patterns',
  ],
  suggestedImprovements:
    'Prepare 2–3 concise stories that demonstrate ownership from problem discovery to measurable impact.',
  summary:
    'Very strong fit for senior roles; emphasise leadership and coaching examples to reach the top of the band.',
};

export const JOB_ANALYSIS_SEED_HISTORY: JobAnalysisHistoryItem[] = [
  {
    id: 'JA-2025-01-12-aurora',
    jobTitle: 'Senior Frontend Engineer',
    company: 'Aurora Systems',
    analyzedAt: '2025-01-12T14:05:00Z',
    result: JOB_ANALYSIS_SEED_LATEST,
  },
  {
    id: 'JA-2024-12-03-lumina',
    jobTitle: 'Frontend Engineer',
    company: 'Lumina Labs',
    analyzedAt: '2024-12-03T09:40:00Z',
    result: {
      jobTitle: 'Frontend Engineer',
      jobDescription:
        'Work with a cross-functional team to build analytics dashboards and admin tools.',
      fitScore: 0.81,
      strengths: [
        'Experience building dashboards and data-heavy UIs',
        'Confident with TypeScript and API integration patterns',
      ],
      weaknesses: ['Fewer examples of working with data visualisation libraries'],
      missingSkills: ['D3.js', 'Charting best practices'],
      roadmap: [
        'Add 1 portfolio project showcasing charts and visualisations',
        'Practice explaining trade-offs between chart libraries',
      ],
      suggestedImprovements:
        'Highlight your experience making complex data understandable, even without advanced chart libraries.',
      summary:
        'Solid fit; emphasise collaboration with analysts or PMs to strengthen your narrative.',
    },
  },
];

export const JOB_ANALYSIS_SEED_DATA: JobAnalysisSeed = {
  latest: JOB_ANALYSIS_SEED_LATEST,
  history: JOB_ANALYSIS_SEED_HISTORY,
};


