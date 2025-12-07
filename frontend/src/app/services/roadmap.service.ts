import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface RoadmapTask {
  taskKey: string;
  title: string;
  description: string;
  completed: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class RoadmapService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/roadmap`;

  constructor(private readonly http: HttpClient) {}

  getRoadmap(email: string): Observable<RoadmapTask[]> {
    return this.http.get<RoadmapTask[]>(`${this.baseUrl}/${encodeURIComponent(email)}`);
  }
}

