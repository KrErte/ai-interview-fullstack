import { Component, DestroyRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, catchError, debounceTime, distinctUntilChanged, forkJoin, of, switchMap } from 'rxjs';

import { AiReadinessService } from '../../services/ai-readiness.service';
import { AnalyticsService } from '../../services/analytics.service';
import {
  AssessmentResult,
  CategorySummary,
  ProfessionSummary,
  Question,
  RiskBand,
  RoadmapPhase,
  TaskExposure
} from '../../models/ai-readiness.model';
import { buildReminderIcs, nextSessionStart } from './reminder';

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
  /** The task with the most automatable time, if any task passes FIRST_STEP_MIN_AUTOMATION. */
  task: TaskExposure | null;
  firstItem: string | null;
  tool: string | null;
  project: string | null;
}

/**
 * One concrete start: the task with the most automatable time (share × automation), the first
 * suggested tool and the first roadmap project. Null when there is neither a task nor a roadmap.
 */
export function firstStep(r: AssessmentResult): FirstStep | null {
  const task = (r.tasks ?? [])
    .filter(t => t.automationPercent >= FIRST_STEP_MIN_AUTOMATION)
    .reduce<TaskExposure | null>((best, t) =>
      !best || t.share * t.automationPercent > best.share * best.automationPercent ? t : best, null);
  const firstItem = r.professionRoadmap?.[0]?.items?.[0] ?? null;
  if (!task && !firstItem) {
    return null;
  }
  return { task, firstItem, tool: r.aiTools?.[0] ?? null, project: r.professionRoadmap?.[0]?.project ?? null };
}

