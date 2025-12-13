import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { InterviewSessionApiService } from '../../core/services/interview-session-api.service';
import {
  InterviewProgressResponse,
  InterviewSessionCreateRequest,
  InterviewSessionCreateResponse,
  InterviewSummaryResponse,
  InterviewSeniority,
  InterviewerStyle,
  InterviewFitSnapshot,
  InterviewFitBreakdown
} from '../../core/models/interview-session.model';
import {
  SoftSkillCatalogService,
  SoftSkillDimension
} from '../../core/services/soft-skill-catalog.service';
import {
  InterviewDebugStateService,
  InterviewDebugSessionInfo
} from '../../core/services/interview-debug-state.service';
import { InterviewControlRoomDrawerComponent } from './interview-control-room-drawer.component';
import { FitIndicatorComponent } from '../../shared/fit-indicator/fit-indicator.component';
import { FitExplainPanelComponent } from '../../shared/fit-explain-panel/fit-explain-panel.component';
import { ObserverLogPanelComponent } from '../../shared/observer-log/observer-log-panel.component';

interface InterviewTranscriptEntry {
  questionNumber: number;
  question: string;
  answer: string;
  decision?: string;
}

interface InterviewStudioVm {
  currentQuestion: string | null;
  decision: string | null;
  progress: any | null;
  questionCount: number | null;
  fitScore: number | null;
  fitTrend: string | null;
  sessionComplete: boolean;
  transcript: InterviewTranscriptEntry[];
  fit: InterviewFitSnapshot | null;
  fitBreakdown: InterviewFitBreakdown | null;
}

@Component({
  selector: 'app-interview-studio-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    InterviewControlRoomDrawerComponent,
    FitIndicatorComponent,
    FitExplainPanelComponent,
    ObserverLogPanelComponent
  ],
  templateUrl: './interview-studio-page.component.html'
})
export class InterviewStudioPageComponent implements OnInit {
  companyName = '';
  roleTitle = '';
  candidateEmail = '';
  seniority: InterviewSeniority = 'MID';
  interviewerStyle: InterviewerStyle = 'HR';

  private readonly uuidPattern =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

  loading = false;
  isSubmitting = false;
  error = '';

  currentSessionId: string | null = null;
  displaySessionId: string | null = null;
  totalQuestions: number | null = null;
  currentQuestionNumber: number | null = null;
  currentQuestion: { question: string; modelAnswerHint?: string } | null = null;

  summary: InterviewSummaryResponse | null = null;
  currentAnswerText = '';
  lastLocalAnalysis: { detectedStrengths: string[]; detectedRisks: string[] } | null = null;

  isFitExplainOpen = false;

  vm: InterviewStudioVm = {
    currentQuestion: null,
    decision: null,
    progress: null,
    questionCount: null,
    fitScore: null,
    fitTrend: null,
    sessionComplete: false,
    transcript: [],
    fit: null,
    fitBreakdown: null
  };

  readonly seniorityOptions: InterviewSeniority[] = ['JUNIOR', 'MID', 'SENIOR'];
  readonly styleOptions: InterviewerStyle[] = ['HR', 'TECH', 'TEAM_LEAD', 'MIXED'];

  dimensionsMap: Record<string, SoftSkillDimension> = {};
  expandedDimensions = new Set<string>();

  constructor(
    private sessionApi: InterviewSessionApiService,
    private catalog: SoftSkillCatalogService,
    private debugState: InterviewDebugStateService
  ) {}

  ngOnInit(): void {
    this.loadDimensions();
  }

  /* =========================
     REQUIRED BY TEMPLATE
     ========================= */

  toggleControlRoom(): void {
    this.debugState.toggleDrawer();
  }

  openFitExplain(): void {
    this.isFitExplainOpen = true;
  }

  closeFitExplain(): void {
    this.isFitExplainOpen = false;
  }

  /* ========================= */

