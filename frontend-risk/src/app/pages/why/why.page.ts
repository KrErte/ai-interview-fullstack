import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

/** The founder's story: why Futureproof exists. Personal text; change it only with the founder. */
@Component({
  selector: 'app-why-page',
  standalone: true,
  imports: [RouterModule],
  template: `
    <article class="mx-auto max-w-3xl pb-16 pt-4 lg:pt-10">
      <p class="kicker rise" style="--i:0">Why this exists</p>
      <h1 class="display rise mt-4 text-[clamp(2.5rem,6vw,4.5rem)]" style="--i:1">
        Why I built <span class="text-accent">Futureproof</span>
      </h1>

      <div class="rise mt-10 space-y-6 text-[clamp(1.1rem,1.6vw,1.3rem)] leading-relaxed text-muted" style="--i:2">
        <p class="text-fg">
          I was a software developer when I ended up in hospital for half a year. When I came back, AI had moved on so
          much that I had fallen behind, and I didn't know where to start.
        </p>
        <p>
          Then I looked at my friends. They work in completely different fields, and all of them were learning
          &ldquo;AI&rdquo;, but aimlessly, and each in a completely different way. Nobody had a plan that fitted their own job.
        </p>
        <p>
          That is where the idea came from: an AI roadmap for every profession. Not &ldquo;learn AI&rdquo; in general, but
          what AI already does in your work, what to learn first, and in what order.
        </p>
        <p>
          I believe AI will take over a large part of everyone's work, mine included. I don't think the answer is to wait
          or to worry. It is to learn to use AI in your own job, step by step.
        </p>
        <p>
          Futureproof is the starting point I wished I had. Pick your profession, answer 11 questions, and see which of
          your tasks are changing, where you stand, and a learning path for your job. It is free and you don't need an
          account.
        </p>
        <p class="text-fg">If you are where I was, behind and not sure where to start, this is for you.</p>
      </div>

      <div class="rise mt-10 border-t border-line pt-6" style="--i:3">
        <p class="font-semibold text-fg">Kristo Erte</p>
        <p class="kicker mt-1">Founder</p>
      </div>

      <div class="rise mt-10" style="--i:4">
        <a routerLink="/ai-test" class="btn-accent w-full sm:w-auto">
          Find your starting point
          <span class="arrow" aria-hidden="true">&rarr;</span>
        </a>
      </div>
    </article>
  `
})
export class WhyPageComponent {}
