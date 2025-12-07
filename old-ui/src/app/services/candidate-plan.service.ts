import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Question } from '../models/question.model';

export interface PracticeBlock {
  title: string;
  items: string[];
}

export interface RoadmapStep {
  label: string;
  focus: string;
  details: string;
}

export interface CandidatePlanResponse {
  questions: Question[];
  practiceBlocks: PracticeBlock[];
  roadmap: RoadmapStep[];
}

@Injectable({
  providedIn: 'root'
})
export class CandidatePlanService {

  private readonly baseUrl = 'http://localhost:8080/api/candidate-plan';

  constructor(private http: HttpClient) {}

  generatePlan(cvText: string): Observable<CandidatePlanResponse> {
    return this.http.post<CandidatePlanResponse>(this.baseUrl, {
      cvText: cvText
    });
  }
}
