import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type SeniorityLevel = 'Junior' | 'Mid' | 'Senior';
export type InterviewStyle = 'HR' | 'TECH' | 'TEAM_LEAD' | 'MIXED';

export interface InterviewSimulationRequest {
  companyName: string;
  roleTitle: string;
  seniority: SeniorityLevel;
}

export interface InterviewQuestionAnswer {
  question: string;
  answer: string;
}

export interface InterviewSimulationResponse {
  sessionId?: string;
  questions: InterviewQuestionAnswer[];
}

export interface InterviewSessionStartRequest {
  companyName: string;
  roleTitle: string;
  seniority: SeniorityLevel;
  style: InterviewStyle;
}

export interface InterviewQuestion {
  id?: string;
  question: string;
  modelAnswerHint?: string;
}

export interface InterviewSessionStartResponse {
  sessionId: string;
  question: InterviewQuestion;
}

export interface InterviewSessionAnswerRequest {
  sessionId: string;
  answer: string;
}

export interface InterviewDimensionScore {
  dimension: string;
  score: number;
}

export interface InterviewSummary {
  overallFitPercent: number;
  strengths: string[];
  weaknesses: string[];
  dimensionScores: InterviewDimensionScore[];
}

export interface InterviewSessionAnswerResponse {
  isFinished: boolean;
  question?: InterviewQuestion;
  summary?: InterviewSummary;
}

export interface DejaVuPredictRequest {
  jobDescription: string;
}

export interface DejaVuPredictResponse {
  questions: string[];
}

export interface Story {
  id: string;
  title: string;
  situation: string;
  task: string;
  action: string;
  result: string;
  tag?: string;
  updatedAt?: string;
  createdAt?: string;
}

export interface StoryPayload {
  title: string;
  situation: string;
  task: string;
  action: string;
  result: string;
  tag?: string;
}

export interface CareerTwinEntry {
  id: string;
  text: string;
  createdAt: string;
}

export interface CareerTwinAppendRequest {
  text: string;
}

export interface CareerTwinAppendResponse {
  entry: CareerTwinEntry;
}

export interface CareerTwinInsightsResponse {
  insight: string;
}

@Injectable({
  providedIn: 'root',
})
export class MvpApiService {
  private readonly baseUrl = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  // Ghost Interviewer LITE
  simulateInterview(
    payload: InterviewSimulationRequest
  ): Observable<InterviewSimulationResponse> {
    return this.http.post<InterviewSimulationResponse>(
      `${this.baseUrl}/api/interview/simulate`,
      payload
    );
  }

  startInterviewSession(
    payload: InterviewSessionStartRequest
  ): Observable<InterviewSessionStartResponse> {
    return this.http.post<InterviewSessionStartResponse>(
      `${this.baseUrl}/api/interview/session/start`,
      payload
    );
  }

  submitInterviewAnswer(
    payload: InterviewSessionAnswerRequest
  ): Observable<InterviewSessionAnswerResponse> {
    return this.http.post<InterviewSessionAnswerResponse>(
      `${this.baseUrl}/api/interview/session/answer`,
      payload
    );
  }

  // Interview Déjà Vu LITE
  predictDejaVuQuestions(
    payload: DejaVuPredictRequest
  ): Observable<DejaVuPredictResponse> {
    return this.http.post<DejaVuPredictResponse>(
      `${this.baseUrl}/api/dejavu/predict`,
      payload
    );
  }

  // Story Forge LITE
  getStories(): Observable<Story[]> {
    return this.http.get<Story[]>(`${this.baseUrl}/api/stories`);
  }

  createStory(payload: StoryPayload): Observable<Story> {
    return this.http.post<Story>(`${this.baseUrl}/api/stories`, payload);
  }

  updateStory(id: string, payload: StoryPayload): Observable<Story> {
    return this.http.put<Story>(`${this.baseUrl}/api/stories/${id}`, payload);
  }

  deleteStory(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/stories/${id}`);
  }

  // Career Twin LITE
  appendCareerTwinEntry(
    text: string
  ): Observable<CareerTwinAppendResponse> {
    const body: CareerTwinAppendRequest = { text };
    return this.http.post<CareerTwinAppendResponse>(
      `${this.baseUrl}/api/career-twin/append`,
      body
    );
  }

  getCareerTwinEntries(limit = 5): Observable<CareerTwinEntry[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<CareerTwinEntry[]>(
      `${this.baseUrl}/api/career-twin/entries`,
      { params }
    );
  }

  getCareerTwinInsights(): Observable<CareerTwinInsightsResponse> {
    return this.http.get<CareerTwinInsightsResponse>(
      `${this.baseUrl}/api/career-twin/insights`
    );
  }
}


