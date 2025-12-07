import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

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
    return this.http.get<DashboardStats>(`${this.baseUrl}/api/dashboard`, { params });
  }

  getUserProfile(email?: string): Observable<UserProfile> {
    const targetEmail = email || this.auth.getCurrentUserEmail() || '';
    const params = new HttpParams().set('email', targetEmail);
    return this.http.get<UserProfile>(`${this.baseUrl}/api/user/me`, { params });
  }

  saveUserProfile(profile: UserProfile): Observable<UserProfile> {
    const email = profile.email || this.auth.getCurrentUserEmail() || '';
    const params = new HttpParams().set('email', email);
    return this.http.put<UserProfile>(`${this.baseUrl}/api/user/me`, profile, { params });
  }

  getRoadmap(email?: string): Observable<RoadmapTask[]> {
    const targetEmail = email || this.auth.getCurrentUserEmail() || '';
    return this.http.get<RoadmapTask[]>(`${this.baseUrl}/api/roadmap/${targetEmail}`);
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

