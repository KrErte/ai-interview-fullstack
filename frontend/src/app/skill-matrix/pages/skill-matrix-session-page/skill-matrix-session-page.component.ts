import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { SkillMatrixApiService } from '../../services/skill-matrix-api.service';
import {
  InterviewSession,
  SubmitAnswerRequest,
} from '../../models/interview.model';

@Component({
  selector: 'app-skill-matrix-session-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './skill-matrix-session-page.component.html',
  styleUrls: ['./skill-matrix-session-page.component.scss'],
})
export class SkillMatrixSessionPageComponent implements OnInit, OnDestroy {
  sessionId = '';
  session?: InterviewSession;

  answerText = '';
  loadingSession = false;
  submitting = false;
  error = '';

  private sub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private api: SkillMatrixApiService
  ) {}

  ngOnInit(): void {
    this.sub = this.route.paramMap.subscribe(params => {
      const id = params.get('sessionId');
      if (id) {
        this.sessionId = id;
        this.loadSession();
      }
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  loadSession() {
    if (!this.sessionId) {
      return;
    }
    this.loadingSession = true;
    this.error = '';

    this.api.getSession(this.sessionId).subscribe({
      next: session => {
        this.session = session;
        this.loadingSession = false;
      },
      error: err => {
        this.loadingSession = false;
        this.error =
          err?.error?.message ||
          'Failed to load interview session. Please refresh and try again.';
      },
    });
  }

  submitAnswer() {
    if (!this.answerText.trim()) {
      this.error = 'Please write an answer before submitting.';
      return;
    }

    const payload: SubmitAnswerRequest = {
      sessionId: this.sessionId,
      answer: this.answerText.trim(),
    };

    this.submitting = true;
    this.error = '';

    this.api.submitAnswer(payload).subscribe({
      next: () => {
        this.answerText = '';
        this.submitting = false;
        this.loadSession();
      },
      error: err => {
        this.submitting = false;
        this.error =
          err?.error?.message ||
          'Failed to submit answer. Please try again in a moment.';
      },
    });
  }

  get currentPhaseLabel(): string {
    if (!this.session?.currentPhase) {
      return 'Interview in progress';
    }
    return `${this.session.currentPhase.name} · ${this.session.currentPhase.interviewerType}`;
  }
}


