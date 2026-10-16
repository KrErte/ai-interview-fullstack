import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { AiReadinessService } from '../../services/ai-readiness.service';
import { isPaymentRequired } from '../../services/billing.service';

type State = 'empty' | 'loading' | 'locked' | 'ready' | 'error';

/**
 * Minimal /roadmap shell (FP-63). frontend-risk had no /roadmap page; the plan view lives on the /ai-test
 * result. This page checks the paid roadmap for the saved answers: 402 payment_required shows a locked card
 * that leads back to the paywall; success sends the user to the result, which renders the full roadmap.
 */
@Component({
  selector: 'app-roadmap-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="mx-auto max-w-2xl">
      <h1 class="text-3xl font-bold tracking-tight sm:text-4xl">Your AI roadmap</h1>

      <p *ngIf="state === 'loading'" class="mt-6 text-[15px] text-muted" role="status">Loading your roadmap…</p>

      <div *ngIf="state === 'empty'" data-test="empty" class="mt-6 rounded-3xl border border-slate-800/50 bg-slate-900/50 p-5 sm:p-8">
        <p class="text-[15px] text-slate-300">Take the free test first; your roadmap is built from your answers.</p>
        <a routerLink="/ai-test" class="link mt-4 inline-block text-teal-300">Take the test &rarr;</a>
      </div>

      <div *ngIf="state === 'locked'" data-test="locked" class="mt-6 rounded-3xl border border-slate-800/50 bg-slate-900/50 p-5 sm:p-8">
        <h2 class="text-xl font-bold text-slate-100">Your roadmap is locked</h2>
        <p class="mt-2 text-[15px] leading-relaxed text-slate-300">
          The personal AI roadmap is a one-off purchase. Unlock it from your test result to see every stage and project.
        </p>
        <a [routerLink]="'/ai-test'" [queryParams]="{ resume: 'paywall' }" data-test="to-paywall"
           class="mt-5 inline-block rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 px-6 py-3 font-semibold text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-emerald-300">
          Back to your result
        </a>
      </div>

      <div *ngIf="state === 'ready'" data-test="ready" class="mt-6 rounded-3xl border border-slate-800/50 bg-slate-900/50 p-5 sm:p-8">
        <p class="text-[15px] text-slate-300">Your roadmap is unlocked.</p>
        <a [routerLink]="'/ai-test'" [queryParams]="{ resume: 'roadmap' }" class="link mt-4 inline-block text-teal-300">View your roadmap &rarr;</a>
      </div>

      <div *ngIf="state === 'error'" data-test="error" class="mt-6 rounded-2xl border border-rose-500/30 bg-rose-500/10 p-4" role="alert">
        <p class="text-sm text-rose-200">We could not load your roadmap. Check your connection and try again.</p>
        <button type="button" (click)="load()" class="mt-2 text-sm text-rose-300 underline">Try again</button>
      </div>
    </section>
  `
})
export class RoadmapPageComponent implements OnInit {
  state: State = 'loading';

  constructor(private api: AiReadinessService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const saved = this.api.loadPendingPurchase() ?? this.api.loadLastAnswers();
    if (!saved) {
      this.state = 'empty';
      return;
    }
    // the result page restores the answers from here when the user follows either link
    this.api.savePendingPurchase({ professionId: saved.professionId, answers: saved.answers });
    this.state = 'loading';
    this.api.fullRoadmap(saved.professionId, saved.answers).subscribe({
      next: () => (this.state = 'ready'),
      error: err => (this.state = isPaymentRequired(err) ? 'locked' : 'error')
    });
  }
}
