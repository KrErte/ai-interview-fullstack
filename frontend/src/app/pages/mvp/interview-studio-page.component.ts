import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { InterviewSessionApiService } from '../../core/services/interview-session-api.service';
import {
  InterviewAnswerRequest,
  InterviewDimensionScore,
  InterviewerStyle,
  InterviewNextQuestionResponse,
  InterviewQuestionResponse,
  InterviewSessionStartRequest,
  InterviewSummaryResponse,
  InterviewSeniority
} from '../../core/models/interview-session.model';
import {
  SoftSkillCatalogService,
  SoftSkillDimension
} from '../../core/services/soft-skill-catalog.service';

@Component({
  selector: 'app-interview-studio-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './interview-studio-page.component.html',
})
export class InterviewStudioPageComponent implements OnInit {
  companyName = '';
  roleTitle = '';
  seniority: InterviewSeniority = 'MID';
  interviewerStyle: InterviewerStyle = 'HR';

  loading = false;
  error = '';
  currentSessionId: string | null = null;
  totalQuestions: number | null = null;
  currentQuestionNumber: number | null = null;
  currentQuestion: InterviewQuestionResponse | null = null;
  isFinished = false;
  summary: InterviewSummaryResponse | null = null;
  currentAnswerText = '';
  answersHistory: { questionNumber: number; question: string; answer: string }[] = [];
  lastLocalAnalysis: { detectedStrengths: string[]; detectedRisks: string[] } | null = null;

  readonly seniorityOptions: InterviewSeniority[] = ['JUNIOR', 'MID', 'SENIOR'];
  readonly styleOptions: InterviewerStyle[] = ['HR', 'TECH', 'TEAM_LEAD', 'MIXED'];

  dimensionsMap: Record<string, SoftSkillDimension> = {};
  expandedDimensions = new Set<string>();

  constructor(
    private sessionApi: InterviewSessionApiService,
    private catalog: SoftSkillCatalogService
  ) {}

  ngOnInit(): void {
    this.loadDimensions();
  }

  runInterview(): void {
    if (!this.companyName.trim() || !this.roleTitle.trim()) {
      this.error = 'Please provide company name and role title.';
      return;
    }

    const payload: InterviewSessionStartRequest = {
      companyName: this.companyName.trim(),
      roleTitle: this.roleTitle.trim(),
      seniority: this.seniority,
      interviewerStyle: this.interviewerStyle
    };

    this.loading = true;
    this.error = '';
    this.resetSessionState(false);

    this.sessionApi.startSession(payload).subscribe({
      next: (res: InterviewQuestionResponse) => {
        this.currentSessionId = res.sessionId;
        this.totalQuestions = res.totalQuestions;
        this.currentQuestionNumber = res.questionNumber;
        this.currentQuestion = res;
        this.loading = false;
      },
      error: (err) => {
        this.error =
          err?.error?.message ||
          'Failed to start AI interview. Please try again in a moment.';
        this.loading = false;
      }
    });
  }

  submitAnswer(): void {
    if (!this.currentSessionId || !this.currentQuestion || this.currentQuestionNumber === null) {
      this.error = 'No active interview session. Start the interview first.';
      return;
    }
    if (!this.currentAnswerText.trim()) {
      this.error = 'Please write an answer before submitting.';
      return;
    }

    this.loading = true;
    this.error = '';

    const payload: InterviewAnswerRequest = {
      sessionId: this.currentSessionId,
      questionNumber: this.currentQuestionNumber!,
      answer: this.currentAnswerText.trim()
    };

    this.sessionApi.answer(payload).subscribe({
      next: (res: InterviewNextQuestionResponse | InterviewSummaryResponse) => {
        this.loading = false;
        this.answersHistory.push({
          questionNumber: this.currentQuestionNumber!,
          question: this.currentQuestion?.question || '',
          answer: this.currentAnswerText.trim()
        });
        this.currentAnswerText = '';
        this.lastLocalAnalysis = null;

        if ('isFinished' in res && res.isFinished) {
          this.isFinished = true;
          this.summary = res;
          this.currentQuestion = null;
          this.currentQuestionNumber = null;
          this.lastLocalAnalysis = null;
        } else {
          const next = res as InterviewNextQuestionResponse;
          this.isFinished = false;
          this.lastLocalAnalysis = next.localAnalysis || null;
          this.currentQuestion = {
            sessionId: next.sessionId,
            questionNumber: next.questionNumber,
            totalQuestions: next.totalQuestions,
            question: next.question,
            modelAnswerHint: next.modelAnswerHint
          };
          this.currentQuestionNumber = next.questionNumber;
          this.totalQuestions = next.totalQuestions;
          this.summary = null;
        }
      },
      error: (err) => {
        this.error =
          err?.error?.message ||
          'Could not submit the answer. Please try again.';
        this.loading = false;
      }
    });
  }

  dimensionWidth(score: number): number {
    if (score === null || score === undefined || Number.isNaN(score)) {
      return 0;
    }
    return Math.max(0, Math.min(score * 100, 100));
  }

  humanLabel(key: string): string {
    return this.dimensionsMap[key]?.label || key;
  }

  restart(): void {
    this.resetSessionState(true);
  }

  dimensionMeta(key: string): SoftSkillDimension | null {
    return this.dimensionsMap[key] || null;
  }

  toggleDim(key: string): void {
    if (this.expandedDimensions.has(key)) {
      this.expandedDimensions.delete(key);
    } else {
      this.expandedDimensions.add(key);
    }
    // trigger change detection by reassigning set reference
    this.expandedDimensions = new Set(this.expandedDimensions);
  }

  isExpanded(key: string): boolean {
    return this.expandedDimensions.has(key);
  }

  private loadDimensions(): void {
    this.catalog.getDimensions().subscribe({
      next: (dims) => {
        this.dimensionsMap = dims.reduce((acc, dim) => {
          acc[dim.key] = dim;
          return acc;
        }, {} as Record<string, SoftSkillDimension>);
      },
      error: () => {
        this.dimensionsMap = {};
      }
    });
  }

  private resetSessionState(resetForm: boolean): void {
    this.currentSessionId = null;
    this.totalQuestions = null;
    this.currentQuestionNumber = null;
    this.currentQuestion = null;
    this.isFinished = false;
    this.summary = null;
    this.answersHistory = [];
    this.currentAnswerText = '';
    this.lastLocalAnalysis = null;
    if (resetForm) {
      this.companyName = '';
      this.roleTitle = '';
      this.seniority = 'MID';
      this.interviewerStyle = 'HR';
    }
  }
}


