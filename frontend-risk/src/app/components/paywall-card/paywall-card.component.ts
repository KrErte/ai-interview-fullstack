import {
  AfterViewInit, ChangeDetectorRef, Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';

import { AnalyticsService } from '../../services/analytics.service';
import { AuthService } from '../../services/auth.service';
import { BillingService } from '../../services/billing.service';
import { BrowserRedirectService } from '../../services/browser-redirect.service';
import { AiReadinessService } from '../../services/ai-readiness.service';
import { WAIVER_TEXT, WAIVER_TEXT_VERSION } from '../../models/billing.model';

/** Where the paywall lives; /login and /register come back here with ?resume=paywall. */
export const PAYWALL_RETURN_URL = '/ai-test?resume=paywall';

/** FP-65 copy (drafts/paywall-copy.md, headline variant A). Unbuilt [PLANNED] claims are left out. */
export const PAYWALL_COPY = {
  title: 'Turn your result into a plan you can start this week',
  subline: 'Your free result shows which tasks AI tools can already do. The personal roadmap shows what to practise next.',
  bullets: [
    'A step-by-step learning plan for your profession',
    "Practice projects based on your occupation's real tasks (written with AI, so treat them as suggestions)",
    'Download as PDF and update it when you re-take the test'
  ],
  price: 'EUR 19 – one-off payment, no subscription. [VAT TBD: "incl. VAT"]',
  waiver: WAIVER_TEXT,
  waiverVersion: WAIVER_TEXT_VERSION,
  waiverError: 'Please tick the box to continue.',
  buy: 'Pay €19',
  buyLoggedOut: 'Pay €19',
  loggedOutNote: 'You will log in or create an account before paying.',
  smallPrint: 'Secure payment by Stripe. We never see your card details. Not a forecast, and not career advice.',
  busy: 'Opening secure checkout…',
  checking: 'Checking your purchase…',
  error: "We couldn't start the checkout. Check your connection and try again.",
  retry: 'Try again',
  entitledTitle: 'Your personal AI roadmap is unlocked',
  entitledText: 'You have already bought the roadmap. Open it any time.',
  view: 'View your roadmap',
  terms: 'Terms',
  refund: 'Refunds',
  privacy: 'Privacy'
} as const;

type State = 'checking' | 'paywall' | 'entitled';

/**
 * Price card for the paid roadmap on the /ai-test result (BILLING_ENABLED=true, roadmapLocked).
 *
 * - Logged out: "buy" saves professionId + answers in sessionStorage and goes to /login with
 *   returnUrl=/ai-test?resume=paywall; the result page restores them and shows this card again.
 * - Logged in: the entitlement is checked first; roadmap:true shows "View your roadmap" (emits viewRoadmap).
 * - paywall_view is sent once per card instance, the first time the price card is at least half
 *   visible (IntersectionObserver); without IntersectionObserver it is sent when the card first renders.
 * - checkout_start is sent on every buy click, before login redirect or the checkout POST.
 */
@Component({
  selector: 'app-paywall-card',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="rounded-3xl border border-slate-800/50 bg-slate-900/50 p-5 sm:p-8" aria-labelledby="paywall-title">
      <p *ngIf="state === 'checking'" class="text-[15px] text-slate-400" role="status">{{ copy.checking }}</p>

      <ng-container *ngIf="state === 'entitled'">
        <h2 id="paywall-title" class="text-2xl font-bold tracking-tight text-slate-100">{{ copy.entitledTitle }}</h2>
        <p class="mt-2 text-[15px] text-slate-400">{{ copy.entitledText }}</p>
        <button type="button" data-test="view-roadmap" (click)="viewRoadmap.emit()"
                class="mt-5 w-full rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 px-6 py-3 font-semibold text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-emerald-300 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-900 sm:w-auto">
          {{ copy.view }}
        </button>
      </ng-container>

      <ng-container *ngIf="state === 'paywall'">
        <h2 id="paywall-title" class="text-2xl font-bold tracking-tight text-slate-100 sm:text-3xl">{{ copy.title }}</h2>
        <p class="mt-2 text-[15px] leading-relaxed text-slate-400">{{ copy.subline }}</p>
        <ul class="mt-4 space-y-2 text-[15px] leading-relaxed text-slate-300">
          <li *ngFor="let b of copy.bullets" class="flex gap-3">
            <span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-400" aria-hidden="true"></span><span>{{ b }}</span>
          </li>
        </ul>
        <p class="mt-5 text-[15px] font-semibold text-slate-100" data-test="price">{{ copy.price }}</p>

        <div class="mt-5">
          <label for="paywall-waiver" class="flex cursor-pointer items-start gap-3 text-[14px] leading-relaxed text-slate-300">
            <input id="paywall-waiver" type="checkbox" required data-test="waiver" [attr.data-waiver-version]="copy.waiverVersion"
                   class="mt-1 h-5 w-5 shrink-0 accent-emerald-500 focus:outline-none focus-visible:ring-2 focus-visible:ring-emerald-300"
                   [checked]="waiver" (change)="onWaiver($any($event.target).checked)"
                   [attr.aria-invalid]="waiverError" [attr.aria-describedby]="waiverError ? 'paywall-waiver-error' : null" />
            <span>{{ copy.waiver }}</span>
          </label>
          <p *ngIf="waiverError" id="paywall-waiver-error" data-test="waiver-error" class="mt-2 text-sm text-rose-400" role="alert">{{ copy.waiverError }}</p>
        </div>

        <button type="button" data-test="buy" (click)="buy()" [disabled]="!waiver || busy" [attr.aria-busy]="busy"
                class="mt-5 w-full rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 px-6 py-3 font-semibold text-white transition-opacity focus:outline-none focus-visible:ring-2 focus-visible:ring-emerald-300 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-900 disabled:cursor-not-allowed disabled:opacity-50 sm:w-auto">
          {{ busy ? copy.busy : loggedIn ? copy.buy : copy.buyLoggedOut }}
        </button>
        <p *ngIf="!loggedIn" class="mt-2 text-[13px] text-slate-400">{{ copy.loggedOutNote }}</p>

        <p class="mt-4 text-[13px] leading-relaxed text-slate-400">{{ copy.smallPrint }}</p>
        <p class="mt-2 text-[13px] text-slate-400">
          <a routerLink="/terms" class="text-teal-300 underline focus:outline-none focus-visible:ring-2 focus-visible:ring-teal-300">{{ copy.terms }}</a>
          &middot;
          <a routerLink="/refund" class="text-teal-300 underline focus:outline-none focus-visible:ring-2 focus-visible:ring-teal-300">{{ copy.refund }}</a>
          &middot;
          <a routerLink="/privacy" class="text-teal-300 underline focus:outline-none focus-visible:ring-2 focus-visible:ring-teal-300">{{ copy.privacy }}</a>
        </p>

        <div *ngIf="error" data-test="error" class="mt-4 rounded-2xl border border-rose-500/30 bg-rose-500/10 p-4" role="alert">
          <p class="text-sm text-rose-200">{{ copy.error }}</p>
          <button type="button" data-test="retry" (click)="buy()" [disabled]="!waiver || busy"
                  class="mt-2 text-sm text-rose-300 underline focus:outline-none focus-visible:ring-2 focus-visible:ring-rose-300">{{ copy.retry }}</button>
        </div>
      </ng-container>
    </section>
  `
})
export class PaywallCardComponent implements OnInit, AfterViewInit, OnDestroy {
  @Input({ required: true }) professionId!: string;
  @Input({ required: true }) answers: Record<string, string> = {};
  @Output() viewRoadmap = new EventEmitter<void>();

  readonly copy = PAYWALL_COPY;
  state: State = 'paywall';
  loggedIn = false;
  waiver = false;
  waiverError = false;
  busy = false;
  error = false;

  private viewSent = false;
  private viewReady = false;
  private observer: IntersectionObserver | null = null;

  constructor(
    private billing: BillingService,
    private auth: AuthService,
    private api: AiReadinessService,
    private analytics: AnalyticsService,
    private redirect: BrowserRedirectService,
    private router: Router,
    private host: ElementRef<HTMLElement>,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loggedIn = this.auth.isLoggedIn();
    if (this.loggedIn) {
      this.refreshEntitlement();
    }
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.watchPaywallView();
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  onWaiver(checked: boolean): void {
    this.waiver = checked;
    if (checked) {
      this.waiverError = false;
    }
  }

  buy(): void {
    if (this.busy) return;
    if (!this.waiver) {
      this.waiverError = true;
      return;
    }
    this.analytics.track('checkout_start', { profession: this.professionId, loggedIn: this.loggedIn ? 'yes' : 'no' });
    if (!this.auth.isLoggedIn()) {
      this.api.savePendingPurchase({ professionId: this.professionId, answers: this.answers });
      this.router.navigate(['/login'], { queryParams: { returnUrl: PAYWALL_RETURN_URL } });
      return;
    }
    this.busy = true;
    this.error = false;
    this.billing.startCheckout().subscribe({
      next: ({ url }) => {
        // stays busy while the browser leaves the page
        this.redirect.assign(url);
      },
      error: err => {
        this.busy = false;
        const kind = BillingService.classifyCheckoutError(err);
        if (kind === 'already_paid') {
          this.refreshEntitlement(true);
        } else if (kind === 'waiver_required') {
          this.waiver = false;
          this.waiverError = true;
        } else {
          this.error = true;
        }
      }
    });
  }

  /** alreadyPaid: the checkout said 409, so the roadmap counts as unlocked even if the refresh fails. */
  private refreshEntitlement(alreadyPaid = false): void {
    this.state = 'checking';
    this.billing.getEntitlement().subscribe({
      next: e => this.setState(e.roadmap || alreadyPaid ? 'entitled' : 'paywall'),
      // a failed check must not hide the offer; the checkout answers 409 if it was paid after all
      error: () => this.setState(alreadyPaid ? 'entitled' : 'paywall')
    });
  }

  private setState(state: State): void {
    this.state = state;
    this.cdr.markForCheck();
    this.watchPaywallView();
  }

  private watchPaywallView(): void {
    if (!this.viewReady || this.viewSent || this.observer || this.state !== 'paywall') return;
    const IO = typeof window !== 'undefined' ? (window as { IntersectionObserver?: typeof IntersectionObserver }).IntersectionObserver : undefined;
    if (!IO) {
      this.sendPaywallView();
      return;
    }
    this.observer = new IO(entries => {
      if (this.state === 'paywall' && entries.some(e => e.isIntersecting)) {
        this.sendPaywallView();
      }
    }, { threshold: 0.5 });
    this.observer.observe(this.host.nativeElement);
  }

  private sendPaywallView(): void {
    if (this.viewSent) return;
    this.viewSent = true;
    this.observer?.disconnect();
    this.observer = null;
    this.analytics.track('paywall_view', { profession: this.professionId });
  }
}
