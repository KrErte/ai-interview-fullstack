import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { TeamService } from '../../services/team.service';
import { TeamReport } from '../../models/team.model';
import { AnalyticsService } from '../../services/analytics.service';

const BAND_COLORS: Record<string, string> = {
  LOW: 'var(--band-low)',
  MODERATE: 'var(--band-moderate)',
  HIGH: 'var(--band-high)',
  VERY_HIGH: 'var(--band-very-high)'
};

/**
 * The private team report. The token comes from the URL fragment (/teams/report#token), so it never
 * reaches the server logs, analytics or a Referer header.
 */
@Component({
  selector: 'app-team-report-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <p *ngIf="loading" class="kicker py-20 text-center">Loading the report…</p>

    <section *ngIf="!loading && notFound" class="mx-auto max-w-2xl py-16">
      <h1 class="display text-[clamp(2rem,5vw,3.5rem)]">Report not found</h1>
      <p class="mt-5 text-[17px] leading-relaxed text-muted">
        This link has no valid report key. Check that you copied the whole report link, including the part after #.
        If the team was deleted, its report is gone for good.
      </p>
      <a routerLink="/teams" class="btn-ghost mt-8">Create a new team report</a>
    </section>

    <section *ngIf="!loading && deleted" class="mx-auto max-w-2xl py-16">
      <h1 class="display text-[clamp(2rem,5vw,3.5rem)]">Team deleted</h1>
      <p class="mt-5 text-[17px] leading-relaxed text-muted">The team and every answer in it have been deleted.</p>
    </section>

    <article *ngIf="!loading && report && !deleted" class="space-y-6">
      <header class="pb-2">
        <p class="kicker">Team AI report</p>
        <h1 class="display mt-3 text-[clamp(2rem,4.5vw,3.5rem)]">{{ report.teamName }}</h1>
        <p class="mt-3 text-[15px] text-muted">
          {{ report.respondents }} {{ report.respondents === 1 ? 'person' : 'people' }} answered
          <ng-container *ngIf="report.ready && report.firstDate">
            &middot; {{ report.firstDate }}{{ report.lastDate !== report.firstDate ? ' – ' + report.lastDate : '' }}
          </ng-container>
          &middot; up to {{ report.maxMembers }}
        </p>
      </header>

      <section class="card p-6 sm:p-8 print:hidden">
        <p class="kicker">Invite link for your team</p>
        <div class="mt-2 flex flex-col gap-2 sm:flex-row">
          <input readonly [value]="inviteUrl()" class="field font-mono !text-[13px]" aria-label="Invite link" />
          <button type="button" (click)="copyInvite()" class="btn-ghost shrink-0 !py-2.5">{{ copied ? 'Copied' : 'Copy' }}</button>
        </div>
      </section>

      <!-- Below the threshold: only the counter -->
      <section *ngIf="!report.ready" class="card p-6 sm:p-8">
        <p class="num text-6xl font-extrabold">{{ report.respondents }} <span class="text-faint">/ {{ report.minRespondents }}</span></p>
        <p class="mt-4 max-w-2xl text-[17px] leading-relaxed text-muted">
          {{ report.respondents }} of {{ report.minRespondents }} needed. The report opens when {{ report.minRespondents }} people
          have answered, so no one can be singled out.
        </p>
      </section>

      <ng-container *ngIf="report.ready">
        <!-- Key numbers -->
        <section class="grid gap-6 sm:grid-cols-3">
          <div class="card p-6"><p class="kicker">Average replaceability</p><p class="num mt-3 text-6xl font-extrabold">{{ report.averageReplaceability }}</p></div>
          <div class="card p-6"><p class="kicker">Average task exposure</p><p class="num mt-3 text-6xl font-extrabold">{{ report.averageTaskExposure }}</p></div>
          <div class="card p-6"><p class="kicker">Average AI readiness</p><p class="num mt-3 text-6xl font-extrabold">{{ report.averageReadiness }}</p></div>
        </section>

        <!-- Bands -->
        <section class="card avoid-break p-6 sm:p-8">
          <h2 class="text-2xl font-bold tracking-tight">Exposure bands</h2>
          <div class="mt-5 flex h-3 gap-1 overflow-hidden rounded-full" aria-hidden="true">
            <span *ngFor="let b of report.bands" class="basis-0" [style.flex-grow]="b.percent" [style.background]="bandColor(b.key)"></span>
          </div>
          <div class="mt-3 flex flex-wrap gap-x-6 gap-y-1 text-[15px]">
            <span *ngFor="let b of report.bands"><span class="font-semibold">{{ b.label }}</span> <span class="num ml-1 font-mono text-muted">{{ b.percent }}%</span></span>
          </div>
        </section>

        <!-- Focus -->
        <section *ngIf="report.focus.length" class="card avoid-break border-l-4 !border-l-accent p-6 sm:p-8">
          <h2 class="text-2xl font-bold tracking-tight">Suggested training focus</h2>
          <ol class="mt-4 space-y-3 text-[16px] leading-relaxed">
            <li *ngFor="let f of report.focus; let i = index" class="flex gap-3"><span class="font-mono text-accent">{{ i + 1 }}</span><span>{{ f }}</span></li>
          </ol>
        </section>

        <!-- Top tasks -->
        <section class="card p-6 sm:p-8">
          <h2 class="text-2xl font-bold tracking-tight">Where the team's time changes most</h2>
          <p class="mt-2 text-[15px] text-muted">Share of the whole team's working time AI could take on through each task. Only tasks that at least {{ report.minRespondents }} people have.</p>
          <p *ngIf="!report.topTasks.length" class="mt-4 text-[15px] text-muted">No task is shared by {{ report.minRespondents }} or more people yet.</p>
          <div *ngFor="let t of report.topTasks" class="grid grid-cols-[minmax(0,1fr)_auto] gap-4 border-b border-line py-3 last:border-b-0">
            <p class="text-[15px] leading-snug">
              {{ t.name }}
              <span *ngIf="t.observedUse" class="observed-pill">AI already used</span>
            </p>
            <p class="num text-right font-mono text-[13px] text-muted">{{ t.teamTimePercent }}% of team time &middot; {{ t.averageAutomation }}% automatable</p>
          </div>
        </section>

        <!-- AI use -->
        <section class="grid gap-6 lg:grid-cols-3">
          <div *ngFor="let q of report.aiUse" class="card avoid-break p-6">
            <h3 class="text-[17px] font-bold leading-snug">{{ q.question }}</h3>
            <ul class="mt-4 space-y-2">
              <li *ngFor="let o of q.options">
                <div class="flex justify-between gap-3 text-[14px]"><span>{{ o.label }}</span><span class="num font-mono text-muted">{{ o.percent }}%</span></div>
                <div class="mt-1 h-1.5 overflow-hidden rounded-full bg-line"><div class="h-full rounded-full bg-accent" [style.width.%]="o.percent"></div></div>
              </li>
            </ul>
          </div>
        </section>

        <!-- Readiness and stages -->
        <section class="grid gap-6 lg:grid-cols-2">
          <div class="card avoid-break p-6">
            <h2 class="text-2xl font-bold tracking-tight">AI readiness levels</h2>
            <ul class="mt-4 space-y-2">
              <li *ngFor="let l of report.readinessLevels" class="flex justify-between gap-3 border-b border-line pb-2 text-[15px]">
                <span>{{ l.label }}</span><span class="num font-mono text-muted">{{ l.percent }}%</span>
              </li>
            </ul>
          </div>
          <div class="card avoid-break p-6">
            <h2 class="text-2xl font-bold tracking-tight">Where people start on the learning path</h2>
            <ul class="mt-4 space-y-2">
              <li *ngFor="let s of report.stages" class="flex justify-between gap-3 border-b border-line pb-2 text-[15px]">
                <span>{{ s.label }}</span><span class="num font-mono text-muted">{{ s.count }} {{ s.count === 1 ? 'person' : 'people' }}</span>
              </li>
            </ul>
          </div>
        </section>

        <!-- Professions -->
        <section *ngIf="report.professions.length" class="card avoid-break p-6 sm:p-8">
          <h2 class="text-2xl font-bold tracking-tight">By profession</h2>
          <p class="mt-2 text-[15px] text-muted">Only groups of {{ report.minRespondents }} or more people.</p>
          <div class="mt-4 grid grid-cols-[minmax(0,1fr)_4rem_7rem_7rem] gap-x-4 gap-y-2 text-[15px]">
            <span class="kicker">Profession</span><span class="kicker text-right">People</span><span class="kicker text-right">Replaceability</span><span class="kicker text-right">Readiness</span>
            <ng-container *ngFor="let p of report.professions">
              <span>{{ p.name }}</span>
              <span class="num text-right font-mono">{{ p.respondents }}</span>
              <span class="num text-right font-mono">{{ p.averageReplaceability }}</span>
              <span class="num text-right font-mono">{{ p.averageReadiness }}</span>
            </ng-container>
          </div>
        </section>

        <!-- Article 4 note and method -->
        <section class="grid gap-4 pt-2 lg:grid-cols-12">
          <p class="kicker lg:col-span-3">AI literacy record</p>
          <p class="text-[14px] leading-relaxed lg:col-span-9">
            This report records an AI literacy baseline assessment of {{ report.respondents }} people
            on {{ report.firstDate }}{{ report.lastDate !== report.firstDate ? ' – ' + report.lastDate : '' }}, using the
            careerrisk.ee task-exposure method. It is one step an organisation can document under the EU AI Act,
            Article 4; it does not certify that the obligation is met.
          </p>
          <p class="kicker lg:col-span-3">Method</p>
          <p class="text-[13px] leading-relaxed text-muted lg:col-span-9">
            Each person's score is the share of their profession's typical tasks (or their own task shares) multiplied by
            the estimated automation with today's AI tools, adjusted by at most ±30 points from their answers. It is a
            catalog model, not a labour-market forecast. A high score means tasks will change, not that a job disappears.
            The report shows totals only, opens at {{ report.minRespondents }} answers and leaves out groups and tasks of
            fewer than {{ report.minRespondents }} people. Sources: O*NET 29.3 (U.S. Department of Labor / ETA, CC BY 4.0),
            Anthropic Economic Index (CC BY), Eloundou et al. (2024), Science.
          </p>
        </section>
      </ng-container>

      <div class="flex flex-col gap-3 border-t border-line pt-6 sm:flex-row sm:items-center sm:justify-between print:hidden">
        <div>
          <button *ngIf="!confirmDelete" type="button" (click)="confirmDelete = true" class="link text-[14px] text-band-very-high">
            Delete team and all data
          </button>
          <div *ngIf="confirmDelete" class="flex flex-wrap items-center gap-3 text-[14px]" role="alert">
            <span>Delete the team and every answer? This cannot be undone.</span>
            <button type="button" (click)="remove()" [disabled]="deleting" class="btn-ghost !border-band-very-high !px-4 !py-2 !text-band-very-high">
              {{ deleting ? 'Deleting…' : 'Yes, delete' }}
            </button>
            <button type="button" (click)="confirmDelete = false" class="link text-muted">Cancel</button>
          </div>
          <p *ngIf="error" class="mt-2 text-[14px] text-band-very-high">{{ error }}</p>
        </div>
        <button *ngIf="report.ready" type="button" (click)="print()" class="btn-accent">Print / save as PDF</button>
      </div>
    </article>
  `
})
export class TeamReportPageComponent implements OnInit {
  report: TeamReport | null = null;
  loading = true;
  notFound = false;
  deleted = false;
  confirmDelete = false;
  deleting = false;
  copied = false;
  error: string | null = null;
  private token = '';

  constructor(private teams: TeamService, private analytics: AnalyticsService) {}

  ngOnInit(): void {
    this.token = decodeURIComponent(window.location.hash.replace(/^#/, '')).trim();
    if (!this.token) {
      this.loading = false;
      this.notFound = true;
      return;
    }
    this.teams.report(this.token).subscribe({
      next: report => {
        this.report = report;
        this.loading = false;
        // never the token or the team code: only whether the report is open yet
        this.analytics.track('team-report-viewed', { ready: report.ready ? 'yes' : 'no' });
      },
      error: () => {
        this.loading = false;
        this.notFound = true;
      }
    });
  }

  /** The invite link on this site, so it works on any host the report is opened on. */
  inviteUrl(): string {
    return this.report ? `${window.location.origin}/ai-test?team=${this.report.code}` : '';
  }

  copyInvite(): void {
    navigator.clipboard?.writeText(this.inviteUrl()).then(() => (this.copied = true)).catch(() => (this.copied = false));
  }

  bandColor(key: string): string {
    return BAND_COLORS[key] ?? 'var(--line)';
  }

  print(): void {
    this.analytics.track('print', { page: 'team-report' });
    window.print();
  }

  remove(): void {
    this.deleting = true;
    this.error = null;
    this.teams.delete(this.token).subscribe({
      next: () => {
        this.deleting = false;
        this.deleted = true;
      },
      error: () => {
        this.deleting = false;
        this.error = 'Could not delete the team. Please try again.';
      }
    });
  }
}
