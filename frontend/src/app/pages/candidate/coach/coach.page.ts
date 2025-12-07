import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';
import {
  CoachService,
  HistoryTurn,
  MindsetRoadmapSummary,
  SoftSkillQuestionRequest
} from '../../../services/coach.service';

type SessionStatus = 'Planned' | 'In progress' | 'Completed';
type FeedbackType = 'strength' | 'issue' | 'hint';

interface TrainingSession {
  id: string;
  title: string;
  skill: string;
  difficulty: 'Easy' | 'Medium' | 'Hard';
  estimatedMinutes: number;
  status: SessionStatus;
  role: string;
  level: string;
  prompt: string;
}

interface ActiveSession {
  session: TrainingSession;
  questionText: string;
  score: number | null;
  coachFeedback?: string | null;
  weakestSkill?: string | null;
}

interface CoachFeedbackMessage {
  id: string;
  type: FeedbackType;
  text: string;
}

@Component({
  selector: 'app-coach-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './coach.page.html',
  styleUrls: ['./coach.page.scss']
})
export class CoachPageComponent implements OnInit {
  sessions: TrainingSession[] = [];
  feedback: CoachFeedbackMessage[] = [];
  activeSession: ActiveSession | null = null;

  answerText = '';
  answerError: string | null = null;
  nextRecommendation = 'Pick a focus area to start training.';

  loadingSessions = false;
  loadingQuestion = false;
  submittingAnswer = false;
  loadError: string | null = null;

  private historyBySession = new Map<string, HistoryTurn[]>();
  private email: string | null = null;

  constructor(
    private readonly coachService: CoachService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.email = this.authService.getCurrentUserEmail();
    if (!this.email) {
      this.loadError = 'Please log in again to load coaching sessions.';
      return;
    }
    this.loadSessions();
  }

  get scoreText(): string {
    const score = this.activeSession?.score;
    if (score == null) {
      return 'Not scored yet';
    }
    const isHundredScale = score > 10;
    const display = isHundredScale ? Math.round(score) : score.toFixed(1);
    const suffix = isHundredScale ? '/ 100' : '/ 10';
    return `Session score: ${display} ${suffix}`;
  }

  get scorePercent(): number {
    const score = this.activeSession?.score;
    if (score == null) {
      return 0;
    }
    return Math.max(0, Math.min(100, score > 10 ? score : score * 10));
  }

  startSession(session: TrainingSession): void {
    this.answerText = '';
    this.answerError = null;
    this.activeSession = null;
    this.requestNextQuestion(session);
    this.sessions = this.sessions.map((s) =>
      s.id === session.id ? { ...s, status: 'In progress' } : s
    );
  }

  startFirstSession(): void {
    if (this.sessions.length) {
      this.startSession(this.sessions[0]);
    }
  }

  submitAnswer(): void {
    if (!this.activeSession) {
      this.answerError = 'Start a session first.';
      return;
    }
    if (!this.answerText.trim()) {
      this.answerError = 'Please add an answer before submitting.';
      return;
    }

    this.answerError = null;
    this.requestNextQuestion(
      this.activeSession.session,
      this.activeSession.questionText,
      this.answerText.trim()
    );
  }

  skipSession(): void {
    if (!this.sessions.length) {
      return;
    }
    if (!this.activeSession) {
      this.startFirstSession();
      return;
    }
    const currentIndex = this.sessions.findIndex((s) => s.id === this.activeSession?.session.id);
    const nextIndex = (currentIndex + 1) % this.sessions.length;
    this.startSession(this.sessions[nextIndex]);
  }

  private loadSessions(): void {
    if (!this.email) {
      return;
    }
    this.loadingSessions = true;
    this.loadError = null;

    this.coachService.getMindsetRoadmaps(this.email).subscribe({
      next: (roadmaps) => {
        this.sessions = roadmaps.map((roadmap) => this.mapRoadmapToSession(roadmap));
        this.loadingSessions = false;
        if (!this.activeSession && this.sessions.length) {
          this.startSession(this.sessions[0]);
        }
      },
      error: () => {
        this.loadError = 'Could not load your training plan. Showing local fallback.';
        this.sessions = this.sessions.length ? this.sessions : this.buildFallbackSessions();
        this.loadingSessions = false;
      }
    });
  }

