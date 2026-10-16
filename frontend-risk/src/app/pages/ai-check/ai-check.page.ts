import { Component, DestroyRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, catchError, debounceTime, distinctUntilChanged, forkJoin, map, of, skip, switchMap } from 'rxjs';

import { AiReadinessService, lockedResultView, roadmapTeaserLines } from '../../services/ai-readiness.service';
import { isPaymentRequired } from '../../services/billing.service';
import { PaywallCardComponent } from '../../components/paywall-card/paywall-card.component';
import { AnalyticsService } from '../../services/analytics.service';
import {
  AssessmentRequest,
  AssessmentResult,
  CategorySummary,
  FullRoadmap,
  ProfessionSummary,
  Question,
  RiskBand,
  RoadmapPhase,
  TaskExposure
} from '../../models/ai-readiness.model';
import { buildReminderIcs, nextSessionStart } from './reminder';
import { previewResult, rescaleShares, sharesOf, TaskShares } from './task-shares';
import { TeamService } from '../../services/team.service';
import { TeamStatus } from '../../models/team.model';
import { buildShareFragment, parseShareFragment, SharedResult } from './share-link';

function sameShares(a: TaskShares, b: TaskShares): boolean {
  const keys = Object.keys(a);
  return keys.length === Object.keys(b).length && keys.every(k => a[k] === b[k]);
}

type Step = 'profession' | 'questions' | 'loading' | 'result';

const BAND_STYLE: Record<RiskBand, { label: string; color: string }> = {
  LOW: { label: 'Low', color: 'var(--band-low)' },
  MODERATE: { label: 'Moderate', color: 'var(--band-moderate)' },
  HIGH: { label: 'High', color: 'var(--band-high)' },
  VERY_HIGH: { label: 'Very high', color: 'var(--band-very-high)' }
};

/** Same cut-offs as AiReadinessScorer.band(), drawn under the score. */
const SCALE = [
  { from: 0, to: 30, color: 'var(--band-low)' },
  { from: 30, to: 50, color: 'var(--band-moderate)' },
  { from: 50, to: 70, color: 'var(--band-high)' },
  { from: 70, to: 100, color: 'var(--band-very-high)' }
];

/**
 * A task is only suggested as "try it with AI" when AI can really do a good part of it; below this,
 * the first step is the roadmap's first item instead (phase 1 starts where AI already helps).
 */
const FIRST_STEP_MIN_AUTOMATION = 40;

export interface FirstStep {
  /** The unmarked task with the most automatable time, if any passes FIRST_STEP_MIN_AUTOMATION. */
  task: TaskExposure | null;
  /** Every task that would qualify is marked "I already do this with AI"; no task is suggested. */
  allMarked: boolean;
  firstItem: string | null;
  tool: string | null;
  project: string | null;
}

/**
 * One concrete start: the task with the most automatable time (share × automation), the first
 * suggested tool and the first roadmap project. Tasks the user marked "I already do this with AI"
 * are skipped; the marks never change the score. Null when there is neither a task nor a roadmap.
 */
export function firstStep(r: AssessmentResult, marked: ReadonlySet<string> = new Set()): FirstStep | null {
  const candidates = (r.tasks ?? []).filter(t => t.automationPercent >= FIRST_STEP_MIN_AUTOMATION);
  const task = candidates
    .filter(t => !marked.has(t.name))
    .reduce<TaskExposure | null>((best, t) =>
      !best || t.share * t.automationPercent > best.share * best.automationPercent ? t : best, null);
  const allMarked = candidates.length > 0 && !task;
  const firstItem = r.professionRoadmap?.[0]?.items?.[0] ?? null;
  if (!task && !allMarked && !firstItem) {
    return null;
  }
  return { task, allMarked, firstItem, tool: r.aiTools?.[0] ?? null, project: r.professionRoadmap?.[0]?.project ?? null };
}

