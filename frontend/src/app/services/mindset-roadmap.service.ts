import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  MindsetRoadmapDetail,
  MindsetRoadmapSummary,
  TrainingTaskRequest
} from '../models/mindset-roadmap.models';   // <── NB! ../models, mitte ./models

@Injectable({
  providedIn: 'root'
})
export class MindsetRoadmapService {

  private readonly baseUrl = 'http://localhost:8080/api/mindset';

  constructor(private http: HttpClient) {}

  getRoadmaps(email: string): Observable<MindsetRoadmapSummary[]> {
    const params = new HttpParams().set('email', email);
    return this.http.get<MindsetRoadmapSummary[]>(`${this.baseUrl}/roadmaps`, { params });
  }

  getRoadmapDetail(email: string, roadmapKey: string): Observable<MindsetRoadmapDetail> {
    const params = new HttpParams().set('email', email);
    return this.http.get<MindsetRoadmapDetail>(`${this.baseUrl}/roadmaps/${roadmapKey}`, { params });
  }

  updateTask(request: TrainingTaskRequest): Observable<MindsetRoadmapDetail> {
    return this.http.post<MindsetRoadmapDetail>(`${this.baseUrl}/task`, request);
  }
}
