// src/app/workstyle-assessment/workstyle-assessment.component.ts

import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import {
  PersonalityAnswerItem,
  PersonalityAnswerRequest,
  PersonalityProfile,
  PersonalityQuestion,
  WorkstyleBaseline
} from '../models/personality.model';
import { PersonalityService } from '../services/personality.service';

@Component({
  selector: 'app-workstyle-assessment',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './workstyle-assessment.component.html',
styleUrls: ['./workstyle-assessment.component.scss']

})
export class WorkstyleAssessmentComponent implements OnInit {
  /** 1 = lühike vorm, 2 = AI vestlus, 3 = profiil */
  step = signal<1 | 2 | 3>(1);

  loading = signal(false);
  saving = signal(false);
  error = signal<string | null>(null);

  emailAndBaselineForm!: FormGroup;

  questions = signal<PersonalityQuestion[]>([]);
  currentIndex = signal(0);
  answers = signal<Record<string, PersonalityAnswerItem>>({});
  profile = signal<PersonalityProfile | null>(null);

  currentQuestion = computed(
    () => this.questions()[this.currentIndex()] ?? null
  );

  constructor(
    private fb: FormBuilder,
    private personalityService: PersonalityService
  ) {}

  ngOnInit(): void {
    this.emailAndBaselineForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      structurePreference: [50, [Validators.required]],
      speedPreference: [50, [Validators.required]],
      asyncPreference: [50, [Validators.required]],
      conflictDirectness: [50, [Validators.required]],
      autonomyNeed: [50, [Validators.required]],
      chaosTolerance: [50, [Validators.required]],
      notes: ['']
    });
  }

  // ---------- STEP 1: lühike vorm ----------

  goToInterview(): void {
    if (this.emailAndBaselineForm.invalid) {
      this.emailAndBaselineForm.markAllAsTouched();
      return;
    }

    const email = this.emailAndBaselineForm.value.email as string;
    this.loading.set(true);
    this.error.set(null);

    this.personalityService.startInterview(email).subscribe({
      next: (resp) => {
        this.questions.set(resp.questions);
        this.currentIndex.set(0);
        this.answers.set({});
        this.loading.set(false);
        this.step.set(2);
      },
      error: (err) => {
        console.error(err);
        this.error.set('Intervjuu käivitamine ebaõnnestus. Proovi uuesti.');
        this.loading.set(false);
      }
    });
  }

  // ---------- STEP 2: AI vestlus ----------

  setAnswerForCurrent(value: number): void {
    const q = this.currentQuestion();
    if (!q) return;

    const existing = this.answers()[q.id] ?? { questionId: q.id, value };
    const updated: PersonalityAnswerItem = { ...existing, value };
    this.answers.set({ ...this.answers(), [q.id]: updated });
  }

  setNoteForCurrent(note: string): void {
    const q = this.currentQuestion();
    if (!q) return;

    const existing = this.answers()[q.id] ?? { questionId: q.id, value: 3 };
    const updated: PersonalityAnswerItem = { ...existing, note };
    this.answers.set({ ...this.answers(), [q.id]: updated });
  }

  getCurrentValue(): number {
    const q = this.currentQuestion();
    if (!q) return 3;
    return this.answers()[q.id]?.value ?? 3;
  }

  getCurrentNote(): string {
    const q = this.currentQuestion();
    if (!q) return '';
    return this.answers()[q.id]?.note ?? '';
  }

  canGoNext(): boolean {
    const q = this.currentQuestion();
    if (!q) return false;
    return !!this.answers()[q.id];
  }

  prevQuestion(): void {
    if (this.currentIndex() > 0) {
      this.currentIndex.update((i) => i - 1);
    }
  }

  nextQuestion(): void {
    if (this.currentIndex() < this.questions().length - 1) {
      this.currentIndex.update((i) => i + 1);
    }
  }

  // ---------- STEP 2 -> 3: submit + profiil ----------

  private buildBaseline(): WorkstyleBaseline {
    const v = this.emailAndBaselineForm.value;
    return {
      structurePreference: v.structurePreference,
      speedPreference: v.speedPreference,
      asyncPreference: v.asyncPreference,
      conflictDirectness: v.conflictDirectness,
      autonomyNeed: v.autonomyNeed,
      chaosTolerance: v.chaosTolerance,
      notes: v.notes || undefined
    };
  }

  finishInterview(): void {
    const email = this.emailAndBaselineForm.value.email as string;
    const allQuestions = this.questions();

    if (!email || allQuestions.length === 0) return;

    const answerList: PersonalityAnswerItem[] = allQuestions
      .map((q) => this.answers()[q.id])
      .filter((a): a is PersonalityAnswerItem => !!a);

    if (answerList.length !== allQuestions.length) {
      this.error.set('Palun vasta kõigile küsimustele enne lõpetamist.');
      return;
    }

    const payload: PersonalityAnswerRequest = {
      email,
      answers: answerList,
      baseline: this.buildBaseline()
    };

    this.saving.set(true);
    this.error.set(null);

    this.personalityService.submitAnswers(payload).subscribe({
      next: (prof) => {
        this.profile.set(prof);
        this.saving.set(false);
        this.step.set(3);
      },
      error: (err) => {
        console.error(err);
        this.error.set('Vastuste salvestamine ebaõnnestus. Proovi uuesti.');
        this.saving.set(false);
      }
    });
  }

  restart(): void {
    this.step.set(1);
    this.profile.set(null);
    this.answers.set({});
    this.questions.set([]);
    this.currentIndex.set(0);
    this.error.set(null);
  }
}