@Component({
  selector: 'app-ai-check-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, PaywallCardComponent],
  template: `
    <div *ngIf="error" class="mb-8 rounded-xl border border-band-very-high bg-surface px-4 py-3 text-[15px] text-band-very-high" role="alert">
      {{ error }}
    </div>

    <!-- Team invite (/ai-test?team=CODE): shown before and during the test -->
    <div *ngIf="team && (step === 'profession' || step === 'questions')"
         class="card mb-8 border-l-4 !border-l-accent p-5 text-[15px] leading-relaxed" role="note">
      <ng-container *ngIf="!team.full">
        Your result is added anonymously to <span class="font-semibold text-fg">{{ team.name }}</span>.
        Your employer sees only totals for groups of 5 or more people, never your answers.
      </ng-container>
      <ng-container *ngIf="team.full">
        <span class="font-semibold text-fg">{{ team.name }}</span> is full, so your answers will not be added.
        You can still take the test and see your own result.
      </ng-container>
    </div>
    <p *ngIf="teamLinkInvalid && (step === 'profession' || step === 'questions')" class="mb-8 text-[15px] text-muted" role="note">
      This team link is not valid any more, so your answers will not be added to a team. The test works as usual.
    </p>

    <!-- STEP 1: profession -->
    <section *ngIf="step === 'profession'">
      <p class="kicker rise" style="--i:0">Step 1 of 2 &middot; Your profession</p>
      <h1 class="display rise mt-5 text-[clamp(2.5rem,6.5vw,5.5rem)]" style="--i:1">What do you do for <span class="text-accent">work?</span></h1>
      <p class="rise mt-5 max-w-xl text-[17px] leading-relaxed text-muted" style="--i:2">
        Then {{ questions.length || 11 }} short questions about how you work. No account needed.
      </p>

      <div *ngIf="lastResult" class="card rise mt-10 flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between" style="--i:3">
        <div class="flex items-center gap-4">
          <span class="num font-mono text-2xl font-medium text-accent">{{ lastResult.replaceabilityScore }}</span>
          <p class="text-[15px] text-muted">
            Saved result
            <span class="ml-1 font-semibold text-fg">{{ lastResult.professionName }}</span>
          </p>
        </div>
        <button (click)="showLastResult()" class="btn-ghost self-start !py-2.5 sm:self-auto">
          View result <span class="arrow" aria-hidden="true">&rarr;</span>
        </button>
      </div>

      <div class="rise relative mt-10" style="--i:3">
        <label for="prof-search" class="sr-only">Search professions</label>
        <svg class="pointer-events-none absolute left-5 top-1/2 h-5 w-5 -translate-y-1/2 text-faint" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <circle cx="11" cy="11" r="7" /><path stroke-linecap="round" d="m20 20-3.5-3.5" />
        </svg>
        <input
          id="prof-search"
          type="search"
          [ngModel]="search"
          (ngModelChange)="onSearchChange($event)"
          placeholder="Search 900+ professions…"
          autocomplete="off"
          class="field !rounded-2xl !py-4 !pl-14 !text-[18px]"
        />
      </div>

      <!-- Browse by field: one row that scrolls sideways on small screens -->
      <div *ngIf="categories.length" class="no-scrollbar rise -mx-4 mt-5 overflow-x-auto px-4 pb-1 sm:mx-0 sm:px-0" style="--i:4">
        <div class="flex w-max gap-2 sm:w-auto sm:flex-wrap">
          <button (click)="selectCategory(null)" class="chip" [class.is-active]="!category">All fields</button>
          <button *ngFor="let c of categories" (click)="selectCategory(c.name)" class="chip" [class.is-active]="category === c.name">
            {{ c.name }} <span class="num font-mono text-[11px] opacity-60">{{ c.count }}</span>
          </button>
        </div>
      </div>

      <p *ngIf="!loadingCatalog" class="kicker mt-6" aria-live="polite">
        <ng-container *ngIf="searching">Searching…</ng-container>
        <ng-container *ngIf="!searching && results === null">Popular professions</ng-container>
        <ng-container *ngIf="!searching && results !== null">
          {{ results.length }} {{ results.length === 1 ? 'profession' : 'professions' }}<ng-container *ngIf="category"> &middot; {{ category }}</ng-container>
        </ng-container>
      </p>

      <div class="rise mt-3 grid gap-2 sm:grid-cols-2 lg:grid-cols-3" style="--i:5">
        <button
          *ngFor="let p of visibleProfessions()"
          (click)="selectProfession(p)"
          class="tile flex items-center justify-between gap-4 px-5 py-4 text-left"
        >
          <span class="min-w-0">
            <span class="block text-[17px] font-semibold leading-tight">{{ p.name }}</span>
            <span class="kicker mt-1.5 block truncate !text-faint">{{ p.category }}</span>
          </span>
          <span class="tile-arrow shrink-0 text-accent" aria-hidden="true">&rarr;</span>
        </button>
      </div>
      <p *ngIf="isTruncated()" class="mt-4 text-[14px] text-muted">
        Showing {{ results?.length }} of {{ categoryCount() }}. Type to narrow it down.
      </p>
      <div *ngIf="!searching && results !== null" class="mt-6 flex flex-wrap items-center gap-3 text-[15px] text-muted">
        <span>{{ results.length === 0 ? 'No match.' : 'Not the right one?' }}</span>
        <button *ngIf="otherProfession() as other" (click)="selectProfession(other)" class="btn-ghost !px-4 !py-2 !text-[14px]">
          {{ other.name }}
        </button>
      </div>
      <p *ngIf="loadingCatalog" class="kicker mt-6">Loading…</p>
    </section>

    <!-- STEP 2: questions -->
    <section *ngIf="step === 'questions' && currentQuestion" class="mx-auto max-w-3xl">
      <div class="flex items-center justify-between gap-4">
        <p class="truncate text-[15px] font-semibold text-muted">{{ selectedProfessionName() }}</p>
        <p class="num shrink-0 font-mono text-sm text-faint">
          <span class="text-fg">{{ pad(questionIndex + 1) }}</span> / {{ pad(questions.length) }}
        </p>
      </div>
      <div class="mt-3 flex gap-1" aria-hidden="true">
        <span *ngFor="let _ of questions; let i = index"
              class="h-1.5 flex-1 rounded-full transition-colors duration-200"
              [style.background]="i <= questionIndex ? 'var(--accent)' : 'var(--line)'"></span>
      </div>

      @for (q of [currentQuestion!]; track q.id) {
        <div class="swap-in mt-12 sm:mt-16">
          <h2 class="display text-[clamp(1.85rem,4.5vw,3rem)] !leading-[1.05]">{{ q.text }}</h2>
          <p *ngIf="q.help" class="mt-4 max-w-2xl text-[16px] leading-relaxed text-muted">{{ q.help }}</p>

          <div class="mt-10 space-y-2.5" role="radiogroup" [attr.aria-label]="q.text">
            <button
              *ngFor="let o of q.options; let i = index"
              (click)="answer(q.id, o.id)"
              role="radio"
              [attr.aria-checked]="answers[q.id] === o.id"
              class="option flex w-full items-center gap-4 rounded-xl border px-4 py-4 text-left text-[16px] leading-snug"
              [class.is-selected]="answers[q.id] === o.id"
            >
              <span class="option-key flex h-7 w-7 shrink-0 items-center justify-center rounded-lg border font-mono text-[12px]">{{ letter(i) }}</span>
              <span>{{ o.label }}</span>
            </button>
          </div>

          <label *ngIf="isLastQuestion() && aiCommentAvailable" class="mt-8 flex cursor-pointer items-start gap-3 text-[14px] leading-relaxed text-muted">
            <input type="checkbox" [(ngModel)]="personalize" (ngModelChange)="trackAiComment($event)" class="mt-1 h-4 w-4 shrink-0 accent-[#c6f432]" />
            <span>Add an AI-written comment. Your answers are sent to a language model (OpenAI) without your name or email. The score does not depend on it. <a routerLink="/privacy" class="underline hover:text-fg">Privacy</a></span>
          </label>

          <div class="mt-10 flex items-center justify-between border-t border-line pt-6">
            <button (click)="back()" class="text-[15px] text-muted transition-colors hover:text-fg">&larr; Back</button>
            <button *ngIf="isLastQuestion()" (click)="submit()" [disabled]="!answers[q.id]" class="btn-accent">
              Show result
              <span class="arrow" aria-hidden="true">&rarr;</span>
            </button>
          </div>
        </div>
      }
    </section>

    <!-- STEP 3: loading -->
    <section *ngIf="step === 'loading'" class="flex flex-col items-center py-32" aria-live="polite">
      <p class="text-2xl font-bold tracking-tight">Calculating your result</p>
      <div class="relative mt-6 h-1 w-56 overflow-hidden rounded-full bg-line">
        <div class="absolute inset-y-0 left-0 w-1/3 rounded-full bg-accent" style="animation: scan 1.1s linear infinite"></div>
      </div>
    </section>

    <!-- STEP 4: result -->
    <article *ngIf="step === 'result' && result as r" class="space-y-6">
      <header class="rise pb-2" style="--i:0">
        <p class="kicker">Exposure report</p>
        <h1 class="display mt-3 text-[clamp(2rem,4.5vw,3.5rem)]">{{ r.professionName }}</h1>
        <p *ngIf="savedResultOutdated" class="mt-4 flex flex-wrap items-center gap-3 text-[15px] text-muted print:hidden">
          This result was saved before the plans were updated.
          <button (click)="restart()" class="link text-accent">Take the test again for the latest plan</button>
        </p>
        <p *ngIf="teamNotice" class="mt-4 text-[15px] text-muted print:hidden" role="status">{{ teamNotice }}</p>
        <p *ngIf="sharedView" class="mt-4 flex flex-wrap items-center gap-3 text-[15px] text-muted print:hidden" role="note">
          This is a shared result. Nothing from it is saved in your browser.
          <button (click)="restart()" class="link text-accent">Take the test yourself</button>
        </p>
      </header>

      <!-- Scores -->
      <section class="grid gap-6 lg:grid-cols-12">
        <div class="card rise p-6 sm:p-8 lg:col-span-7" style="--i:1">
          <p class="kicker">Replaceability by AI</p>
          <div class="mt-4 flex items-end gap-3">
            <span class="num text-[clamp(6rem,16vw,10rem)] font-extrabold leading-[0.8] tracking-[-0.05em]"
                  [style.color]="band(r).color">{{ r.replaceabilityScore }}</span>
            <span class="mb-2 font-mono text-lg text-faint">/100</span>
          </div>

          <!-- Position on the scale -->
          <div class="relative mt-10" aria-hidden="true">
            <div class="flex h-2.5 gap-1">
              <span *ngFor="let b of scale; let i = index" class="grow-x h-full basis-0 rounded-full"
                    [style.--i]="i" [style.flex-grow]="b.to - b.from" [style.background]="b.color"></span>
            </div>
            <span class="absolute -top-2 h-[26px] w-1 -translate-x-1/2 rounded-full bg-fg ring-4 ring-surface"
                  [style.left.%]="r.replaceabilityScore"></span>
            <div class="num relative mt-2.5 h-4 font-mono text-[11px] text-faint">
              <span class="absolute left-0">0</span>
              <span *ngFor="let t of [30, 50, 70]" class="absolute -translate-x-1/2" [style.left.%]="t">{{ t }}</span>
              <span class="absolute right-0">100</span>
            </div>
          </div>

          <p class="mt-7 inline-flex items-center gap-2 rounded-full border px-3 py-1"
             [style.border-color]="band(r).color" [style.color]="band(r).color">
            <span class="h-1.5 w-1.5 rounded-full" [style.background]="band(r).color"></span>
            <span class="kicker !text-current">{{ band(r).label }} exposure</span>
          </p>
          <p class="mt-4 max-w-xl text-[clamp(1.25rem,2.4vw,1.6rem)] font-semibold leading-snug tracking-tight">
            {{ r.headline }}
          </p>
          <p class="num mt-4 font-mono text-[12px] text-faint">
            Task exposure {{ r.taskExposureScore }} {{ r.personalAdjustment >= 0 ? '+' : '−' }} {{ abs(r.personalAdjustment) }} from your answers
          </p>
        </div>

        <div class="card rise flex flex-col p-6 sm:p-8 lg:col-span-5" style="--i:2">
          <p class="kicker">AI readiness</p>
          <p class="mt-4 flex items-end gap-2">
            <span class="num text-7xl font-extrabold leading-[0.85] tracking-[-0.04em]">{{ r.readinessScore }}</span>
            <span class="mb-1 font-mono text-faint">/100</span>
          </p>
          <p class="mt-3 text-xl font-semibold text-accent">{{ r.readinessLevel }}</p>
          <div class="mt-6 h-2.5 overflow-hidden rounded-full bg-line">
            <div class="grow-x h-full rounded-full bg-accent" style="--i:4" [style.width.%]="r.readinessScore"></div>
          </div>
          <p *ngIf="planOf(r) as p" class="mt-auto pt-6 text-[15px] leading-relaxed text-muted">
            At your weekly pace, the remaining stages of the general learning path take about
            <span class="font-semibold text-fg">{{ p.totalWeeks }} weeks</span>.
          </p>
        </div>
      </section>

      <section *ngIf="r.personalNote" class="card avoid-break border-l-4 !border-l-accent p-6 sm:p-8">
        <p class="kicker !text-accent">AI comment</p>
        <p class="mt-4 max-w-3xl whitespace-pre-line text-[clamp(1.1rem,2vw,1.3rem)] leading-relaxed">{{ r.personalNote }}</p>
      </section>
      <p *ngIf="!r.personalNote && personalNoteRequested" class="text-[14px] text-muted">
        The AI comment is not available right now. The rest of the result does not depend on it.
      </p>

      <!-- One concrete start -->
      <section *ngIf="firstStep(r) as s" class="card avoid-break grid gap-6 border-accent p-6 sm:p-8 lg:grid-cols-12">
        <div class="lg:col-span-4 lg:pr-6">
          <p class="kicker !text-accent">Start this week</p>
          <h2 class="mt-3 text-3xl font-bold leading-tight tracking-tight">Your first step</h2>
          <p class="mt-3 text-[15px] leading-relaxed text-muted">
            One real try this week beats a course you never finish.
          </p>
          <ng-container *ngIf="planOf(r)">
          <button (click)="downloadReminder(r, s)" data-test="ics" class="btn-ghost mt-5 !px-4 !py-2 !text-[14px] print:hidden">
            Add a weekly reminder
          </button>
          <p class="mt-2 text-[12px] leading-relaxed text-faint print:hidden">
            A calendar file: one session every Monday at 18:00 for 12 weeks. Move it to a time that suits you.
          </p>
          </ng-container>
        </div>
        <div class="space-y-5 lg:col-span-8">
          <div class="rounded-xl bg-surface-2 p-5">
            <p class="kicker">This week</p>
            <ng-container *ngIf="s.task; else noTask">
              <p class="mt-2 text-[17px] font-semibold leading-snug">Do this task once with AI help: {{ s.task.name }}</p>
              <p class="num mt-2 font-mono text-[12px] text-faint">{{ s.task.share }}% of the week &middot; {{ s.task.automationPercent }}% automatable</p>
              <p *ngIf="s.tool" class="mt-3 text-[15px] leading-relaxed text-muted">A tool to try: <span class="font-semibold text-fg">{{ s.tool }}</span>. Check the result yourself before you use it.</p>
              <button (click)="markDoneWithAi(r, s.task.name)" class="link mt-3 text-[14px] text-muted print:hidden">
                I already do this with AI
              </button>
            </ng-container>
            <ng-template #noTask>
              <p *ngIf="s.allMarked" class="mt-2 text-[17px] font-semibold leading-snug">
                You already use AI for every task where it helps most; keep going with the roadmap below.
              </p>
              <ng-container *ngIf="!s.allMarked">
                <p class="mt-2 text-[17px] font-semibold leading-snug">{{ s.firstItem }}</p>
                <p class="mt-3 text-[15px] leading-relaxed text-muted">Most of your work stays hands-on, so start where AI helps around it: records, planning and learning.</p>
              </ng-container>
            </ng-template>
            <p *ngIf="aiMarks(r).size" class="mt-4 flex flex-wrap items-center gap-x-3 text-[13px] text-faint print:hidden">
              <span>{{ aiMarks(r).size }} {{ aiMarks(r).size === 1 ? 'task' : 'tasks' }} marked as already done with AI. This does not change your score.</span>
              <button (click)="clearAiMarks(r)" class="link text-muted">Clear</button>
            </p>
          </div>
          <div *ngIf="s.project" class="rounded-xl border border-line p-5">
            <p class="kicker">Then your first project</p>
            <p class="mt-2 text-[15px] leading-relaxed">{{ s.project }}</p>
            <label class="mt-3 flex cursor-pointer items-center gap-2 text-[14px] text-muted print:hidden">
              <input type="checkbox" class="h-4 w-4 accent-[#c6f432]"
                     [checked]="isDone(r, s.project)" (change)="toggleDone(r, s.project)" />
              Project done
            </label>
          </div>
        </div>
      </section>

      <!-- Tasks -->
      <section class="card grid gap-8 p-6 sm:p-8 lg:grid-cols-12">
        <div class="lg:col-span-4 lg:pr-6">
          <p class="font-mono text-sm text-accent">02</p>
          <h2 class="mt-3 text-3xl font-bold leading-tight tracking-tight">Which tasks AI can take on</h2>
          <p class="mt-3 text-[15px] leading-relaxed text-muted">Typical tasks in this profession, their share of working time, and estimated automation with today's tools.</p>
          <p class="mt-3 text-[15px] leading-relaxed text-muted print:hidden">
            Your week is not the typical one? Drag a share to match it. Automation stays the same, and the other
            tasks adjust so the week still adds up to 100%. Your answers still set the adjustment.
          </p>
          <p *ngIf="customShares" class="mt-3 flex flex-wrap items-center gap-x-3 gap-y-1 text-[14px] print:hidden">
            <span class="kicker !text-accent">Your week</span>
            <button *ngIf="canRecalculate(r)" (click)="resetShares(r)" class="link text-muted">Reset to the typical week</button>
          </p>
        </div>
        <div class="lg:col-span-8">
          <div class="kicker hidden grid-cols-[minmax(0,1fr)_5rem_10rem_3rem] gap-4 border-b border-line pb-3 sm:grid">
            <span>Task</span><span class="text-right">Of week</span><span>Automatable</span><span></span>
          </div>
          <div *ngFor="let t of visibleTasks(r); let i = index; trackBy: trackTask"
               class="grid grid-cols-[minmax(0,1fr)_3rem] items-center gap-x-4 gap-y-2 border-b border-line py-4 last:border-b-0 sm:grid-cols-[minmax(0,1fr)_5rem_10rem_3rem]">
            <div class="min-w-0">
              <p class="text-[15px] leading-snug">
                {{ t.name }}
                <span *ngIf="t.observedUse" class="observed-pill" title="People already use AI for this task at work (Anthropic Economic Index, 2025 usage data).">AI already used</span>
                <span class="num ml-1 font-mono text-[12px] text-faint sm:hidden">{{ t.share }}% of week</span>
              </p>
              <input type="range" min="0" max="100" step="1" class="share-slider mt-2 w-full max-w-sm accent-[#c6f432] print:hidden"
                     [value]="t.share" [disabled]="r.tasks.length < 2"
                     (input)="onShareInput(r, t.name, $any($event.target).valueAsNumber)"
                     [attr.aria-label]="'Share of your week: ' + t.name"
                     [attr.aria-valuetext]="t.share + '% of your week'" />
            </div>
            <span class="num hidden text-right font-mono text-[13px] text-muted sm:block">{{ t.share }}%</span>
            <div class="order-last col-span-2 h-2 overflow-hidden rounded-full bg-line sm:order-none sm:col-span-1">
              <div class="grow-x h-full rounded-full" [style.--i]="i" [style.width.%]="t.automationPercent" [style.background]="taskColor(t.automationPercent)"></div>
            </div>
            <span class="num text-right font-mono text-[14px]">{{ t.automationPercent }}%</span>
          </div>
          <button *ngIf="hiddenTasks(r) as more" (click)="showAllTasks = !showAllTasks"
                  class="mt-4 text-[14px] font-medium text-accent hover:underline">
            {{ showAllTasks ? 'Show fewer tasks' : '+ ' + more.count + ' more tasks (' + more.share + '% of the week)' }}
          </button>
        </div>
      </section>

      <!-- Edge / tools / roles -->
      <section class="grid gap-6 sm:grid-cols-3">
        <div class="card avoid-break p-6">
          <p class="font-mono text-sm text-accent">03</p>
          <h3 class="mt-3 text-2xl font-bold tracking-tight">Your human edge</h3>
          <ul class="mt-4 space-y-3 text-[15px] leading-relaxed">
            <li *ngFor="let h of r.humanEdge" class="flex gap-3"><span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-accent"></span><span>{{ h }}</span></li>
          </ul>
        </div>
        <div class="card avoid-break p-6">
          <p class="font-mono text-sm text-accent">04</p>
          <h3 class="mt-3 text-2xl font-bold tracking-tight">Tools to try</h3>
          <div class="mt-4 flex flex-wrap gap-2">
            <span *ngFor="let tool of r.aiTools" class="rounded-full border border-line bg-surface-2 px-3 py-1.5 text-[13px]">{{ tool }}</span>
          </div>
        </div>
        <div class="card avoid-break p-6">
          <p class="font-mono text-sm text-accent">05</p>
          <h3 class="mt-3 text-2xl font-bold tracking-tight">Related roles to consider</h3>
          <ul class="mt-4 space-y-3 text-[15px] leading-relaxed">
            <li *ngFor="let p of r.pivotRoles" class="flex gap-3"><span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-accent"></span><span>{{ p }}</span></li>
          </ul>
        </div>
      </section>

      <!-- Billing on and not unlocked: teaser + price card instead of the roadmaps (FP-63) -->
      <ng-container *ngIf="!planOf(r); else fullRoadmaps">
        <section id="roadmap-paywall" class="space-y-6 scroll-mt-6">
          <div *ngIf="teaserLines(r).length" class="rounded-3xl border border-slate-800/50 bg-slate-900/50 p-5 sm:p-8" data-test="roadmap-teaser">
            <p class="font-mono text-sm text-accent">06</p>
            <h2 class="mt-3 text-2xl font-bold leading-tight tracking-tight sm:text-3xl">A preview of your roadmap</h2>
            <ul class="mt-4 space-y-2 text-[15px] leading-relaxed text-slate-300">
              <li *ngFor="let t of teaserLines(r)" class="flex gap-3"><span class="text-faint">&ndash;</span><span>{{ t }}</span></li>
            </ul>
          </div>
          <p *ngIf="fullRoadmapLoading" class="text-[15px] text-muted" role="status">Loading your roadmap…</p>
          <div *ngIf="fullRoadmapError" class="rounded-2xl border border-rose-500/30 bg-rose-500/10 p-4" role="alert" data-test="roadmap-error">
            <p class="text-sm text-rose-200">{{ fullRoadmapError }}</p>
            <button type="button" (click)="loadFullRoadmap(r)" class="mt-2 text-sm text-rose-300 underline">Try again</button>
          </div>
          <app-paywall-card [professionId]="r.professionId" [answers]="paywallAnswers(r)"
                            (viewRoadmap)="loadFullRoadmap(r)"></app-paywall-card>
        </section>
      </ng-container>

      <!-- Billing off, or the paid roadmap from POST /api/ai-readiness/roadmap (never the locked result) -->
      <ng-template #fullRoadmaps>
      <ng-container *ngIf="planOf(r) as p">
      <!-- General roadmap -->
      <section class="card grid gap-8 p-6 sm:p-8 lg:grid-cols-12">
        <div class="lg:col-span-4 lg:pr-6">
          <p class="font-mono text-sm text-accent">06</p>
          <h2 class="mt-3 text-3xl font-bold leading-tight tracking-tight">General AI learning path</h2>
          <p class="mt-3 text-[15px] leading-relaxed text-muted">The same five stages for every profession. Your readiness sets where you start and your weekly hours set how long each stage takes. Stages you already cover are checked.</p>
        </div>
        <ol class="space-y-3 lg:col-span-8">
          <li *ngFor="let s of p.generalRoadmap; let i = index"
              class="avoid-break grid grid-cols-[2.75rem_minmax(0,1fr)] gap-x-4 rounded-xl border p-5"
              [ngClass]="s.status === 'CURRENT' ? 'border-accent bg-surface-2' : 'border-line'">
            <span class="flex h-11 w-11 items-center justify-center rounded-full font-mono text-sm font-medium"
                  [ngClass]="s.status === 'DONE' ? 'bg-line text-muted' : s.status === 'CURRENT' ? 'bg-accent text-on-accent' : 'border border-line text-muted'">
              {{ s.status === 'DONE' ? '✓' : pad(i + 1) }}
            </span>
            <div class="pt-1.5">
              <div class="flex flex-wrap items-center gap-x-3 gap-y-1">
                <h3 class="text-xl font-bold leading-tight tracking-tight" [class.text-muted]="s.status === 'DONE'">{{ stageTitle(s.title) }}</h3>
                <span *ngIf="s.status === 'CURRENT'" class="rounded-full bg-accent px-2.5 py-0.5 text-[12px] font-semibold text-on-accent">Start here</span>
                <span *ngIf="s.status !== 'DONE'" class="font-mono text-[12px] text-faint">~{{ s.weeks }} wk</span>
              </div>
              <p class="mt-2 text-[15px] leading-relaxed text-muted">{{ s.goal }}</p>
              <ng-container *ngIf="s.status !== 'DONE' || expanded[s.id]">
                <ul class="mt-4 space-y-1.5 text-[15px] leading-relaxed">
                  <li *ngFor="let it of s.items" class="flex gap-3"><span class="text-faint">&ndash;</span><span>{{ it }}</span></li>
                </ul>
                <div class="mt-4 flex flex-wrap gap-2">
                  <a *ngFor="let res of s.resources" [href]="res.url" target="_blank" rel="noopener"
                     class="rounded-full border border-line px-3 py-1.5 text-[13px] text-accent transition-colors hover:border-accent">
                    {{ res.title }} &nearr;
                  </a>
                </div>
              </ng-container>
              <button *ngIf="s.status === 'DONE' && !expanded[s.id]" (click)="expanded[s.id] = true" class="link mt-3 text-[13px] text-muted">
                Show content
              </button>
            </div>
          </li>
        </ol>
      </section>

      <!-- Profession roadmap -->
      <section class="card p-6 sm:p-8">
        <p class="font-mono text-sm text-accent">07</p>
        <h2 class="mt-3 text-3xl font-bold leading-tight tracking-tight">Starter plan: {{ p.professionName }}</h2>
        <p class="mt-3 max-w-2xl text-[15px] leading-relaxed text-muted">The same steps for everyone in this profession, with a practice project in each phase. Tick off what you have done; your progress is saved in this browser.</p>
        <div *ngIf="roadmapProgress(p) as p" class="mt-5 flex max-w-md items-center gap-4">
          <div class="h-2 flex-1 overflow-hidden rounded-full bg-line" role="progressbar"
               [attr.aria-valuenow]="p.done" aria-valuemin="0" [attr.aria-valuemax]="p.total" aria-label="Roadmap progress">
            <div class="h-full rounded-full bg-accent transition-[width] duration-300" [style.width.%]="p.total ? (p.done / p.total) * 100 : 0"></div>
          </div>
          <span class="num shrink-0 font-mono text-[13px] text-muted">{{ p.done }} / {{ p.total }} done</span>
        </div>

        <div class="mt-8 grid gap-4 md:grid-cols-3">
          <div *ngFor="let ph of p.professionRoadmap; let i = index" class="avoid-break flex flex-col rounded-xl border border-line p-5">
            <div class="flex items-baseline justify-between gap-3">
              <p class="kicker">Phase {{ i + 1 }}</p>
              <span class="num font-mono text-[12px] text-faint">{{ phaseDone(p, ph) }} / {{ ph.items.length + 1 }}</span>
            </div>
            <h3 class="mt-2 text-xl font-bold leading-tight tracking-tight">{{ ph.title }}</h3>
            <ul class="mt-4 flex-1 space-y-2 text-[15px] leading-relaxed">
              <li *ngFor="let it of ph.items">
                <label class="flex cursor-pointer gap-3">
                  <input type="checkbox" class="mt-1 h-4 w-4 shrink-0 accent-[#c6f432]"
                         [checked]="isDone(p, it)" (change)="toggleDone(p, it)" />
                  <span [class.text-muted]="isDone(p, it)" [class.line-through]="isDone(p, it)">{{ it }}</span>
                </label>
              </li>
            </ul>
            <div class="mt-6 rounded-xl bg-surface-2 p-4">
              <p class="kicker !text-accent">Project</p>
              <p class="mt-2 text-[15px] leading-relaxed">{{ ph.project }}</p>
              <label class="mt-3 flex cursor-pointer items-center gap-2 text-[14px] text-muted">
                <input type="checkbox" class="h-4 w-4 accent-[#c6f432]"
                       [checked]="isDone(p, ph.project)" (change)="toggleDone(p, ph.project)" />
                Project done
              </label>
            </div>
          </div>
        </div>
      </section>
      </ng-container>
      </ng-template>

      <footer class="grid gap-4 pt-4 lg:grid-cols-12">
        <p class="kicker lg:col-span-3">Method</p>
        <p class="text-[13px] leading-relaxed text-muted lg:col-span-9">
          The share of your profession's typical tasks multiplied by their estimated automation
          with today's AI tools (in the spirit of the OpenAI/UPenn "GPTs are GPTs" and ILO exposure studies), adjusted by
          at most ±30 points from your answers. It is a catalog model, not a labor-market forecast. A high score means
          those tasks will change, not that the job disappears.
        </p>
        <p *ngIf="r.professionId.startsWith('onet-')" class="text-[13px] leading-relaxed text-muted lg:col-span-9 lg:col-start-4">
          Sources: occupation and task data from O*NET 29.3, U.S. Department of Labor / ETA, CC BY 4.0. Task time shares
          and observed AI use from the Anthropic Economic Index, CC BY. Human exposure labels from Eloundou et al. (2024),
          <em>Science</em>. Automation estimates combine a Claude rating with those labels.
        </p>
      </footer>

      <p class="text-[14px] text-muted print:hidden">
        Want this for your team? <a routerLink="/teams" class="link text-accent">Anonymous team report &rarr;</a>
      </p>

      <div class="flex flex-col gap-3 pt-2 sm:flex-row sm:justify-end print:hidden">
        <button (click)="restart()" class="btn-ghost">Take the test again</button>
        <button *ngIf="shareUrl(r) as url" (click)="copyShareLink(url)" class="btn-ghost">
          {{ shareCopied ? 'Link copied' : 'Copy share link' }}
        </button>
        <button *ngIf="planOf(r)" (click)="print()" data-test="pdf" class="btn-accent">Print / save as PDF</button>
      </div>
      <div *ngIf="shareFallback" class="print:hidden">
        <p class="kicker">Share link</p>
        <input readonly [value]="shareFallback" class="field mt-2 font-mono !text-[13px]" aria-label="Share link" />
      </div>
      <p class="text-[13px] leading-relaxed text-faint print:hidden">
        The share link holds your profession, answers and task shares, so anyone with it sees this result. It is not
        saved on our server; opening it calculates the result again. The AI comment is not included.
      </p>
    </article>
  `
})
export class AiCheckPageComponent implements OnInit {
  readonly scale = SCALE;

