import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export type SoftSkillSourceType = 'HR' | 'TECH' | 'TEAM_LEAD' | 'SELF' | 'OTHER';

export interface SoftSkillSourceDto {
  sourceType: SoftSkillSourceType | string;
  label?: string | null;
  content: string;
}

export interface SoftSkillMergedProfileDto {
  summary: string;
  strengths: string[];
  risks: string[];
  communicationStyle: string;
  collaborationStyle: string;
  growthAreas: string[];
}

export interface SoftSkillMergeResponseDto {
  mergedProfile: SoftSkillMergedProfileDto;
  saved: boolean;
  savedProfileId?: string | null;
  createdAt: string;
}

export interface SoftSkillMergeRequestDto {
  email: string;
  sources: SoftSkillSourceDto[];
  saveMerged: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class SoftSkillMergerApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/soft-skills`;

  constructor(private readonly http: HttpClient) {}

  mergeSoftSkills(
    request: SoftSkillMergeRequestDto
  ): Observable<SoftSkillMergeResponseDto> {
    return this.http.post<SoftSkillMergeResponseDto>(
      `${this.baseUrl}/merge`,
      request
    );
  }
}


