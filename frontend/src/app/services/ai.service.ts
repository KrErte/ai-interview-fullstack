import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';
import { DASHBOARD_STATS_SEED } from '../mock-data/dashboard.seed';
import { PROFILE_SEED_PROFILE } from '../mock-data/profile.seed';
import { TRAINING_ROADMAP_SEED_TASKS } from '../mock-data/training.seed';

export interface RoadmapTask {
  taskKey: string;
  title: string;
  description: string;
  completed: boolean;
  dayNumber?: number;
}

export interface UserProfile {
  email?: string;
  fullName?: string;
  currentRole?: string;
  targetRole?: string;
  yearsOfExperience?: number;
  skills?: string;
  bio?: string;
  profileCompleteness?: number;
}

export interface DashboardStats {
  email: string;
  fullName?: string;
  targetRole?: string;
  profileCompleteness?: number;
  latestFitScore?: number;
  totalTrainingTasks?: number;
  completedTrainingTasks?: number;
  trainingProgressPercent?: number;
  cvUploaded: boolean;
  lastAnalysisSummary?: string;
  lastActive?: string;
}

@Injectable({ providedIn: 'root' })
export class AiService {
  private readonly baseUrl = environment.apiBaseUrl;

  constructor(private http: HttpClient, private auth: AuthService) {}

  getDashboard(email?: string): Observable<DashboardStats> {
    const targetEmail = email || this.auth.getCurrentUserEmail() || '';
    const params = new HttpParams().set('email', targetEmail);
    return this.http
      .get<DashboardStats>(`${this.baseUrl}/api/dashboard`, { params })
      .pipe(
        map((response) => response || DASHBOARD_STATS_SEED),
        catchError(() => of(DASHBOARD_STATS_SEED))
      );
  }

  getUserProfile(email?: string): Observable<UserProfile> {
    const targetEmail = email || this.auth.getCurrentUserEmail() || '';
    const params = new HttpParams().set('email', targetEmail);
    return this.http
      .get<UserProfile>(`${this.baseUrl}/api/user/me`, { params })
      .pipe(
        map((response) => response || PROFILE_SEED_PROFILE),
        catchError(() => of(PROFILE_SEED_PROFILE))
      );
  }

  saveUserProfile(profile: UserProfile): Observable<UserProfile> {
    const email = profile.email || this.auth.getCurrentUserEmail() || '';
    const params = new HttpParams().set('email', email);
    return this.http.put<UserProfile>(`${this.baseUrl}/api/user/me`, profile, { params });
  }

  getRoadmap(email?: string): Observable<RoadmapTask[]> {
    const targetEmail = email || this.auth.getCurrentUserEmail() || '';
    return this.http
      .get<RoadmapTask[]>(`${this.baseUrl}/api/roadmap/${targetEmail}`)
      .pipe(
        map((response) => (response && response.length ? response : TRAINING_ROADMAP_SEED_TASKS)),
        catchError(() => of(TRAINING_ROADMAP_SEED_TASKS))
      );
  }

  updateRoadmapTask(taskKey: string, completed: boolean, email?: string): Observable<RoadmapTask[]> {
    const targetEmail = email || this.auth.getCurrentUserEmail() || '';
    return this.http.post<RoadmapTask[]>(`${this.baseUrl}/api/roadmap/update`, {
      email: targetEmail,
      taskKey,
      completed
    });
  }
}