  /** Tasks shown before "+ N more tasks"; O*NET professions have up to 40. */
  readonly TOP_TASKS = 6;

  step: Step = 'profession';
  featured: ProfessionSummary[] = [];
  categories: CategorySummary[] = [];
  questions: Question[] = [];
  loadingCatalog = true;
  error: string | null = null;

  search = '';
  category: string | null = null;
  /** Server results for the current search/category; null shows the featured (popular) list. */
  results: ProfessionSummary[] | null = null;
  searching = false;
  selectedProfession: ProfessionSummary | null = null;
  showAllTasks = false;
  private readonly query$ = new Subject<{ q: string; category: string | null }>();

  professionId: string | null = null;
  questionIndex = 0;
  answers: Record<string, string> = {};
  personalize = false;
  /** Shown only when the server has an LLM key; otherwise the checkbox would promise a comment that never comes. */
  aiCommentAvailable = false;
  personalNoteRequested = false;
  /** The open result was saved without its answers, so it may show an older plan. */
  savedResultOutdated = false;
  /** The task shares on screen are the user's own week, not the catalog's typical week. */
  customShares = false;
  private readonly sharesRequest$ = new Subject<AssessmentRequest>();

  result: AssessmentResult | null = null;
  lastResult: AssessmentResult | null = null;
  expanded: Record<string, boolean> = {};

