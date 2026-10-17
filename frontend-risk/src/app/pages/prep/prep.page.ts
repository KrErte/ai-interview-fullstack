import { Component, DestroyRef, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AiReadinessService } from '../../services/ai-readiness.service';
import { AnalyticsService } from '../../services/analytics.service';
import { AssessmentResult, InterviewQuestion, MockEvaluation } from '../../models/ai-readiness.model';
import { MockInterviewStatusService, MockProvider } from '../../services/mock-interview-status.service';
import { HttpErrorResponse } from '@angular/common/http';
import { ProofStory, answerFor, emptyStory, isComplete, notAiSuggestion, proofTasks } from './prep-answers';

const DRAFTS_KEY = 'ai_prep_drafts';

interface MockDraft {
  answers: string[];
  results: (MockEvaluation | null)[];
}

interface Draft {
  stories: ProofStory[];
  notAi: string;
  mock: MockDraft;
}

const QUESTION_COUNT = 6;
const LEVEL_COLORS = ['#ff4d6d', '#ffd23f', '#3ddc97', '#c6f432'];

function emptyMock(): MockDraft {
  return { answers: Array(QUESTION_COUNT).fill(''), results: Array(QUESTION_COUNT).fill(null) };
}

/**
 * AI interview prep, free beta (docs/vajadus-ai-ajastul-2026-10-17.md): the likely AI fluency questions for the
 * user's profession, answers built from their own examples and numbers, and a printable AI work portfolio.
 * Works from the saved test result; drafts stay in this browser only.
 */