  runInterview(): void {
    if (!this.candidateEmail.trim()) {
      this.error = 'Please provide your email.';
      return;
    }

    this.loading = true;
    this.error = '';
    this.resetSessionState(false);

    const payload: InterviewSessionCreateRequest = {
      email: this.candidateEmail.trim()
    };

    this.sessionApi.createSession(payload).subscribe({
      next: (res: InterviewSessionCreateResponse) => {
        const sessionUuid = res.sessionUuid;
        if (!sessionUuid || !this.uuidPattern.test(sessionUuid)) {
          this.error = 'Invalid session id from backend.';
          this.loading = false;
          return;
        }

        this.currentSessionId = sessionUuid;
        this.displaySessionId = String(res.sessionId ?? sessionUuid);
        this.currentQuestionNumber = 1;

        const introQuestion = `Let's start with a quick intro. Tell me about your background and why you’re interested in the "${this.roleTitle}" role at ${this.companyName}.`;

        this.currentQuestion = {
          question: introQuestion,
          modelAnswerHint:
            'Give a concise story: 2–3 key experiences, your strengths, and why this role/company fits you.'
        };

        this.vm = {
          ...this.vm,
          currentQuestion: introQuestion,
          questionCount: 1,
          transcript: []
        };

        const debugInfo: InterviewDebugSessionInfo = {
          sessionId: this.displaySessionId,
          sessionUuid,
          email: this.candidateEmail.trim()
        };
        this.debugState.setSessionInfo(debugInfo);

        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to start interview.';
        this.loading = false;
      }
    });
  }

  submitAnswer(): void {
    if (!this.currentSessionId || !this.currentAnswerText.trim()) {
      return;
    }

    this.isSubmitting = true;
    this.loading = true;

    this.sessionApi
      .nextQuestion(this.currentSessionId, this.currentAnswerText.trim())
      .subscribe({
        next: (res: InterviewProgressResponse) => {
          const prevQ = this.vm.currentQuestion ?? '';
          const prevN = this.vm.questionCount ?? 1;

          this.vm.transcript.push({
            questionNumber: prevN,
            question: prevQ,
            answer: this.currentAnswerText.trim(),
            decision: res.decision ?? undefined
          });

          this.vm = {
            ...this.vm,
            currentQuestion: res.question,
            decision: res.decision ?? null,
            progress: res.progress ?? null,
            questionCount: res.progress?.questionCount ?? prevN + 1,
            fitScore: res.fitScore ?? null,
            fitTrend: res.fitTrend ?? null,
            sessionComplete: !!res.sessionComplete,
            fit: res.fit ?? null,
            fitBreakdown: res.fitBreakdown ?? null
          };

          this.currentQuestion = { question: res.question };
          this.currentQuestionNumber = this.vm.questionCount;
          this.currentAnswerText = '';
          this.loading = false;
          this.isSubmitting = false;
        },
        error: () => {
          this.error = 'Failed to submit answer.';
          this.loading = false;
          this.isSubmitting = false;
        }
      });
  }

  restart(): void {
    this.resetSessionState(true);
  }

  hasValidSessionId(): boolean {
    return !!this.currentSessionId && this.uuidPattern.test(this.currentSessionId);
  }

  dimensionWidth(score: number): number {
    return Math.max(0, Math.min((score ?? 0) * 100, 100));
  }

  humanLabel(key: string): string {
    return this.dimensionsMap[key]?.label ?? key;
  }

  dimensionMeta(key: string): SoftSkillDimension | null {
    return this.dimensionsMap[key] ?? null;
  }

  toggleDim(key: string): void {
    this.expandedDimensions.has(key)
      ? this.expandedDimensions.delete(key)
      : this.expandedDimensions.add(key);
    this.expandedDimensions = new Set(this.expandedDimensions);
  }

  isExpanded(key: string): boolean {
    return this.expandedDimensions.has(key);
  }

  private loadDimensions(): void {
    this.catalog.getDimensions().subscribe({
      next: dims => {
        this.dimensionsMap = dims.reduce((acc, d) => {
          acc[d.key] = d;
          return acc;
        }, {} as Record<string, SoftSkillDimension>);
      },
      error: () => (this.dimensionsMap = {})
    });
  }

  private resetSessionState(resetForm: boolean): void {
    this.currentSessionId = null;
    this.displaySessionId = null;
    this.currentQuestion = null;
    this.currentQuestionNumber = null;
    this.currentAnswerText = '';
    this.summary = null;
    this.vm = {
      currentQuestion: null,
      decision: null,
      progress: null,
      questionCount: null,
      fitScore: null,
      fitTrend: null,
      sessionComplete: false,
      transcript: [],
      fit: null,
      fitBreakdown: null
    };

    if (resetForm) {
      this.companyName = '';
      this.roleTitle = '';
      this.candidateEmail = '';
      this.seniority = 'MID';
      this.interviewerStyle = 'HR';
    }
  }
}
