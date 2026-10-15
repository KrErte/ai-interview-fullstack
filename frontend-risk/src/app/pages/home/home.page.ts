import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

/** Bands as in AiReadinessScorer.band(): the scale on this page shows the real cut-offs. */
const SCALE = [
  { label: 'Low', range: '0–29', from: 0, to: 30, color: 'var(--band-low)' },
  { label: 'Moderate', range: '30–49', from: 30, to: 50, color: 'var(--band-moderate)' },
  { label: 'High', range: '50–69', from: 50, to: 70, color: 'var(--band-high)' },
  { label: 'Very high', range: '70–100', from: 70, to: 100, color: 'var(--band-very-high)' }
];

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <!-- Hero -->
    <section class="grid gap-10 pb-14 pt-4 lg:grid-cols-12 lg:gap-8 lg:pb-20 lg:pt-10">
      <div class="lg:col-span-8">
        <p class="rise inline-flex items-center gap-2 rounded-full border border-line px-3 py-1.5" style="--i:0">
          <span class="h-1.5 w-1.5 rounded-full bg-accent"></span>
          <span class="kicker !text-fg">Free &middot; 11 questions &middot; No account</span>
        </p>
        <h1 class="display rise mt-7 text-[clamp(3rem,8vw,9.5rem)]" style="--i:1">
          The AI roadmap for <span class="text-accent">your job.</span>
        </h1>
      </div>

      <div class="flex flex-col justify-end lg:col-span-4">
        <p class="rise max-w-md text-[18px] leading-relaxed text-muted" style="--i:2">
          Everyone is learning AI, most of us at random. Pick your profession, answer 11 questions and see where to
          start: which of your tasks AI is already changing, and what to learn first.
        </p>
        <div class="rise mt-8" style="--i:3">
          <a routerLink="/ai-test" class="btn-accent w-full sm:w-auto">
            Find your starting point
            <span class="arrow" aria-hidden="true">&rarr;</span>
          </a>
          <!-- plain href: /jobs pages are rendered by the backend, not by Angular -->
          <a href="/jobs" class="mt-4 block text-[15px] text-muted underline-offset-4 hover:text-fg hover:underline sm:ml-4 sm:mt-0 sm:inline">
            Browse 900+ jobs
          </a>
        </div>
      </div>
    </section>

    <!-- The scale: uses the real band thresholds -->
    <section class="card rise p-6 sm:p-8" style="--i:4" aria-labelledby="scale-title">
      <div class="flex flex-wrap items-baseline justify-between gap-2">
        <h2 id="scale-title" class="kicker !text-fg">The replaceability scale</h2>
        <p class="kicker">0 = nothing automatable &middot; 100 = all of it</p>
      </div>

      <div class="mt-6 flex h-14 gap-1 sm:h-20">
        <div *ngFor="let b of scale; let i = index"
             class="grow-x h-full basis-0 rounded-lg"
             [style.--i]="i"
             [style.flex-grow]="b.to - b.from"
             [style.background]="b.color"></div>
      </div>

      <div class="mt-3 flex gap-1">
        <div *ngFor="let b of scale" [style.flex-grow]="b.to - b.from" class="min-w-0 basis-0">
          <p class="truncate text-[15px] font-semibold sm:text-lg">{{ b.label }}</p>
          <p class="num font-mono text-[11px] text-faint">{{ b.range }}</p>
        </div>
      </div>
    </section>

    <!-- What you get -->
    <section class="mt-6 grid gap-6 sm:grid-cols-3">
      <article *ngFor="let f of features; let i = index" class="card rise p-6 sm:p-7" [style.--i]="5 + i">
        <p class="font-mono text-sm text-accent">{{ '0' + (i + 1) }}</p>
        <h3 class="mt-4 text-2xl font-bold tracking-tight">{{ f.title }}</h3>
        <p class="mt-3 text-[15px] leading-relaxed text-muted">{{ f.body }}</p>
      </article>
    </section>

    <!-- Why this exists: the founder's story, full text on /why -->
    <section class="card rise mt-6 grid gap-6 p-6 sm:p-8 lg:grid-cols-12 lg:items-center" style="--i:8">
      <p class="kicker lg:col-span-2">Why this exists</p>
      <p class="text-[clamp(1.2rem,2.1vw,1.6rem)] font-semibold leading-snug tracking-tight lg:col-span-7">
        &ldquo;I was a software developer when I ended up in hospital for half a year. When I came back, I had fallen
        behind on AI and didn't know where to start.&rdquo;
      </p>
      <div class="lg:col-span-3 lg:text-right">
        <a routerLink="/why" class="btn-ghost w-full sm:w-auto">
          Why I built this
          <span class="arrow" aria-hidden="true">&rarr;</span>
        </a>
      </div>
    </section>

    <!-- Method + closing CTA -->
    <section class="mt-20 grid gap-8 border-t border-line pt-10 lg:mt-28 lg:grid-cols-12">
      <p class="kicker lg:col-span-3">How the score works</p>
      <div class="lg:col-span-6">
        <p class="text-[clamp(1.35rem,2.4vw,1.85rem)] font-semibold leading-snug tracking-tight">
          The share of your profession's typical tasks, multiplied by how much of each today's AI tools can do,
          then adjusted by at most <span class="text-accent">&plusmn;30 points</span> from your answers.
        </p>
        <p class="mt-4 text-[15px] leading-relaxed text-muted">
          In the spirit of the OpenAI/UPenn &ldquo;GPTs are GPTs&rdquo; and ILO exposure studies. A high score means those
          tasks will change, not that the job disappears.
        </p>
        <p class="mt-4 text-[13px] leading-relaxed text-faint">
          Sources: O*NET 29.3 (U.S. Department of Labor / ETA, CC BY 4.0), Anthropic Economic Index (CC BY),
          Eloundou et al. (2024), <em>Science</em>.
        </p>
      </div>
      <div class="lg:col-span-3 lg:text-right">
        <a routerLink="/ai-test" class="btn-ghost w-full sm:w-auto">
          Find your starting point
          <span class="arrow" aria-hidden="true">&rarr;</span>
        </a>
      </div>
    </section>
  `
})
export class HomePageComponent {
  readonly scale = SCALE;

  readonly features = [
    {
      title: 'Where you stand',
      body: '11 questions about how you work and how you already use AI. You get a replaceability score and your AI readiness, so you know your starting point.'
    },
    {
      title: 'What changes in your work',
      body: 'Each typical task in your profession, its share of the working week, how much of it AI can already do, and where people already use AI for it.'
    },
    {
      title: 'What to learn, in order',
      body: 'A general AI learning path that starts at your level, plus a starter plan with practice projects for your profession.'
    }
  ];
}
