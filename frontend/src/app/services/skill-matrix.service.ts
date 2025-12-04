import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { SkillMatrixResponse } from '../models/skill-matrix.model';

@Injectable({ providedIn: 'root' })
export class SkillMatrixService {
  private readonly apiBaseUrl = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  getSkillMatrix(email: string): Observable<SkillMatrixResponse> {
    return this.http.get<SkillMatrixResponse>(`${this.apiBaseUrl}/skills/matrix?email=${email}`);
  }
}
