import { JobMatchResult } from '../services/job.service';

export interface JobMatchSeed {
  matches: JobMatchResult[];
}

export const JOB_MATCH_SEED_RESULTS: JobMatchResult[] = [
  {
    jobTitle: 'Senior Frontend Engineer',
    jobDescription:
      'Lead the frontend for a modern SaaS platform using Angular and TypeScript. Collaborate closely with design and backend teams.',
    fitScore: 0.9,
    strengths: [
      'Strong experience with modern Angular and TypeScript',
      'Hands-on work with performance optimization and web vitals',
      'Experience mentoring junior engineers and leading small squads',
    ],
    weaknesses: [
      'Limited exposure to design systems at scale',
      'System design communication could be more structured',
    ],
    missingSkills: ['Storybook', 'Advanced accessibility (WCAG 2.2)'],
    roadmap: [
      'Prepare 2–3 STAR stories about leading large UI refactors',
      'Deep-dive on performance case studies (largest contentful paint, TTFB, etc.)',
      'Practice explaining design trade-offs for component libraries',
    ],
    suggestedImprovements:
      'Focus your narrative on ownership and cross-team collaboration. Bring 1–2 concise metrics about performance wins.',
    summary:
      'Excellent role fit. You are above baseline on technical depth; highlight impact and collaboration to stand out.',
  },
  {
    jobTitle: 'Frontend Engineer',
    jobDescription:
      'Contribute to an established React/Angular codebase, working on new features and iterative improvements.',
    fitScore: 0.78,
    strengths: [
      'Solid experience with component-driven development',
      'Comfortable working with designers and product managers',
    ],
    weaknesses: ['Limited testing portfolio (unit/e2e)', 'Less experience with CI pipelines'],
    missingSkills: ['Cypress', 'Playwright'],
    roadmap: [
      'Add 1–2 testing-focused projects to your portfolio',
      'Prepare examples of improving code quality over time',
    ],
    suggestedImprovements:
      'Have 1 concrete story about improving quality with tests, and 1 about unblocking a cross-team dependency.',
    summary:
      'Good fit for mid-level roles. Strengthen your testing narrative to avoid being screened out late.',
  },
];

export const JOB_MATCH_SEED_DATA: JobMatchSeed = {
  matches: JOB_MATCH_SEED_RESULTS,
};


