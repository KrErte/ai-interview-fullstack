import { Component, DestroyRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { AiReadinessService } from '../../services/ai-readiness.service';
import { AnalyticsService } from '../../services/analytics.service';
import { BillingService, isPaymentRequired } from '../../services/billing.service';
import { PendingPurchase } from '../../models/ai-readiness.model';

type State = 'empty' | 'loading' | 'locked' | 'ready' | 'error' | 'confirming' | 'pending' | 'confirmed';

/**
 * /roadmap (FP-63 shell, FP-64 checkout return).
 *
 * - Plain visit: checks the paid roadmap for the saved answers (402 shows the locked card).
 * - Checkout return (?paid=1[&session_id=…]): polls GET /api/billing/entitlement every 1 s for up to 15 s and
 *   unlocks ONLY on roadmap:true: then the full roadmap is loaded (POST /api/ai-readiness/roadmap), the
 *   'purchase' event is sent once per checkout session, and "Payment confirmed" is shown with a link to the
 *   plan view. Time up: "Payment received, unlocking can take a minute" with Retry. Polling stops on destroy.
 */
@Component({
  selector: 'app-roadmap-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="mx-auto max-w-2xl">
      <h1 class="text-3xl font-bold tracking-tight sm:text-4xl">Your AI roadmap</h1>

      <p *ngIf="state === 'loading'" class="mt-6 text-[15px] text-muted" role="status">Loading your roadmap…</p>
      <p *ngIf="state === 'confirming'" data-test="confirming" class="mt-6 text-[15px] text-muted" role="status" aria-live="polite">
        Confirming your payment…
      </p>

      <div *ngIf="state === 'pending'" data-test="pending" role="status" aria-live="polite"
           class="mt-6 rounded-3xl border border-slate-800/50 bg-slate-900/50 p-5 sm:p-8">
        <p class="text-[15px] font-semibold text-slate-100">Payment received, unlocking can take a minute.</p>
        <p class="mt-2 text-[14px] text-slate-400">You won't be charged again. Try again in a moment.</p>
        <button type="button" data-test="retry" (click)="retryConfirm()"
                class="mt-5 min-h-11 w-full rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 px-6 font-semibold text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-emerald-300 sm:w-auto">
          Retry
        </button>
      </div>

      <div *ngIf="state === 'confirmed'" data-test="confirmed" class="mt-6 space-y-4">
        <p data-test="payment-confirmed" role="status"
           class="rounded-3xl border border-emerald-500/30 bg-emerald-500/10 px-5 py-4 text-[15px] font-semibold text-emerald-200">
          Payment confirmed. Your full roadmap is unlocked.
        </p>
        <a *ngIf="hasAnswers" [routerLink]="'/ai-test'" [queryParams]="{ resume: 'roadmap' }" data-test="view"
           class="inline-flex min-h-11 items-center rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 px-6 font-semibold text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-emerald-300">
          View your roadmap
        </a>
        <a *ngIf="!hasAnswers" routerLink="/ai-test" data-test="take-test" class="link inline-flex min-h-11 items-center text-teal-300">
          Take the test to build your roadmap &rarr;
        </a>
      </div>

      <div *ngIf="state === 'empty'" data-test="empty" class="mt-6 rounded-3xl border border-slate-800/50 bg-slate-900/50 p-5 sm:p-8">
        <p class="text-[15px] text-slate-300">Take the free test first; your roadmap is built from your answers.</p>
        <a routerLink="/ai-test" class="link mt-4 inline-flex min-h-11 items-center text-teal-300">Take the test &rarr;</a>
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
        <button type="button" (click)="paid ? retryConfirm() : load()" class="mt-2 min-h-11 text-sm text-rose-300 underline">Try again</button>
      </div>
    </section>
  `
})
export class RoadmapPageComponent implements OnInit {
  state: State = 'loading';
  /** Back from checkout (?paid=1). */
  paid = false;
  hasAnswers = false;
  private sessionId: string | null = null;
  private sub?: Subscription;

  constructor(
    private api: AiReadinessService,
    private billing: BillingService,
    private analytics: AnalyticsService,
    private route: ActivatedRoute,
    private destroyRef: DestroyRef
  ) {
    this.destroyRef.onDestroy(() => this.sub?.unsubscribe());
  }

  ngOnInit(): void {
    const q = this.route.snapshot.queryParamMap;
    this.paid = q.get('paid') === '1';
    this.sessionId = q.get('session_id');
    if (this.paid) {
      this.retryConfirm();
    } else {
      this.load();
    }
  }

  private saved(): PendingPurchase | null {
    const saved = this.api.loadPendingPurchase() ?? this.api.loadLastAnswers();
    return saved ? { professionId: saved.professionId, answers: saved.answers } : null;
  }

  load(): void {
    const saved = this.saved();
    if (!saved) {
      this.state = 'empty';
      return;
    }
    // the result page restores the answers from here when the user follows either link
    this.api.savePendingPurchase(saved);
    this.state = 'loading';
    this.sub?.unsubscribe();
    this.sub = this.api.fullRoadmap(saved.professionId, saved.answers).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => (this.state = 'ready'),
      error: err => (this.state = isPaymentRequired(err) ? 'locked' : 'error')
    });
  }

  /** Polls the entitlement (1 s, up to 15 s); unlocks only on roadmap:true. */
  retryConfirm(): void {
    const saved = this.saved();
    this.hasAnswers = !!saved;
    this.state = 'confirming';
    this.sub?.unsubscribe();
    this.sub = this.billing.pollEntitlement().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(ok => {
      if (!ok) {
        this.state = 'pending';
        return;
      }
      this.analytics.trackPurchaseOnce(this.sessionId, { product: 'roadmap' });
      if (!saved) {
        this.state = 'confirmed';
        return;
      }
      this.api.savePendingPurchase(saved);
      this.sub = this.api.fullRoadmap(saved.professionId, saved.answers).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: () => (this.state = 'confirmed'),
        error: () => (this.state = 'error')
      });
    });
  }
}
