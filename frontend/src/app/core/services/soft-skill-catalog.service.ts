import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SoftSkillDimension {
  key: string;
  label: string;
  definition: string;
  highSignals: string[];
  lowSignals: string[];
  interviewSignals: string[];
  coachingIdeas: string[];
}

@Injectable({ providedIn: 'root' })
export class SoftSkillCatalogService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/soft-skills/dimensions`;

  constructor(private http: HttpClient) {}

  getDimensions(): Observable<SoftSkillDimension[]> {
    return this.http.get<SoftSkillDimension[]>(this.baseUrl).pipe(map(list => list || []));
  }

  getDimension(key: string): Observable<SoftSkillDimension> {
    return this.http.get<SoftSkillDimension>(`${this.baseUrl}/${encodeURIComponent(key)}`);
  }
}


