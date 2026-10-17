import { Component, DestroyRef, Input, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { WaitlistService } from '../../services/waitlist.service';
import { AnalyticsService } from '../../services/analytics.service';
import { MockInterviewStatusService } from '../../services/mock-interview-status.service';

/**
 * The AI interview prep offer on the result (docs/vajadus-ai-ajastul-2026-10-17.md): a link to the free beta (/prep)
 * and an email list for the mock interview that comes next. Clicks and sign-ups are counted.
 */
@Component({
  selector: 'app-prep-waitlist-card',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="card avoid-break grid gap-6 p-6 sm:p-8 lg:grid-cols-12 print:hidden" data-test="prep-card">
      <div class="lg:col-span-5 lg:pr-6">
        <p class="kicker !text-accent">Free beta</p>
        <h2 class="mt-3 text-3xl font-bold leading-tight tracking-tight">Interview or review coming up?</h2>
        <p class="mt-3 text-[15px] leading-relaxed text-muted">
          More and more employers ask how you use AI in your work, in job interviews and in performance reviews.
        </p>
      </div>
      <div class="lg:col-span-7">
        <p class="text-[15px] leading-relaxed">
          AI interview prep for <span class="font-semibold">{{ professionName }}</span>:
        </p>
        <ul class="mt-3 space-y-2 text-[15px] leading-relaxed text-muted">
          <li class="flex gap-3"><span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-accent"></span><span>the AI questions you are likely to get, with strong and weak answers;</span></li>
          <li class="flex gap-3"><span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-accent"></span><span>answers built from your own top tasks, with your own numbers;</span></li>
          <li *ngIf="mockOn" class="flex gap-3" data-test="prep-mock-bullet"><span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-accent"></span><span>a mock interview that scores your answers and tells you what to fix;</span></li>
          <li class="flex gap-3"><span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-accent"></span><span>a one-page AI work portfolio to print or save as PDF.</span></li>
        </ul>
        <p class="num mt-4 font-mono text-[13px] text-faint" data-test="prep-price">Free while in beta &middot; later &euro;29 one-off</p>

        <div class="mt-5 flex flex-wrap items-center gap-x-5 gap-y-3">
          <a routerLink="/prep" (click)="startBeta()" class="btn-accent" data-test="prep-start">
            Try the free beta <span class="arrow" aria-hidden="true">&rarr;</span>
          </a>
          <button *ngIf="state === 'idle' && !mockOn" type="button" (click)="open()" class="link text-[14px] text-muted" data-test="prep-open">
            Email me when the mock interview is ready
          </button>
        </div>

        <form *ngIf="state === 'form' || state === 'sending'" (ngSubmit)="submit()" #f="ngForm" class="mt-5 space-y-3" novalidate>
          <label class="block">
            <span class="kicker">Your email</span>
            <input name="email" type="email" [(ngModel)]="email" required maxlength="254" email
                   class="field mt-2" autocomplete="email" data-test="prep-email" />
          </label>
          <p class="text-[13px] leading-relaxed text-faint">
            We email you once when it is ready. Nothing else, and you can ask us to delete your address at any time
            (<a routerLink="/privacy" fragment="waitlist" class="link">privacy</a>).
          </p>
          <p *ngIf="error" class="text-[14px] text-band-very-high" role="alert">{{ error }}</p>
          <button type="submit" class="btn-accent" [disabled]="f.invalid || state === 'sending'" data-test="prep-submit">
            {{ state === 'sending' ? 'Saving…' : 'Join the list' }}
          </button>
        </form>

        <p *ngIf="state === 'done'" class="mt-5 rounded-xl bg-surface-2 p-4 text-[15px] font-semibold leading-relaxed" aria-live="polite" data-test="prep-done">
          You are on the list. We will email you when the mock interview is ready.
        </p>
      </div>
    </section>
  `
})
export class PrepWaitlistCardComponent implements OnInit {
  @Input({ required: true }) professionName = '';
  @Input() professionId: string | null = null;
  /** Where the card is shown, for the waitlist row and the events. */
  @Input() source = 'result';

  state: 'idle' | 'form' | 'sending' | 'done' = 'idle';
  email = '';
  error: string | null = null;

  /** The mock interview is live (the server has a language model key): then there is nothing to wait for. */
  mockOn = false;

  constructor(private waitlist: WaitlistService, private analytics: AnalyticsService,
              private mockStatus: MockInterviewStatusService, private destroyRef: DestroyRef) {}

  ngOnInit(): void {
    this.mockStatus.provider().pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(provider => (this.mockOn = provider !== null));
  }

  startBeta(): void {
    this.analytics.track('prep-cta-click', this.eventData());
  }

  open(): void {
    this.state = 'form';
    this.analytics.track('prep-waitlist-open', this.eventData());
  }

  submit(): void {
    if (this.state === 'sending') {
      return;
    }
    this.state = 'sending';
    this.error = null;
    this.waitlist.join(this.email, 'interview-prep', this.professionId, this.source).subscribe({
      next: () => {
        this.state = 'done';
        this.analytics.track('prep-waitlist-join', this.eventData());
      },
      error: (err: { status?: number }) => {
        this.state = 'form';
        this.error = err?.status === 429
          ? 'Too many sign-ups from here. Try again in an hour.'
          : 'Could not save your email. Check it and try again.';
      }
    });
  }

  private eventData(): Record<string, string> {
    return this.professionId ? { profession: this.professionId, source: this.source } : { source: this.source };
  }
}
