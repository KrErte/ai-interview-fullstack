import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

/** Placeholder until Growth delivers the refund policy (FP-63). */
@Component({
  selector: 'app-refund-page',
  standalone: true,
  imports: [RouterModule],
  template: `
    <article class="legal mx-auto max-w-3xl pb-16 pt-4 lg:pt-10">
      <h1 class="display mt-4 text-[clamp(2.5rem,6vw,4.5rem)]">Refund policy</h1>
      <p data-test="placeholder">Refund policy: final text coming soon.</p>
      <p>Questions in the meantime: <a href="mailto:m6rnaja@gmail.com">m6rnaja&#64;gmail.com</a>. See also the <a routerLink="/terms">terms of use</a>.</p>
    </article>
  `,
  styleUrl: './legal.css'
})
export class RefundPageComponent {}