@Component({
  selector: 'app-ai-check-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div *ngIf="error" class="mb-8 rounded-xl border border-band-very-high bg-surface px-4 py-3 text-[15px] text-band-very-high" role="alert">
      {{ error }}
    </div>

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
            <input type="checkbox" [(ngModel)]="personalize" class="mt-1 h-4 w-4 shrink-0 accent-[#c6f432]" />
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
          <p class="mt-auto pt-6 text-[15px] leading-relaxed text-muted">
            At your weekly pace, the remaining stages of the general learning path take about
            <span class="font-semibold text-fg">{{ r.totalWeeks }} weeks</span>.
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
          <button (click)="downloadReminder(r, s)" class="btn-ghost mt-5 !px-4 !py-2 !text-[14px] print:hidden">
            Add a weekly reminder
          </button>
          <p class="mt-2 text-[12px] leading-relaxed text-faint print:hidden">
            A calendar file: one session every Monday at 18:00 for 12 weeks. Move it to a time that suits you.
          </p>
        </div>
        <div class="space-y-5 lg:col-span-8">
          <div class="rounded-xl bg-surface-2 p-5">
            <p class="kicker">This week</p>
            <ng-container *ngIf="s.task; else roadmapStart">
              <p class="mt-2 text-[17px] font-semibold leading-snug">Do this task once with AI help: {{ s.task.name }}</p>
              <p class="num mt-2 font-mono text-[12px] text-faint">{{ s.task.share }}% of the week &middot; {{ s.task.automationPercent }}% automatable</p>
              <p *ngIf="s.tool" class="mt-3 text-[15px] leading-relaxed text-muted">A tool to try: <span class="font-semibold text-fg">{{ s.tool }}</span>. Check the result yourself before you use it.</p>
            </ng-container>
            <ng-template #roadmapStart>
              <p class="mt-2 text-[17px] font-semibold leading-snug">{{ s.firstItem }}</p>
              <p class="mt-3 text-[15px] leading-relaxed text-muted">Most of your work stays hands-on, so start where AI helps around it: records, planning and learning.</p>
            </ng-template>
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
        </div>
        <div class="lg:col-span-8">
          <div class="kicker hidden grid-cols-[minmax(0,1fr)_5rem_10rem_3rem] gap-4 border-b border-line pb-3 sm:grid">
            <span>Task</span><span class="text-right">Of week</span><span>Automatable</span><span></span>
          </div>
          <div *ngFor="let t of visibleTasks(r); let i = index"
               class="grid grid-cols-[minmax(0,1fr)_3rem] items-center gap-x-4 gap-y-2 border-b border-line py-4 last:border-b-0 sm:grid-cols-[minmax(0,1fr)_5rem_10rem_3rem]">
            <p class="text-[15px] leading-snug">
              {{ t.name }}
              <span *ngIf="t.observedUse" class="observed-pill" title="People already use AI for this task at work (Anthropic Economic Index, 2025 usage data).">AI already used</span>
              <span class="num ml-1 font-mono text-[12px] text-faint sm:hidden">{{ t.share }}% of week</span>
            </p>
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

      <!-- General roadmap -->
      <section class="card grid gap-8 p-6 sm:p-8 lg:grid-cols-12">
        <div class="lg:col-span-4 lg:pr-6">
          <p class="font-mono text-sm text-accent">06</p>
          <h2 class="mt-3 text-3xl font-bold leading-tight tracking-tight">General AI learning path</h2>
          <p class="mt-3 text-[15px] leading-relaxed text-muted">The same five stages for every profession. Your readiness sets where you start and your weekly hours set how long each stage takes. Stages you already cover are checked.</p>
        </div>
        <ol class="space-y-3 lg:col-span-8">
          <li *ngFor="let s of r.generalRoadmap; let i = index"
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
        <h2 class="mt-3 text-3xl font-bold leading-tight tracking-tight">Starter plan: {{ r.professionName }}</h2>
        <p class="mt-3 max-w-2xl text-[15px] leading-relaxed text-muted">The same steps for everyone in this profession, with a practice project in each phase. Tick off what you have done; your progress is saved in this browser.</p>
        <div *ngIf="roadmapProgress(r) as p" class="mt-5 flex max-w-md items-center gap-4">
          <div class="h-2 flex-1 overflow-hidden rounded-full bg-line" role="progressbar"
               [attr.aria-valuenow]="p.done" aria-valuemin="0" [attr.aria-valuemax]="p.total" aria-label="Roadmap progress">
            <div class="h-full rounded-full bg-accent transition-[width] duration-300" [style.width.%]="p.total ? (p.done / p.total) * 100 : 0"></div>
          </div>
          <span class="num shrink-0 font-mono text-[13px] text-muted">{{ p.done }} / {{ p.total }} done</span>
        </div>

        <div class="mt-8 grid gap-4 md:grid-cols-3">
          <div *ngFor="let ph of r.professionRoadmap; let i = index" class="avoid-break flex flex-col rounded-xl border border-line p-5">
            <div class="flex items-baseline justify-between gap-3">
              <p class="kicker">Phase {{ i + 1 }}</p>
              <span class="num font-mono text-[12px] text-faint">{{ phaseDone(r, ph) }} / {{ ph.items.length + 1 }}</span>
            </div>
            <h3 class="mt-2 text-xl font-bold leading-tight tracking-tight">{{ ph.title }}</h3>
            <ul class="mt-4 flex-1 space-y-2 text-[15px] leading-relaxed">
              <li *ngFor="let it of ph.items">
                <label class="flex cursor-pointer gap-3">
                  <input type="checkbox" class="mt-1 h-4 w-4 shrink-0 accent-[#c6f432]"
                         [checked]="isDone(r, it)" (change)="toggleDone(r, it)" />
                  <span [class.text-muted]="isDone(r, it)" [class.line-through]="isDone(r, it)">{{ it }}</span>
                </label>
              </li>
            </ul>
            <div class="mt-6 rounded-xl bg-surface-2 p-4">
              <p class="kicker !text-accent">Project</p>
              <p class="mt-2 text-[15px] leading-relaxed">{{ ph.project }}</p>
              <label class="mt-3 flex cursor-pointer items-center gap-2 text-[14px] text-muted">
                <input type="checkbox" class="h-4 w-4 accent-[#c6f432]"
                       [checked]="isDone(r, ph.project)" (change)="toggleDone(r, ph.project)" />
                Project done
              </label>
            </div>
          </div>
        </div>
      </section>

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

      <div class="flex flex-col gap-3 pt-2 sm:flex-row sm:justify-end print:hidden">
        <button (click)="restart()" class="btn-ghost">Take the test again</button>
        <button (click)="print()" class="btn-accent">Print / save as PDF</button>
      </div>
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

  result: AssessmentResult | null = null;
  lastResult: AssessmentResult | null = null;
  expanded: Record<string, boolean> = {};

  constructor(
    private api: AiReadinessService,
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
    });
  }

  get currentQuestion(): Question | null {
    return this.questions[this.questionIndex] ?? null;
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
    const personalize = this.personalize && this.aiCommentAvailable;
    this.api.assess({ professionId: this.professionId, answers: this.answers, personalize }).subscribe({
      next: result => {
        this.result = result;
        this.analytics.track('test-complete', { profession: result.professionId, band: result.band });
        this.lastResult = result;
        this.personalNoteRequested = personalize;
        this.savedResultOutdated = false;
        this.api.saveLastResult(result, this.answers);
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
    this.step = 'result';
    const stored = this.api.loadLastAnswers();
    this.savedResultOutdated = !stored || stored.professionId !== saved.professionId;
    if (this.savedResultOutdated || !stored) {
      return;
    }
    this.api.assess({ professionId: stored.professionId, answers: stored.answers, personalize: false }).subscribe({
      next: fresh => {
        const refreshed = { ...fresh, personalNote: saved.personalNote };
        this.api.saveLastResult(refreshed, stored.answers);
        this.lastResult = refreshed;
        // the user may have left the result while the request was running
        if (this.result === saved) {
          this.result = refreshed;
        }
      },
      error: () => { /* keep showing the saved result */ }
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
    this.personalize = false;
    this.personalNoteRequested = false;
    this.savedResultOutdated = false;
    this.expanded = {};
    this.step = 'profession';
    window.scrollTo({ top: 0 });
  }

  /**
   * The TOP_TASKS largest tasks by share, in the server's order (most automatable first);
   * all tasks once expanded.
   */
  visibleTasks(r: AssessmentResult): TaskExposure[] {
    if (this.showAllTasks || r.tasks.length <= this.TOP_TASKS) {
      return r.tasks;
    }
    const top = new Set([...r.tasks].sort((a, b) => b.share - a.share).slice(0, this.TOP_TASKS));
    return r.tasks.filter(t => top.has(t));
  }

  /** Count and week share of the tasks hidden behind "+ N more tasks"; null when nothing is hidden. */
  hiddenTasks(r: AssessmentResult): { count: number; share: number } | null {
    if (r.tasks.length <= this.TOP_TASKS) {
      return null;
    }
    const top = new Set([...r.tasks].sort((a, b) => b.share - a.share).slice(0, this.TOP_TASKS));
    const rest = r.tasks.filter(t => !top.has(t));
    return { count: rest.length, share: rest.reduce((sum, t) => sum + t.share, 0) };
  }

  firstStep(r: AssessmentResult): FirstStep | null {
    return firstStep(r);
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
    window.print();
  }
}
