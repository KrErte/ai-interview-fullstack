import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { WaitlistService } from '../../services/waitlist.service';
import { AnalyticsService } from '../../services/analytics.service';

/**
 * Fake door for the AI interview prep (docs/vajadus-ai-ajastul-2026-10-17.md): the offer, a price and an email
 * waitlist. Nothing is sold; the card says so. Clicks and sign-ups are counted to decide whether to build it.
 */
@Component({
  selector: 'app-prep-waitlist-card',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="card avoid-break grid gap-6 p-6 sm:p-8 lg:grid-cols-12 print:hidden" data-test="prep-card">
      <div class="lg:col-span-5 lg:pr-6">
        <p class="kicker !text-accent">Coming soon</p>
        <h2 class="mt-3 text-3xl font-bold leading-tight tracking-tight">Interview or review coming up?</h2>
        <p class="mt-3 text-[15px] leading-relaxed text-muted">
          More and more employers ask how you use AI in your work, in job interviews and in performance reviews.
        </p>
      </div>
      <div class="lg:col-span-7">
        <p class="text-[15px] leading-relaxed">
          We are building an AI interview prep for <span class="font-semibold">{{ professionName }}</span>:
        </p>
        <ul class="mt-3 space-y-2 text-[15px] leading-relaxed text-muted">
          <li class="flex gap-3"><span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-accent"></span><span>the AI questions you are likely to get, with strong and weak answers;</span></li>
          <li class="flex gap-3"><span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-accent"></span><span>short drills on your own top tasks, so you have real examples with your own numbers;</span></li>
          <li class="flex gap-3"><span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-accent"></span><span>a mock interview that scores your answers and tells you what to fix.</span></li>
        </ul>
        <p class="num mt-4 font-mono text-[13px] text-faint" data-test="prep-price">&euro;29 one-off &middot; not available yet</p>

        <button *ngIf="state === 'idle'" type="button" (click)="open()" class="btn-accent mt-5" data-test="prep-open">
          Get early access
        </button>

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
          You are on the list. We will email you when the prep for {{ professionName }} is ready.
        </p>
      </div>
    </section>
  `
})
export class PrepWaitlistCardComponent {
  @Input({ required: true }) professionName = '';
  @Input() professionId: string | null = null;
  /** Where the card is shown, for the waitlist row and the events. */
  @Input() source = 'result';

  state: 'idle' | 'form' | 'sending' | 'done' = 'idle';
  email = '';
  error: string | null = null;

  constructor(private waitlist: WaitlistService, private analytics: AnalyticsService) {}

  open(): void {
    this.state = 'form';
    this.analytics.track('prep-cta-click', this.eventData());
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
