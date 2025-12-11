import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SoftSkillDimensionDto {
  key: string;
  label: string;
  definition: string;
  highSignals: string[];
  lowSignals: string[];
  interviewSignals: string[];
  coachingIdeas: string[];
}

@Injectable({
  providedIn: 'root'
})
export class SoftSkillApiService {
  private readonly baseUrl = '/api/soft-skill';

  constructor(private readonly http: HttpClient) {}

  getDimensions(): Observable<SoftSkillDimensionDto[]> {
    return this.http.get<SoftSkillDimensionDto[]>(`${this.baseUrl}/dimensions`);
  }
}


