import { CvUploadResponse } from '../services/job.service';

export interface CvSeedMeta {
  fileName: string;
  lastUpdatedIso: string;
}

export interface CvSeed {
  response: CvUploadResponse;
  meta: CvSeedMeta;
}

export const CV_UPLOAD_SEED_RESPONSE: CvUploadResponse = {
  text:
    'Senior Frontend Engineer with 6+ years of experience building production Angular and React applications. ' +
    'Comfortable owning features end-to-end, mentoring juniors, and working closely with design and product.\n\n' +
    'Key skills: Angular, TypeScript, RxJS, NgRx, Tailwind, Node.js, REST, accessibility, performance.',
  headline: 'Senior Frontend Engineer – Angular / TypeScript / UX-minded',
  skills: [
    'Angular 17+',
    'TypeScript',
    'RxJS',
    'NgRx',
    'Tailwind CSS',
    'Node.js',
    'REST APIs',
    'Accessibility',
    'Performance tuning',
  ],
  experienceSummary:
    'Led multiple frontend migrations to modern Angular, introduced performance budgets, and collaborated with designers to ship accessible UI components.',
};

export const CV_SEED_META: CvSeedMeta = {
  fileName: 'Seed-Candidate-CV.pdf',
  lastUpdatedIso: '2025-01-10T10:15:00Z',
};

export const CV_SEED_DATA: CvSeed = {
  response: CV_UPLOAD_SEED_RESPONSE,
  meta: CV_SEED_META,
};