  /** Team from an invite link (/ai-test?team=CODE). */
  team: { code: string; name: string; full: boolean } | null = null;
  teamLinkInvalid = false;
  /** What happened to the anonymous team answer, shown under the result title. */
  teamNotice: string | null = null;
  /** "shares-first-move" is sent once per result on screen. */
  private slidersMoved = false;
  /** Queries already reported as empty, so each is sent once. */
  private readonly emptySearches = new Set<string>();

  /** Paid roadmap (FP-63): loading / error state of POST /api/ai-readiness/roadmap. */
  fullRoadmapLoading = false;
  fullRoadmapError: string | null = null;
  /** The last roadmap request said 402 payment_required. */
  fullRoadmapLocked = false;
  /**
   * The paid roadmap from POST /api/ai-readiness/roadmap, for one profession. Held in memory only: it is
   * never written to localStorage/sessionStorage, and a locked result never carries its own roadmaps.
   */
  entitledRoadmap: { professionId: string; roadmap: FullRoadmap } | null = null;
  private planCache: { source: AssessmentResult; roadmap: FullRoadmap | null; plan: AssessmentResult } | null = null;

  /** Showing someone else's result from a share link: nothing is saved in this browser. */
  sharedView = false;
  shareCopied = false;
  /** The share link, shown in a field when the clipboard is not available. */
  shareFallback: string | null = null;

