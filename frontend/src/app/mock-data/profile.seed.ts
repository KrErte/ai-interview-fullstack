import { UserProfile } from '../services/ai.service';

export interface ProfileSeed {
  profile: UserProfile;
}

export const PROFILE_SEED_PROFILE: UserProfile = {
  email: 'seed.candidate@example.com',
  fullName: 'Seed Candidate',
  currentRole: 'Frontend Engineer',
  targetRole: 'Senior Frontend Engineer',
  yearsOfExperience: 6,
  skills:
    'Angular, TypeScript, RxJS, NgRx, Tailwind CSS, Node.js, REST APIs, accessibility, performance optimisation',
  bio:
    'Frontend engineer focused on building robust, accessible experiences in Angular and TypeScript. Enjoys mentoring, debugging tricky issues, and working closely with design and product.',
  profileCompleteness: 86,
};

export const PROFILE_SEED_DATA: ProfileSeed = {
  profile: PROFILE_SEED_PROFILE,
};