@Component({
  selector: 'app-prep-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section *ngIf="!result" class="mx-auto max-w-2xl py-16" data-test="prep-no-result">
      <p class="kicker !text-accent">AI interview prep · free beta</p>
      <h1 class="display mt-4 text-[clamp(2.2rem,5vw,3.5rem)]">Take the free test first</h1>
      <p class="mt-5 text-[17px] leading-relaxed text-muted">
        The prep is built from your own tasks, so it needs your test result. It takes about three minutes and no account.
      </p>
      <a [routerLink]="['/ai-test']" [queryParams]="professionParam ? { profession: professionParam } : {}" class="btn-accent mt-8">
        Take the test <span class="arrow" aria-hidden="true">&rarr;</span>
      </a>
    </section>

    <article *ngIf="result as r" class="space-y-6">
      <header class="print:hidden">
        <p class="kicker !text-accent">AI interview prep · free beta</p>
        <h1 class="display mt-4 text-[clamp(2.2rem,5vw,3.5rem)]">Walk into the interview with real answers</h1>
        <p class="mt-4 max-w-3xl text-[17px] leading-relaxed text-muted">
          For <span class="font-semibold text-fg">{{ r.professionName }}</span>. More and more employers ask how you use AI,
          in job interviews and in performance reviews. Build your answers from your own work, with your own numbers.
          Free while in beta; later part of a &euro;29 one-off pass.
        </p>
        <p class="mt-2 text-[13px] text-faint">
          Not {{ r.professionName }}? <a routerLink="/ai-test" class="link">Take the test for your job</a>.
        </p>
      </header>

      <!-- 1. Questions -->
      <section class="card p-6 sm:p-8 print:hidden">
        <p class="font-mono text-sm text-accent">01</p>
        <h2 class="mt-3 text-3xl font-bold leading-tight tracking-tight">Questions you are likely to get</h2>
        <p class="mt-3 text-[15px] leading-relaxed text-muted">Typical AI fluency questions, based on public hiring rubrics, not the questions of one employer.</p>
        <p *ngIf="questionsError" class="mt-4 text-[14px] text-band-very-high" role="alert">Could not load the questions. Reload the page to try again.</p>
        <ol class="mt-5 space-y-4" data-test="prep-questions">
          <li *ngFor="let q of questions; let i = index" class="rounded-xl bg-surface-2 p-5">
            <p class="text-[17px] font-semibold leading-snug">{{ i + 1 }}. {{ q.question }}</p>
            <p class="mt-2 text-[14px] leading-relaxed text-muted"><span class="font-semibold text-fg">What they look for:</span> {{ q.lookFor }}</p>
            <p class="mt-1 text-[14px] leading-relaxed text-muted"><span class="font-semibold text-fg">A strong answer:</span> {{ q.strongAnswer }}</p>
          </li>
        </ol>
      </section>

      <!-- 2. Proof stories -->
      <section class="card p-6 sm:p-8 print:hidden">
        <p class="font-mono text-sm text-accent">02</p>
        <h2 class="mt-3 text-3xl font-bold leading-tight tracking-tight">Your answers, from your own work</h2>
        <p class="mt-3 text-[15px] leading-relaxed text-muted">
          Pick a task you have actually done with AI. Say which tool, how long it took before and now, and what you check.
          We turn it into an answer for "How do you use AI in your work?". Leave out tasks you have not tried yet.
        </p>
        <div *ngFor="let s of draft.stories; let i = index" class="mt-6 rounded-xl border border-line p-5" [attr.data-test]="'prep-story-' + i">
          <p class="kicker">Task {{ i + 1 }}</p>
          <p class="mt-1 text-[17px] font-semibold leading-snug">{{ s.task }}</p>
          <div class="mt-4 grid gap-4 sm:grid-cols-2">
            <label class="block sm:col-span-2">
              <span class="kicker">AI tool you used</span>
              <input [name]="'tool' + i" [attr.data-test]="'prep-tool-' + i" [(ngModel)]="s.tool" (ngModelChange)="changed()" [attr.list]="'tools-' + i" maxlength="80"
                     class="field mt-2" placeholder="e.g. {{ r.aiTools[0] || 'ChatGPT' }}" />
              <datalist [id]="'tools-' + i"><option *ngFor="let t of r.aiTools" [value]="t"></option></datalist>
            </label>
            <label class="block">
              <span class="kicker">Minutes it took before</span>
              <input [name]="'before' + i" [attr.data-test]="'prep-before-' + i" type="number" min="0" max="100000" [(ngModel)]="s.before" (ngModelChange)="changed()" class="field mt-2" />
            </label>
            <label class="block">
              <span class="kicker">Minutes it takes now</span>
              <input [name]="'after' + i" [attr.data-test]="'prep-after-' + i" type="number" min="0" max="100000" [(ngModel)]="s.after" (ngModelChange)="changed()" class="field mt-2" />
            </label>
            <label class="block sm:col-span-2">
              <span class="kicker">What do you check before you use the result?</span>
              <input [name]="'check' + i" [attr.data-test]="'prep-check-' + i" [(ngModel)]="s.check" (ngModelChange)="changed()" maxlength="200" class="field mt-2"
                     placeholder="e.g. I compare the totals with the source" />
            </label>
            <label class="block sm:col-span-2">
              <span class="kicker">What came of it? (optional)</span>
              <input [name]="'outcome' + i" [attr.data-test]="'prep-outcome-' + i" [(ngModel)]="s.outcome" (ngModelChange)="changed()" maxlength="200" class="field mt-2"
                     placeholder="e.g. the report goes out a day earlier" />
            </label>
          </div>
          <div class="mt-4 rounded-xl bg-surface-2 p-4" aria-live="polite">
            <p class="kicker">Your answer</p>
            <p *ngIf="answer(r, s) as a; else incomplete" class="mt-2 text-[15px] leading-relaxed" [attr.data-test]="'prep-answer-' + i">{{ a }}</p>
            <ng-template #incomplete><p class="mt-2 text-[14px] text-faint">Fill in the tool, both times and what you check.</p></ng-template>
          </div>
        </div>

        <div class="mt-6 rounded-xl border border-line p-5">
          <p class="kicker">"Where would you not use AI?"</p>
          <textarea name="notAi" [(ngModel)]="draft.notAi" (ngModelChange)="changed()" rows="4" maxlength="600"
                    class="field mt-2" data-test="prep-not-ai"></textarea>
          <p class="mt-2 text-[13px] text-faint">A starting point from your least automatable task. Make it yours.</p>
        </div>
      </section>

      <!-- 3. Mock interview -->
      <section class="card p-6 sm:p-8 print:hidden" data-test="prep-mock">
        <p class="font-mono text-sm text-accent">03</p>
        <h2 class="mt-3 text-3xl font-bold leading-tight tracking-tight">Mock interview</h2>
        <p class="mt-3 text-[15px] leading-relaxed text-muted">
          Answer a question the way you would say it in the interview. An AI interviewer scores it on the four levels of
          public AI fluency rubrics (Unacceptable, Capable, Adoptive, Transformative), tells you what is missing and
          rewrites it using only your own facts.
        </p>
        <p *ngIf="mockAvailable === false" class="mt-4 rounded-xl bg-surface-2 p-4 text-[15px]" data-test="mock-off">
          The mock interview is not switched on yet. Your questions and answers above already work.
        </p>
        <ng-container *ngIf="mockAvailable">
          <div class="mt-5 flex flex-wrap gap-2" role="tablist" aria-label="Interview questions">
            <button *ngFor="let q of questions; let i = index" type="button" role="tab" class="chip"
                    [class.is-active]="mockIndex === i" [attr.aria-selected]="mockIndex === i" (click)="selectMock(i)"
                    [attr.data-test]="'mock-q-' + i">
              Q{{ i + 1 }}<span *ngIf="draft.mock.results[i] as e" class="num ml-1 opacity-70">· {{ e.score }}/4</span>
            </button>
          </div>
          <p class="mt-4 text-[17px] font-semibold leading-snug">{{ mockQuestion() }}</p>
          <textarea name="mockAnswer" [(ngModel)]="draft.mock.answers[mockIndex]" (ngModelChange)="changed()" rows="6"
                    maxlength="2000" class="field mt-3" data-test="mock-answer"
                    placeholder="Say what you would say: the tool, a real task, the result, what you check."></textarea>
          <p class="mt-2 text-[12px] leading-relaxed text-faint">
            {{ (draft.mock.answers[mockIndex] || '').length }}/2000 · Your answer is sent to {{ mockProviderLabel }} to be scored
            and we do not store it. Leave out names and confidential details.
          </p>
          <button type="button" class="btn-accent mt-4" (click)="score(r)" data-test="mock-score"
                  [disabled]="scoring || (draft.mock.answers[mockIndex] || '').trim().length < 20">
            {{ scoring ? 'Scoring…' : 'Score my answer' }}
          </button>
          <p *ngIf="mockError" class="mt-3 text-[14px] text-band-very-high" role="alert">{{ mockError }}</p>

          <div *ngIf="draft.mock.results[mockIndex] as e" class="mt-6 space-y-4" aria-live="polite" data-test="mock-result">
            <p class="text-[20px] font-bold" [style.color]="levelColor(e.score)">Level {{ e.score }}/4: {{ e.level }}</p>
            <div *ngIf="e.strengths.length">
              <p class="kicker">What works</p>
              <ul class="mt-2 space-y-1 text-[15px] leading-relaxed">
                <li *ngFor="let s of e.strengths" class="flex gap-3"><span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-accent"></span><span>{{ s }}</span></li>
              </ul>
            </div>
            <div *ngIf="e.missing.length">
              <p class="kicker">To make it stronger</p>
              <ul class="mt-2 space-y-1 text-[15px] leading-relaxed">
                <li *ngFor="let s of e.missing" class="flex gap-3"><span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-band-high"></span><span>{{ s }}</span></li>
              </ul>
            </div>
            <div class="rounded-xl bg-surface-2 p-4">
              <p class="kicker">A stronger version, with your facts</p>
              <p class="mt-2 text-[15px] leading-relaxed" data-test="mock-improved">{{ e.improvedAnswer }}</p>
              <p class="mt-2 text-[12px] text-faint">Fill in anything in [brackets] with your real numbers. Check it says only what is true.</p>
              <button type="button" class="link mt-3 text-[14px]" (click)="useImproved(e)" data-test="mock-use">Use this as my answer</button>
            </div>
          </div>
        </ng-container>
      </section>

      <!-- 4. Portfolio -->
      <section class="card avoid-break p-6 sm:p-8" data-test="prep-portfolio">
        <p class="font-mono text-sm text-accent print:hidden">04</p>
        <h2 class="mt-3 text-3xl font-bold leading-tight tracking-tight">My AI work: {{ r.professionName }}</h2>
        <p class="mt-3 text-[15px] leading-relaxed text-muted print:hidden">
          Your answers on one page. Print it or save it as PDF for the interview, your application or your review.
        </p>
        <p *ngIf="!completeCount()" class="mt-4 text-[15px] text-faint print:hidden">Fill in at least one task above and your answers appear here.</p>
        <ol *ngIf="completeCount()" class="mt-5 space-y-4">
          <ng-container *ngFor="let s of draft.stories">
            <li *ngIf="answer(r, s) as a">
              <p class="text-[15px] font-semibold">{{ s.task }}</p>
              <p class="mt-1 text-[15px] leading-relaxed">{{ a }}</p>
            </li>
          </ng-container>
        </ol>
        <div *ngIf="completeCount() && draft.notAi.trim()" class="mt-5">
          <p class="text-[15px] font-semibold">Where I don't use AI</p>
          <p class="mt-1 text-[15px] leading-relaxed">{{ draft.notAi }}</p>
        </div>
        <div class="mt-6 flex flex-wrap gap-3 print:hidden">
          <button type="button" class="btn-accent" [disabled]="!completeCount()" (click)="print()" data-test="prep-print">Print or save as PDF</button>
          <button type="button" class="btn-ghost" [disabled]="!completeCount()" (click)="copy(r)" data-test="prep-copy">
            {{ copied ? 'Copied' : 'Copy the answers' }}
          </button>
        </div>
        <p class="mt-6 hidden text-[12px] text-faint print:block">Prepared with careerrisk.ee</p>
      </section>

      <p class="text-[13px] leading-relaxed text-faint print:hidden">
        Your drafts stay in this browser only; we do not store them.
      </p>
    </article>
  `
})
export class PrepPageComponent implements OnInit {
  result: AssessmentResult | null = null;
  questions: InterviewQuestion[] = [];
  questionsError = false;
  draft: Draft = { stories: [], notAi: '', mock: emptyMock() };
  /** null until the status answers; on when the server has a language model key (Claude or OpenAI). */
  mockAvailable: boolean | null = null;
  /** Who receives the answer, for the note under the text box. */
  mockProviderLabel = '';
  mockIndex = 0;
  scoring = false;
  mockError: string | null = null;
  copied = false;
  /** ?profession= from a job page, used for the test link when there is no saved result. */
  professionParam: string | null = null;
  private doneTasks = new Set<string>();

  constructor(private api: AiReadinessService, private analytics: AnalyticsService, private mockStatus: MockInterviewStatusService,
              private route: ActivatedRoute, private destroyRef: DestroyRef) {}

  ngOnInit(): void {
    this.professionParam = this.route.snapshot.queryParamMap.get('profession');
    // arriving from the scrolled result page: start at the top (the router keeps the scroll position)
    if (typeof window !== 'undefined') {
      window.scrollTo(0, 0);
    }
    this.result = this.api.loadLastResult();
    const r = this.result;
    if (!r) {
      return;
    }
    this.draft = this.loadDraft(r);
    this.draft.stories.filter(isComplete).forEach(s => this.doneTasks.add(s.task));
    this.analytics.track('prep-open', { profession: r.professionId });
    this.mockStatus.provider().pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((provider: MockProvider | null) => {
        this.mockAvailable = provider !== null;
        this.mockProviderLabel = MockInterviewStatusService.label(provider);
        if (this.mockAvailable) {
          this.selectMock(0);
        }
      });
    this.api.interviewQuestions(r.professionId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: qs => (this.questions = qs),
      error: () => (this.questionsError = true)
    });
  }

  answer(r: AssessmentResult, s: ProofStory): string | null {
    return answerFor(r.professionName, s);
  }

  completeCount(): number {
    return this.draft.stories.filter(isComplete).length;
  }

  changed(): void {
    const r = this.result;
    if (!r) {
      return;
    }
    this.copied = false;
    for (const s of this.draft.stories) {
      if (isComplete(s) && !this.doneTasks.has(s.task)) {
        this.doneTasks.add(s.task);
        this.analytics.track('prep-answer-done', { profession: r.professionId, count: this.doneTasks.size });
      }
    }
    if (this.mockAvailable && !this.draft.mock.answers[this.mockIndex]?.trim()) {
      this.draft.mock.answers[this.mockIndex] = this.prefill(r, this.mockIndex);
    }
    this.saveDraft(r);
  }

  /** The selected question's text; empty until the questions have loaded. */
  mockQuestion(): string {
    const q: InterviewQuestion | undefined = this.questions[this.mockIndex];
    return q ? q.question : '';
  }

  selectMock(i: number): void {
    this.mockIndex = i;
    this.mockError = null;
    const r = this.result;
    if (r && !this.draft.mock.answers[i]?.trim()) {
      this.draft.mock.answers[i] = this.prefill(r, i);
    }
  }

  /** A first answer from what the person already wrote: their stories for Q1/Q2, their limits for Q4. */
  private prefill(r: AssessmentResult, i: number): string {
    const stories = this.draft.stories.map(s => answerFor(r.professionName, s)).filter((a): a is string => !!a);
    if (i === 0 || i === 1) {
      return stories[i] ?? stories[0] ?? '';
    }
    return i === 3 ? this.draft.notAi : '';
  }

  score(r: AssessmentResult): void {
    const i = this.mockIndex;
    const answer = (this.draft.mock.answers[i] ?? '').trim();
    if (this.scoring || answer.length < 20) {
      return;
    }
    this.scoring = true;
    this.mockError = null;
    this.api.mockInterview(r.professionId, i, answer).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: e => {
        this.scoring = false;
        this.draft.mock.results[i] = e;
        this.saveDraft(r);
        this.analytics.track('mock-score', { profession: r.professionId, question: i + 1, level: e.level });
      },
      error: (err: unknown) => {
        this.scoring = false;
        const status = err instanceof HttpErrorResponse ? err.status : 0;
        this.mockError = status === 429 ? 'Too many scored answers from here. Try again in an hour.'
          : status === 503 ? 'The mock interview is not available right now. Try again later.'
          : status === 400 ? 'Write a little more: at least a sentence or two.'
          : 'Could not score the answer. Try again.';
      }
    });
  }

  useImproved(e: MockEvaluation): void {
    this.draft.mock.answers[this.mockIndex] = e.improvedAnswer;
    this.changed();
  }

  levelColor(score: number): string {
    return LEVEL_COLORS[Math.min(Math.max(score, 1), 4) - 1];
  }

  print(): void {
    const r = this.result;
    if (r) {
      this.analytics.track('prep-print', { profession: r.professionId });
    }
    window.print();
  }

  copy(r: AssessmentResult): void {
    const lines = this.draft.stories
      .map(s => answerFor(r.professionName, s))
      .filter((a): a is string => !!a);
    if (this.draft.notAi.trim()) {
      lines.push(this.draft.notAi.trim());
    }
    navigator.clipboard?.writeText(lines.join('\n\n'))
      .then(() => {
        this.copied = true;
        this.analytics.track('prep-copy', { profession: r.professionId });
      })
      .catch(() => (this.copied = false));
  }

  private loadDraft(r: AssessmentResult): Draft {
    const fresh: Draft = {
      stories: proofTasks(r.tasks).map(t => emptyStory(t.name)),
      notAi: notAiSuggestion(r.tasks, r.humanEdge),
      mock: emptyMock()
    };
    try {
      const all = JSON.parse(localStorage.getItem(DRAFTS_KEY) ?? '{}') as Record<string, Draft>;
      const saved = all[r.professionId];
      if (!saved || !Array.isArray(saved.stories)) {
        return fresh;
      }
      // keep the saved answers for the tasks that are still the top tasks of this result
      return {
        stories: fresh.stories.map(f => ({ ...f, ...(saved.stories.find(s => s && s.task === f.task) ?? {}) })),
        notAi: typeof saved.notAi === 'string' ? saved.notAi : fresh.notAi,
        mock: validMock(saved.mock) ?? fresh.mock
      };
    } catch {
      return fresh;
    }
  }

  private saveDraft(r: AssessmentResult): void {
    try {
      const all = JSON.parse(localStorage.getItem(DRAFTS_KEY) ?? '{}') as Record<string, Draft>;
      all[r.professionId] = this.draft;
      localStorage.setItem(DRAFTS_KEY, JSON.stringify(all));
    } catch {
      // storage full or blocked: the page still works, the draft is just not kept
    }
  }
}

/** A saved mock interview draft from this browser, or null when it is missing or not in the expected shape. */
function validMock(m: unknown): MockDraft | null {
  const d = m as MockDraft | null;
  if (!d || !Array.isArray(d.answers) || !Array.isArray(d.results)
      || d.answers.length !== QUESTION_COUNT || d.results.length !== QUESTION_COUNT) {
    return null;
  }
  return {
    answers: d.answers.map(a => (typeof a === 'string' ? a : '')),
    results: d.results.map(e => (e && typeof e.level === 'string' && typeof e.improvedAnswer === 'string' ? e : null))
  };
}