  constructor(
    private api: AiReadinessService,
    private teams: TeamService,
    private analytics: AnalyticsService,
    private route: ActivatedRoute,
    private router: Router,
    private destroyRef: DestroyRef
  ) {}

  ngOnInit(): void {
    this.lastResult = this.api.loadLastResult();
    forkJoin({
      professions: this.api.professions(),
      questions: this.api.questions(),
      // browsing by field is optional; the test still works without it
      categories: this.api.categories().pipe(catchError(() => of([] as CategorySummary[]))),
      features: this.api.features().pipe(catchError(() => of({ aiComment: false })))
    }).subscribe({
      next: ({ professions, questions, categories, features }) => {
        this.aiCommentAvailable = features.aiComment;
        this.featured = professions;
        this.questions = questions;
        this.categories = categories;
        this.loadingCatalog = false;
        this.preselectFromLink();
        // a share link opened while this page is already showing (only the #fragment changes)
        this.route.fragment.pipe(skip(1), takeUntilDestroyed(this.destroyRef)).subscribe(fragment => {
          const shared = parseShareFragment(fragment);
          if (shared) {
            this.openShared(shared);
          }
        });
      },
      error: () => {
        this.loadingCatalog = false;
        this.error = 'Could not load the test. Check that the backend is running.';
      }
    });

    // switchMap drops a slower, older response, so results always match the latest input
    this.query$.pipe(
      debounceTime(150),
      distinctUntilChanged((a, b) => a.q === b.q && a.category === b.category),
      switchMap(({ q, category }) => !q && !category
        ? of(null)
        : this.api.searchProfessions(q, category).pipe(catchError(() => of([] as ProfessionSummary[])))),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(results => {
      this.results = results;
      this.searching = false;
      this.trackEmptySearch(results);
    });

    // Share sliders: one request after ~300 ms of quiet; switchMap drops an older answer. A reply
    // is used only if the shares on screen are still the ones it was asked for, so a late reply
    // cannot pull a slider back while the user keeps dragging.
    this.sharesRequest$.pipe(
      debounceTime(300),
      switchMap(request => this.api.assess(request).pipe(
        map(fresh => ({ fresh, request })),
        catchError(() => of(null)))),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(reply => {
      if (!reply || !this.result || !sameShares(sharesOf(this.result), reply.request.taskShares ?? {})) {
        return;
      }
      this.applyServerResult(reply.fresh, reply.request.answers, reply.request.taskShares);
    });
  }

  get currentQuestion(): Question | null {
    return this.questions[this.questionIndex] ?? null;
  }

  /**
   * A search with no result tells which job titles are missing. Only the title typed into the job
   * search is sent (lower case, at most 60 characters), once per query.
   */
  private trackEmptySearch(results: ProfessionSummary[] | null): void {
    const q = this.search.trim().toLowerCase().slice(0, 60);
    if (!results || results.length || !q || this.emptySearches.has(q)) {
      return;
    }
    this.emptySearches.add(q);
    this.analytics.track('search-empty', { query: q, category: this.category ?? 'all' });
  }

  trackAiComment(on: boolean): void {
    this.analytics.track('ai-comment-toggle', { on: on ? 'yes' : 'no' });
  }

  onSearchChange(value: string): void {
    this.search = value;
    this.runQuery();
  }

  selectCategory(name: string | null): void {
    this.category = this.category === name ? null : name;
    this.runQuery();
  }

  private runQuery(): void {
    const q = this.search.trim();
    this.searching = !!q || !!this.category;
    this.query$.next({ q, category: this.category });
  }

  visibleProfessions(): ProfessionSummary[] {
    return this.results ?? this.featured;
  }

  otherProfession(): ProfessionSummary | null {
    return this.featured.find(p => p.id === 'other') ?? null;
  }

  categoryCount(): number {
    return this.categories.find(c => c.name === this.category)?.count ?? 0;
  }

  /** The server returns at most 50; say so when a browsed field has more. */
  isTruncated(): boolean {
    return !this.searching && !this.search.trim() && !!this.category && !!this.results
      && this.results.length < this.categoryCount();
  }

  selectedProfessionName(): string {
    return this.selectedProfession?.name ?? '';
  }

  /**
   * /ai-test?profession=<id> (the button on a /jobs page) starts the test with that profession;
   * /ai-test?view=saved opens the saved result.
   */
  private preselectFromLink(): void {
    // back from /login or /register (?resume=paywall) or from /roadmap (?resume=roadmap) with the saved answers
    const resume = this.route.snapshot.queryParamMap.get('resume');
    if ((resume === 'paywall' || resume === 'roadmap') && this.resumePurchase(resume)) {
      return;
    }
    const shared = parseShareFragment(this.route.snapshot.fragment);
    if (shared) {
      this.openShared(shared);
      return;
    }
    this.joinTeamFromLink();
    // /ai-test?view=saved (the "continue your plan" card on the home page) opens the saved result
    if (this.route.snapshot.queryParamMap.get('view') === 'saved') {
      // drop the parameter, so a reload after "Take the test again" does not reopen the result
      this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
      if (this.lastResult) {
        this.showLastResult();
      }
      return;
    }
    const id = this.route.snapshot.queryParamMap.get('profession');
    if (!id) {
      return;
    }
    this.api.profession(id).subscribe({
      next: p => this.selectProfession(p, 'job-page'),
      error: () => { /* unknown id: stay on the picker */ }
    });
  }

  /** /ai-test?team=CODE (a team invite): show whose report the anonymous result goes to. */
  private joinTeamFromLink(): void {
    const raw = this.route.snapshot.queryParamMap.get('team');
    if (raw === null) {
      return;
    }
    const code = TeamService.normalizeCode(raw);
    if (!code) {
      this.teamLinkInvalid = true;
      return;
    }
    this.teams.join(code).subscribe({
      next: invite => {
        this.team = { code, name: invite.name, full: invite.full };
        this.analytics.track('team-joined', { full: invite.full ? 'yes' : 'no' });
      },
      error: () => {
        this.teamLinkInvalid = true;
      }
    });
  }

  /** The team the result on screen belongs to: this invite, or the one saved with the result. */
  private teamCodeFor(r: AssessmentResult): string | null {
    if (this.team && this.professionId === r.professionId) {
      return this.team.code;
    }
    const saved = this.api.loadLastAnswers();
    return saved && saved.professionId === r.professionId ? saved.teamCode ?? null : null;
  }

  /** Line under the result title about the anonymous team answer; null when there was no team. */
  private teamNoticeFor(status: TeamStatus | undefined): string | null {
    switch (status?.status) {
      case 'added':
        return `Your anonymous result was added to ${status.teamName}.`;
      case 'updated':
        return `Your anonymous answer in ${status.teamName} was updated.`;
      case 'full':
        return `${status.teamName} is full, so your result was not added. Your own result is below.`;
      case 'not-found':
        return 'The team link is not valid any more, so your result was not added to a team.';
      default:
        return null;
    }
  }

  /**
   * Opens a shared result: the same assessment runs again from the ids in the link (first without
   * shares to learn the task order, then with the shares if the link has them). Nothing is saved.
   */
  private openShared(shared: SharedResult): void {
    this.sharedView = true;
    this.professionId = shared.professionId;
    this.answers = shared.answers;
    this.step = 'loading';
    const base = { professionId: shared.professionId, answers: shared.answers, personalize: false };
    this.api.assess(base).pipe(
      switchMap(plain => {
        if (!shared.shares || shared.shares.length !== plain.tasks.length) {
          return of({ result: plain, custom: false });
        }
        const taskShares = Object.fromEntries(plain.tasks.map((t, i) => [t.name, shared.shares![i]]));
        return this.api.assess({ ...base, taskShares }).pipe(
          map(result => ({ result, custom: true })),
          catchError(() => of({ result: plain, custom: false })));
      })
    ).subscribe({
      next: ({ result, custom }) => {
        this.result = result;
        this.customShares = custom;
        this.pinnedTop = null;
        this.slidersMoved = false;
        this.step = 'result';
      },
      error: () => {
        this.sharedView = false;
        this.answers = {};
        this.professionId = null;
        this.step = 'profession';
        this.error = 'This shared link could not be opened. You can take the test yourself.';
      }
    });
  }

  /** A link that recalculates this result elsewhere; null when the answers behind it are not known. */
  shareUrl(r: AssessmentResult): string | null {
    const answers = this.answersFor(r);
    if (!answers || !Object.keys(answers).length) {
      return null;
    }
    const shares = this.customShares ? r.tasks.map(t => t.share) : undefined;
    return `${window.location.origin}/ai-test#${buildShareFragment({ professionId: r.professionId, answers, shares })}`;
  }

  copyShareLink(url: string): void {
    const fallback = () => {
      this.shareFallback = url;
    };
    if (!navigator.clipboard) {
      fallback();
      return;
    }
    navigator.clipboard.writeText(url).then(() => {
      this.shareCopied = true;
      this.shareFallback = null;
    }).catch(fallback);
  }

  selectProfession(p: ProfessionSummary, source: 'picker' | 'job-page' = 'picker'): void {
    this.analytics.track('test-start', { profession: p.id, source });
    this.selectedProfession = p;
    this.professionId = p.id;
    this.questionIndex = 0;
    this.step = 'questions';
    window.scrollTo({ top: 0 });
  }

  isLastQuestion(): boolean {
    return this.questionIndex === this.questions.length - 1;
  }

  answer(questionId: string, optionId: string): void {
    this.answers = { ...this.answers, [questionId]: optionId };
    if (!this.isLastQuestion()) {
      // A double click schedules two advances; only the first may move on, or a question gets skipped.
      const index = this.questionIndex;
      setTimeout(() => {
        if (this.questionIndex === index) {
          this.questionIndex = index + 1;
        }
      }, 150);
    }
  }

  allAnswered(): boolean {
    return this.questions.every(q => !!this.answers[q.id]);
  }

  back(): void {
    if (this.questionIndex === 0) {
      this.step = 'profession';
    } else {
      this.questionIndex--;
    }
  }

  submit(): void {
    if (!this.professionId) {
      return;
    }
    if (!this.allAnswered()) {
      this.questionIndex = this.questions.findIndex(q => !this.answers[q.id]);
      return;
    }
    this.error = null;
    this.step = 'loading';
    this.sharedView = false;
    const personalize = this.personalize && this.aiCommentAvailable;
    // a team answer is sent unless the team is full; a browser that already answered updates its answer
    const teamCode = this.team && !this.team.full ? this.team.code : undefined;
    const teamEditToken = teamCode ? this.teams.editToken(teamCode) ?? undefined : undefined;
    this.api.assess({ professionId: this.professionId, answers: this.answers, personalize, teamCode, teamEditToken }).subscribe({
      next: result => {
        this.result = result;
        this.analytics.track('test-complete', { profession: result.professionId, band: result.band });
        this.lastResult = result;
        this.personalNoteRequested = personalize;
        this.savedResultOutdated = false;
        this.customShares = false;
        this.pinnedTop = null;
        this.slidersMoved = false;
        this.teamNotice = this.teamNoticeFor(result.team);
        if (result.team?.status === 'added' || result.team?.status === 'updated') {
          this.analytics.track('team-submitted', { status: result.team.status, band: result.band });
        }
        if (teamCode && result.team?.status === 'added' && result.team.editToken) {
          this.teams.rememberAnswer(teamCode, result.team.editToken);
        }
        const savedTeam = teamCode && (result.team?.status === 'added' || result.team?.status === 'updated') ? teamCode : undefined;
        this.api.saveLastResult(result, this.answers, undefined, savedTeam);
        this.showAllTasks = false;
        this.step = 'result';
        window.scrollTo({ top: 0 });
      },
      error: () => {
        this.error = 'Could not calculate your result. Please try again.';
        this.step = 'questions';
      }
    });
  }

  /**
   * Shows the saved result at once, then recalculates it from the saved answers so the tasks and
   * roadmaps match today's catalog. The AI comment is kept, not requested again. Results saved
   * before answers were stored cannot be recalculated; those get a hint to retake the test.
   */
  showLastResult(): void {
    const saved = this.lastResult;
    if (!saved) {
      return;
    }
    this.result = saved;
    this.showAllTasks = false;
    this.personalNoteRequested = false;
    this.pinnedTop = null;
    this.slidersMoved = false;
    this.teamNotice = null;
    this.sharedView = false;
    this.step = 'result';
    const stored = this.api.loadLastAnswers();
    this.savedResultOutdated = !stored || stored.professionId !== saved.professionId;
    this.customShares = !!stored?.taskShares && !this.savedResultOutdated;
    if (this.savedResultOutdated || !stored) {
      return;
    }
    // the user's own shares are sent again; if the catalog has renamed a task the server says 400
    // and the saved result stays as it is
    this.api.assess({
      professionId: stored.professionId, answers: stored.answers, personalize: false, taskShares: stored.taskShares
    }).subscribe({
      next: fresh => {
        const refreshed = { ...fresh, personalNote: saved.personalNote };
        this.api.saveLastResult(refreshed, stored.answers, stored.taskShares, stored.teamCode);
        this.lastResult = refreshed;
        // the user may have left the result while the request was running
        if (this.result === saved) {
          this.result = refreshed;
        }
      },
      error: () => { /* keep showing the saved result */ }
    });
  }

  teaserLines(r: AssessmentResult): string[] {
    return roadmapTeaserLines(r.roadmapTeaser);
  }

  /** The answers behind the result on screen, for the paywall's login detour and the roadmap request. */
  paywallAnswers(r: AssessmentResult): Record<string, string> {
    if (Object.keys(this.answers).length) {
      return this.answers;
    }
    const stored = this.api.loadLastAnswers();
    return stored && stored.professionId === r.professionId ? stored.answers : {};
  }

  /**
   * Restores the result from the answers saved before the login detour and shows the paywall again
   * (or, for 'roadmap', loads the paid roadmap at once). False when nothing is saved.
   */
  resumePurchase(mode: 'paywall' | 'roadmap'): boolean {
    const pending = this.api.loadPendingPurchase();
    this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
    if (!pending) {
      return false;
    }
    this.professionId = pending.professionId;
    this.answers = pending.answers;
    this.sharedView = false;
    this.step = 'loading';
    this.api.assess({ professionId: pending.professionId, answers: pending.answers, personalize: false }).subscribe({
      next: result => {
        this.api.clearPendingPurchase();
        this.result = result;
        this.customShares = false;
        this.pinnedTop = null;
        this.slidersMoved = false;
        this.step = 'result';
        if (mode === 'roadmap' || !result.roadmapLocked) {
          if (result.roadmapLocked) {
            this.loadFullRoadmap(result);
          }
          return;
        }
        setTimeout(() => document.getElementById('roadmap-paywall')?.scrollIntoView({ block: 'start' }));
      },
      error: () => {
        this.error = 'Could not restore your result. Please take the test again.';
        this.step = 'profession';
      }
    });
    return true;
  }

  /**
   * Loads the paid roadmap from POST /api/ai-readiness/roadmap (in memory only) and renders it with the
   * existing plan view (see planOf). FP-64 calls this after polling the entitlement.
   */
  loadFullRoadmap(r: AssessmentResult): void {
    if (this.fullRoadmapLoading) return;
    this.fullRoadmapLoading = true;
    this.fullRoadmapError = null;
    this.fullRoadmapLocked = false;
    this.api.fullRoadmap(r.professionId, this.paywallAnswers(r)).subscribe({
      next: full => {
        this.fullRoadmapLoading = false;
        // kept apart from the result, so saveLastResult never sees the paid roadmap
        this.entitledRoadmap = { professionId: r.professionId, roadmap: full };
      },
      error: err => {
        this.fullRoadmapLoading = false;
        this.fullRoadmapLocked = isPaymentRequired(err);
        this.fullRoadmapError = this.fullRoadmapLocked
          ? 'Your roadmap is not unlocked yet. If you just paid, wait a moment and try again.'
          : 'We could not load your roadmap. Check your connection and try again.';
      }
    });
  }

  restart(): void {
    this.answers = {};
    this.professionId = null;
    this.selectedProfession = null;
    this.search = '';
    this.category = null;
    this.results = null;
    this.searching = false;
    this.showAllTasks = false;
    this.questionIndex = 0;
    this.result = null;
    this.fullRoadmapError = null;
    this.fullRoadmapLocked = false;
    this.entitledRoadmap = null;
    this.personalize = false;
    this.personalNoteRequested = false;
    this.savedResultOutdated = false;
    this.customShares = false;
    this.pinnedTop = null;
    this.teamNotice = null;
    this.sharedView = false;
    this.shareCopied = false;
    this.shareFallback = null;
    this.expanded = {};
    this.step = 'profession';
    window.scrollTo({ top: 0 });
  }

  /**
   * The TOP_TASKS largest tasks by share, in the server's order (most automatable first);
   * all tasks once expanded. The set is fixed when the result opens, so a row does not vanish
   * from under the user's finger while they drag its share down.
   */
  visibleTasks(r: AssessmentResult): TaskExposure[] {
    if (this.showAllTasks || r.tasks.length <= this.TOP_TASKS) {
      return r.tasks;
    }
    const top = this.topTaskNames(r);
    return r.tasks.filter(t => top.has(t.name));
  }

  /** Count and week share of the tasks hidden behind "+ N more tasks"; null when nothing is hidden. */
  hiddenTasks(r: AssessmentResult): { count: number; share: number } | null {
    if (r.tasks.length <= this.TOP_TASKS) {
      return null;
    }
    const top = this.topTaskNames(r);
    const rest = r.tasks.filter(t => !top.has(t.name));
    return { count: rest.length, share: rest.reduce((sum, t) => sum + t.share, 0) };
  }

  private pinnedTop: { professionId: string; names: Set<string> } | null = null;

  private topTaskNames(r: AssessmentResult): Set<string> {
    if (this.pinnedTop?.professionId !== r.professionId) {
      const names = [...r.tasks].sort((a, b) => b.share - a.share).slice(0, this.TOP_TASKS).map(t => t.name);
      this.pinnedTop = { professionId: r.professionId, names: new Set(names) };
    }
    return this.pinnedTop.names;
  }

  trackTask(_: number, t: TaskExposure): string {
    return t.name;
  }

  /** The answers behind the result on screen: this test's, or the saved ones for that profession. */
  private answersFor(r: AssessmentResult): Record<string, string> | null {
    if (this.professionId === r.professionId && Object.keys(this.answers).length) {
      return this.answers;
    }
    const saved = this.api.loadLastAnswers();
    return saved && saved.professionId === r.professionId ? saved.answers : null;
  }

  canRecalculate(r: AssessmentResult): boolean {
    return !!this.answersFor(r);
  }

  /**
   * A share slider moved: rescale the other shares, update the score at once with the same
   * formula as the server, then (debounced) ask the server with the same answers and no AI
   * comment. Without saved answers the local preview is kept and saved as it is.
   */
  onShareInput(r: AssessmentResult, name: string, value: number): void {
    if (!Number.isFinite(value)) {
      return;
    }
    this.topTaskNames(r);
    if (!this.slidersMoved) {
      this.slidersMoved = true;
      this.analytics.track('shares-first-move', { profession: r.professionId });
    }
    const taskShares = rescaleShares(sharesOf(r), name, value);
    const preview = previewResult(r, taskShares);
    this.result = preview;
    this.customShares = true;
    const answers = this.answersFor(r);
    if (answers) {
      this.sharesRequest$.next({
        professionId: r.professionId, answers, personalize: false, taskShares, teamEditToken: this.teamEditTokenFor(r)
      });
    } else {
      if (!this.sharedView) {
        this.lastResult = preview;
        this.api.saveLastResult(preview);
      }
    }
  }

  /** Back to the catalog's typical week (needs the answers, as the server recalculates it). */
  resetShares(r: AssessmentResult): void {
    const answers = this.answersFor(r);
    if (!answers) {
      return;
    }
    this.api.assess({ professionId: r.professionId, answers, personalize: false, teamEditToken: this.teamEditTokenFor(r) }).subscribe({
      next: fresh => this.applyServerResult(fresh, answers, undefined),
      error: () => { /* keep the current shares */ }
    });
  }

  /** Shows a recalculated result, keeping the AI comment, and saves it with the shares behind it. */
  private applyServerResult(fresh: AssessmentResult, answers: Record<string, string>,
                            taskShares: Record<string, number> | undefined): void {
    const current = this.result;
    if (!current || current.professionId !== fresh.professionId) {
      return;
    }
    const updated = { ...fresh, personalNote: current.personalNote };
    this.result = updated;
    this.customShares = !!taskShares;
    if (!taskShares) {
      this.pinnedTop = null;
    }
    if (this.sharedView) {
      return;
    }
    this.lastResult = updated;
    this.api.saveLastResult(updated, answers, taskShares, this.teamCodeFor(current) ?? undefined);
  }

  /** Edit token of this browser's team answer for the result on screen, so slider moves update it. */
  private teamEditTokenFor(r: AssessmentResult): string | undefined {
    if (this.sharedView) {
      // someone else's shared result must never update this browser's own team answer
      return undefined;
    }
    const code = this.teamCodeFor(r);
    return code ? this.teams.editToken(code) ?? undefined : undefined;
  }

  firstStep(r: AssessmentResult): FirstStep | null {
    return firstStep(this.planOf(r) ?? lockedResultView(r), this.aiMarks(r));
  }

  /**
   * The result with the roadmaps to show: the result itself when it is not locked (billing off), the
   * paid roadmap from POST /roadmap merged in when locked and entitled, and null when locked and unpaid.
   */
  planOf(r: AssessmentResult): AssessmentResult | null {
    if (!r.roadmapLocked) return r;
    const paid = this.entitledRoadmap?.professionId === r.professionId ? this.entitledRoadmap.roadmap : null;
    if (!paid) return null;
    if (this.planCache?.source !== r || this.planCache.roadmap !== paid) {
      this.planCache = {
        source: r, roadmap: paid,
        plan: { ...r, generalRoadmap: paid.generalRoadmap ?? [], professionRoadmap: paid.professionRoadmap ?? [], totalWeeks: paid.totalWeeks }
      };
    }
    return this.planCache.plan;
  }

  /** Tasks marked "I already do this with AI" for the profession on screen; used only by the first-step box. */
  private marks: { professionId: string; names: Set<string> } | null = null;

  aiMarks(r: AssessmentResult): Set<string> {
    if (this.marks?.professionId !== r.professionId) {
      this.marks = { professionId: r.professionId, names: new Set(this.api.loadAiMarks(r.professionId)) };
    }
    return this.marks.names;
  }

  markDoneWithAi(r: AssessmentResult, taskName: string): void {
    const names = this.aiMarks(r);
    names.add(taskName);
    this.api.saveAiMarks(r.professionId, [...names]);
  }

  clearAiMarks(r: AssessmentResult): void {
    this.aiMarks(r).clear();
    this.api.saveAiMarks(r.professionId, []);
  }

  /**
   * Downloads a weekly .ics reminder for practising the plan. Session length follows the weekly
   * hours answer (1 h → 60 min, 3 h → 90 min, more → 120 min); 60 min when it is not known.
   */
  downloadReminder(r: AssessmentResult, s: FirstStep): void {
    const saved = this.api.loadLastAnswers();
    const hours = Number(saved?.professionId === r.professionId ? saved.answers['hours'] : NaN);
    const minutes = hours >= 6 ? 120 : hours >= 3 ? 90 : 60;
    const ics = buildReminderIcs({
      professionName: r.professionName,
      firstStep: s.task ? `Do this task once with AI help: ${s.task.name}` : (s.firstItem ?? 'Start the first phase of your plan.'),
      planUrl: `${window.location.origin}/ai-test?view=saved`,
      start: nextSessionStart(new Date()),
      minutes
    });
    const url = URL.createObjectURL(new Blob([ics], { type: 'text/calendar;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = 'ai-practice-reminder.ics';
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    this.analytics.track('reminder-download', { profession: r.professionId });
  }

  /** Ticked roadmap steps (items and projects, by text) for the profession on screen. */
  private progress: { professionId: string; done: Set<string> } | null = null;

  private doneSet(r: AssessmentResult): Set<string> {
    if (this.progress?.professionId !== r.professionId) {
      this.progress = { professionId: r.professionId, done: new Set(this.api.loadProgress(r.professionId)) };
    }
    return this.progress.done;
  }

  isDone(r: AssessmentResult, step: string): boolean {
    return this.doneSet(r).has(step);
  }

  toggleDone(r: AssessmentResult, step: string): void {
    const done = this.doneSet(r);
    if (done.has(step)) {
      done.delete(step);
    } else {
      done.add(step);
      this.analytics.track('roadmap-step-done', { profession: r.professionId });
    }
    this.api.saveProgress(r.professionId, [...done]);
  }

  /** Ticked steps in one phase, counting its project as a step. */
  phaseDone(r: AssessmentResult, phase: RoadmapPhase): number {
    return [...phase.items, phase.project].filter(step => this.isDone(r, step)).length;
  }

  /** Ticked and total steps over the whole profession roadmap; null when it has none. */
  roadmapProgress(r: AssessmentResult): { done: number; total: number } | null {
    const phases = r.professionRoadmap ?? [];
    if (!phases.length) {
      return null;
    }
    return {
      done: phases.reduce((sum, ph) => sum + this.phaseDone(r, ph), 0),
      total: phases.reduce((sum, ph) => sum + ph.items.length + 1, 0)
    };
  }

  band(r: AssessmentResult) {
    return BAND_STYLE[r.band];
  }

  /** Bar colour for one task, same 40/70 cut-offs the old design used. */
  taskColor(automationPercent: number): string {
    if (automationPercent >= 70) {
      return 'var(--band-very-high)';
    }
    return automationPercent >= 40 ? 'var(--band-moderate)' : 'var(--band-low)';
  }

  /** Stage titles in the content carry their own "2. " prefix; the big numeral already shows it. */
  stageTitle(title: string): string {
    return title.replace(/^\d+\.\s*/, '');
  }

  letter(i: number): string {
    return String.fromCharCode(65 + i);
  }

  pad(n: number): string {
    return n < 10 ? '0' + n : String(n);
  }

  abs(n: number): number {
    return Math.abs(n);
  }

  print(): void {
    if (this.result) {
      this.analytics.track('print', { page: 'result', profession: this.result.professionId });
    }
    window.print();
  }
}
