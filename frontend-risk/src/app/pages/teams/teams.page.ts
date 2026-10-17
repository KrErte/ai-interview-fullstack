import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { TeamService } from '../../services/team.service';
import { CreatedTeam } from '../../models/team.model';
import { AnalyticsService } from '../../services/analytics.service';

const CONTACT = 'm6rnaja@gmail.com';

/** Demo numbers for the sample report on the sales page; not real data. */
const SAMPLE = {
  respondents: 23,
  replaceability: 48,
  readiness: 41,
  bands: [
    { label: 'Low', percent: 17, color: 'var(--band-low)' },
    { label: 'Moderate', percent: 39, color: 'var(--band-moderate)' },
    { label: 'High', percent: 30, color: 'var(--band-high)' },
    { label: 'Very high', percent: 14, color: 'var(--band-very-high)' }
  ],
  tasks: [
    { name: 'Prepare and check routine reports', time: 6.1 },
    { name: 'Answer customer questions by email', time: 4.8 },
    { name: 'Enter and reconcile data in the ERP', time: 4.2 }
  ],
  focus: [
    'Start with AI for: routine reports; customer email; data reconciliation.',
    'Basic AI literacy session for everyone: 52% of the team do not use AI at work every week.'
  ]
};

@Component({
  selector: 'app-teams-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <!-- Hero -->
    <section class="grid gap-10 pb-12 lg:grid-cols-12 lg:gap-8">
      <div class="lg:col-span-8">
        <p class="rise inline-flex items-center gap-2 rounded-full border border-line px-3 py-1.5" style="--i:0">
          <span class="h-1.5 w-1.5 rounded-full bg-accent"></span>
          <span class="kicker !text-fg">For teams &middot; EU AI Act, Article 4</span>
        </p>
        <h1 class="display rise mt-7 text-[clamp(2.6rem,6.5vw,6.5rem)]" style="--i:1">
          Know where your team <span class="text-accent">stands with AI.</span>
        </h1>
      </div>
      <div class="flex flex-col justify-end lg:col-span-4">
        <p class="rise max-w-md text-[17px] leading-relaxed text-muted" style="--i:2">
          Since February 2025, organisations that use AI must take measures for sufficient AI literacy of their staff
          (EU AI Act, Article 4). Your team takes the same free 11-question test, anonymously. You get one report: which
          tasks AI is changing, how ready people are, and where training should start.
        </p>
        <button type="button" (click)="scrollToCreate()" class="btn-accent rise mt-8 w-full sm:w-auto" style="--i:3">
          Create a team report <span class="arrow" aria-hidden="true">&rarr;</span>
        </button>
      </div>
    </section>

    <!-- How it works -->
    <section class="grid gap-6 sm:grid-cols-3">
      <article *ngFor="let s of steps; let i = index" class="card p-6 sm:p-7">
        <p class="font-mono text-sm text-accent">{{ '0' + (i + 1) }}</p>
        <h2 class="mt-4 text-2xl font-bold tracking-tight">{{ s.title }}</h2>
        <p class="mt-3 text-[15px] leading-relaxed text-muted">{{ s.body }}</p>
      </article>
    </section>

    <!-- Anonymity -->
    <section class="card mt-6 grid gap-6 p-6 sm:p-8 lg:grid-cols-12">
      <div class="lg:col-span-4">
        <p class="kicker !text-accent">Anonymous by design</p>
        <h2 class="mt-3 text-3xl font-bold leading-tight tracking-tight">You see the team, never a person.</h2>
      </div>
      <ul class="space-y-3 text-[15px] leading-relaxed lg:col-span-8">
        <li class="flex gap-3"><span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-accent"></span><span>No accounts, names or emails for the people who answer. We store only the test content and the date.</span></li>
        <li class="flex gap-3"><span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-accent"></span><span>The report opens at 5 answers. Before that you see only the count.</span></li>
        <li class="flex gap-3"><span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-accent"></span><span>Groups and tasks under 5 people are merged or left out, so no one can be singled out.</span></li>
        <li class="flex gap-3"><span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-accent"></span><span>The report talks about tasks and skills, never about whether someone will lose their job, and never ranks people.</span></li>
        <li class="flex gap-3"><span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-accent"></span><span>You can delete the team and every answer from the report page. See the <a routerLink="/privacy" fragment="teams" class="underline">privacy policy</a>.</span></li>
      </ul>
    </section>

    <!-- Sample report -->
    <section class="card mt-6 p-6 sm:p-8" aria-labelledby="sample-title">
      <div class="flex flex-wrap items-baseline justify-between gap-2">
        <h2 id="sample-title" class="text-3xl font-bold tracking-tight">Sample report</h2>
        <p class="kicker">Demo data</p>
      </div>
      <div class="mt-6 grid gap-6 lg:grid-cols-12">
        <div class="grid grid-cols-3 gap-4 lg:col-span-5">
          <div><p class="kicker">People</p><p class="num mt-2 text-4xl font-extrabold">{{ sample.respondents }}</p></div>
          <div><p class="kicker">Replaceability</p><p class="num mt-2 text-4xl font-extrabold">{{ sample.replaceability }}</p></div>
          <div><p class="kicker">AI readiness</p><p class="num mt-2 text-4xl font-extrabold">{{ sample.readiness }}</p></div>
        </div>
        <div class="lg:col-span-7">
          <p class="kicker">Exposure bands</p>
          <div class="mt-3 flex h-3 gap-1 overflow-hidden rounded-full">
            <span *ngFor="let b of sample.bands" [style.flex-grow]="b.percent" [style.background]="b.color" class="basis-0"></span>
          </div>
          <div class="mt-2 flex flex-wrap gap-x-4 text-[13px] text-muted">
            <span *ngFor="let b of sample.bands">{{ b.label }} {{ b.percent }}%</span>
          </div>
        </div>
        <div class="lg:col-span-6">
          <p class="kicker">Where the team's time changes most</p>
          <ul class="mt-3 space-y-2 text-[15px]">
            <li *ngFor="let t of sample.tasks" class="flex justify-between gap-4 border-b border-line pb-2">
              <span>{{ t.name }}</span><span class="num font-mono text-muted">{{ t.time }}% of team time</span>
            </li>
          </ul>
        </div>
        <div class="lg:col-span-6">
          <p class="kicker">Suggested training focus</p>
          <ul class="mt-3 space-y-2 text-[15px] leading-relaxed">
            <li *ngFor="let f of sample.focus" class="flex gap-3"><span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-accent"></span><span>{{ f }}</span></li>
          </ul>
        </div>
      </div>
    </section>

    <!-- Prices -->
    <section class="mt-6" aria-labelledby="prices-title">
      <h2 id="prices-title" class="text-3xl font-bold tracking-tight">Prices</h2>
      <p class="mt-2 text-[15px] text-muted">Paid by invoice. The personal test stays free for everyone.</p>
      <div class="mt-6 grid gap-6 md:grid-cols-3">
        <article class="card flex flex-col p-6">
          <p class="kicker !text-accent">Pilot</p>
          <p class="num mt-3 text-4xl font-extrabold">490 &euro;</p>
          <p class="mt-3 text-[15px] leading-relaxed text-muted">Up to 50 people, one report.</p>
        </article>
        <article class="card flex flex-col p-6">
          <p class="kicker !text-accent">Team</p>
          <p class="num mt-3 text-4xl font-extrabold">1 490 &euro;</p>
          <p class="mt-3 text-[15px] leading-relaxed text-muted">Up to 200 people, one report.</p>
        </article>
        <article class="card flex flex-col p-6 opacity-80">
          <p class="kicker">Annual &middot; coming later</p>
          <p class="num mt-3 text-4xl font-extrabold">6 &euro;</p>
          <p class="mt-3 text-[15px] leading-relaxed text-muted">Per person per year, with a quarterly re-test. Not available yet.</p>
        </article>
      </div>
      <p class="mt-4 text-[13px] text-faint">Prices are in euros. VAT is stated on the invoice.</p>
    </section>

    <!-- Create -->
    <section id="create" class="card mt-6 grid scroll-mt-24 gap-8 p-6 sm:p-8 lg:grid-cols-12">
      <div class="lg:col-span-5">
        <h2 class="text-3xl font-bold leading-tight tracking-tight">Create a team report</h2>
        <p class="mt-3 text-[15px] leading-relaxed text-muted">
          You get two links: one to share with your team and a private one for the report. No account needed.
          A new report holds up to 50 people (Pilot). We send the invoice by email; for up to 200 people, write to us.
        </p>
        <a [href]="mailto" class="btn-ghost mt-6 w-full sm:w-auto">Email us</a>
      </div>

      <div class="lg:col-span-7">
        <form *ngIf="!created" (ngSubmit)="create()" #f="ngForm" class="space-y-4" novalidate>
          <label class="block">
            <span class="kicker">Team or company name</span>
            <input name="name" [(ngModel)]="name" required maxlength="120" class="field mt-2" autocomplete="organization" />
          </label>
          <label class="block">
            <span class="kicker">Your email (for the invoice only)</span>
            <input name="email" type="email" [(ngModel)]="email" required maxlength="254" email class="field mt-2" autocomplete="email" />
          </label>
          <p class="text-[13px] leading-relaxed text-faint">
            We use this email only for the invoice and to contact you about this report. It is never shown to your team.
          </p>
          <p *ngIf="error" class="text-[14px] text-band-very-high" role="alert">{{ error }}</p>
          <button type="submit" class="btn-accent" [disabled]="f.invalid || creating">
            {{ creating ? 'Creating…' : 'Create the report' }}
          </button>
        </form>

        <div *ngIf="created" class="space-y-5" aria-live="polite">
          <p class="rounded-xl border border-band-high bg-surface-2 p-4 text-[15px] font-semibold leading-relaxed">
            Save the report link. It is the only key to your report, and we cannot show it again.
          </p>
          <div>
            <p class="kicker">Invite link for your team</p>
            <div class="mt-2 flex flex-col gap-2 sm:flex-row">
              <input readonly [value]="created.inviteUrl" class="field font-mono !text-[13px]" aria-label="Invite link" />
              <button type="button" (click)="copy(created.inviteUrl, 'invite')" class="btn-ghost shrink-0 !py-2.5">
                {{ copied === 'invite' ? 'Copied' : 'Copy' }}
              </button>
            </div>
          </div>
          <div>
            <p class="kicker">Private report link (for you only)</p>
            <div class="mt-2 flex flex-col gap-2 sm:flex-row">
              <input readonly [value]="created.reportUrl" class="field font-mono !text-[13px]" aria-label="Report link" />
              <button type="button" (click)="copy(created.reportUrl, 'report')" class="btn-ghost shrink-0 !py-2.5">
                {{ copied === 'report' ? 'Copied' : 'Copy' }}
              </button>
            </div>
          </div>
          <p class="text-[14px] leading-relaxed text-muted">
            Share the invite link with your team. The report opens when 5 people have answered.
          </p>
          <a [href]="created.reportUrl" class="btn-accent w-full sm:w-auto">Open the report <span class="arrow" aria-hidden="true">&rarr;</span></a>
        </div>
      </div>
    </section>

    <p class="mt-10 text-[13px] leading-relaxed text-faint">
      The report records an AI literacy baseline. It does not certify that your organisation meets Article 4, and it is
      not a labour-market forecast.
    </p>
  `
})
export class TeamsPageComponent {
  readonly sample = SAMPLE;
  readonly mailto = `mailto:${CONTACT}?subject=${encodeURIComponent('Team AI report')}`;
  readonly steps = [
    { title: 'Create a report', body: 'Give it a name and your email. You get an invite link and a private report link at once.' },
    { title: 'Share the invite', body: 'People take the usual free test through the link and see their own result. Their answer is added anonymously.' },
    { title: 'Read and print', body: 'From 5 answers: averages, exposure bands, the tasks where the team’s time changes most, AI use and a training focus.' }
  ];

  name = '';
  email = '';
  creating = false;
  error: string | null = null;
  created: CreatedTeam | null = null;
  copied: 'invite' | 'report' | null = null;

  constructor(private teams: TeamService, private analytics: AnalyticsService) {}

  create(): void {
    if (this.creating) {
      return;
    }
    this.creating = true;
    this.error = null;
    this.teams.create(this.name.trim(), this.email.trim()).subscribe({
      next: created => {
        this.created = created;
        this.creating = false;
        this.analytics.track('team-created');
      },
      error: (err: { status?: number }) => {
        this.error = err?.status === 429
          ? 'Too many team reports were created. Try again in an hour.'
          : 'Could not create the report. Check the name and email and try again.';
        this.creating = false;
      }
    });
  }

  scrollToCreate(): void {
    document.getElementById('create')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  copy(text: string, which: 'invite' | 'report'): void {
    navigator.clipboard?.writeText(text).then(() => (this.copied = which)).catch(() => (this.copied = null));
  }
}