  private mapRoadmapToSession(summary: MindsetRoadmapSummary): TrainingSession {
    const status: SessionStatus =
      summary.totalTasks > 0 && summary.completedTasks >= summary.totalTasks
        ? 'Completed'
        : summary.completedTasks > 0
          ? 'In progress'
          : 'Planned';

    const difficulty: 'Easy' | 'Medium' | 'Hard' =
      summary.progressPercent >= 75 ? 'Hard' : summary.progressPercent >= 30 ? 'Medium' : 'Easy';

    return {
      id: summary.roadmapKey,
      title: summary.title,
      skill: summary.roadmapKey,
      difficulty,
      estimatedMinutes: Math.max(10, Math.min(45, (summary.totalTasks || 3) * 5)),
      status,
      role: 'Soft-skill focus area',
      level: `${summary.completedTasks}/${summary.totalTasks} tasks`,
      prompt: 'Start to get your next question.'
    };
  }

  private requestNextQuestion(
    session: TrainingSession,
    previousQuestion?: string,
    previousAnswer?: string
  ): void {
    if (!this.email) {
      this.answerError = 'Please log in to continue.';
      return;
    }

    const history = this.historyBySession.get(session.id) ?? [];
    const payload: SoftSkillQuestionRequest = {
      email: this.email,
      roadmapKey: session.id,
      previousQuestion,
      previousAnswer,
      history
    };

    const isAnswering = !!previousQuestion && !!previousAnswer;
    this.loadingQuestion = !isAnswering && !this.activeSession;
    this.submittingAnswer = isAnswering;
    this.answerError = null;

    this.coachService.askSoftQuestion(payload).subscribe({
      next: (res) => {
        if (previousQuestion && previousAnswer) {
          const updatedHistory: HistoryTurn[] = [...history, { question: previousQuestion, answer: previousAnswer }];
          this.historyBySession.set(session.id, updatedHistory);
          this.addFeedback(res.coachFeedback, res.score);
        }

        this.activeSession = {
          session,
          questionText: res.questionText || session.prompt,
          score: res.score ?? null,
          coachFeedback: res.coachFeedback ?? null,
          weakestSkill: res.weakestSkill ?? null
        };

        this.nextRecommendation = res.weakestSkill
          ? `Focus next on ${res.weakestSkill.replace(/_/g, ' ')}`
          : res.coachFeedback || 'Keep practicing to unlock more feedback.';
      },
      error: () => {
        this.answerError = 'Could not get the next question. Please try again.';
      },
      complete: () => {
        this.loadingQuestion = false;
        this.submittingAnswer = false;
        this.answerText = '';
      }
    });
  }

  private addFeedback(text?: string | null, score?: number | null): void {
    if (!text && score == null) {
      return;
    }

    const type: FeedbackType =
      score != null ? (score >= 70 ? 'strength' : 'issue') : 'hint';

    const newMessage: CoachFeedbackMessage = {
      id: `f-${Date.now()}`,
      type,
      text: text || 'Answer recorded. Keep going!'
    };

    this.feedback = [newMessage, ...this.feedback].slice(0, 5);
  }

  private buildFallbackSessions(): TrainingSession[] {
    return [
      {
        id: 'conflict_management',
        title: 'Conflict management',
        skill: 'Conflict management',
        difficulty: 'Medium',
        estimatedMinutes: 20,
        status: 'Planned',
        role: 'Soft skills',
        level: '0/3 tasks',
        prompt: 'Kirjelda olukorda, kus lahendasid tiimikonflikti.'
      },
      {
        id: 'clear_communication',
        title: 'Clear communication',
        skill: 'Communication',
        difficulty: 'Easy',
        estimatedMinutes: 15,
        status: 'Planned',
        role: 'Soft skills',
        level: '0/3 tasks',
        prompt: 'Selgita keeruline tehniline teema mitte-tehnilisele kolleegile.'
      }
    ];
  }
}

