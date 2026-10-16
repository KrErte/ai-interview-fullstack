import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

/**
 * Refunds and withdrawal (FP-65 draft, drafts/terms-refund.md Part B). DRAFT: needs Olen/legal review;
 * the [PLACEHOLDERS] stay visible until they are decided.
 */
@Component({
  selector: 'app-refund-page',
  standalone: true,
  imports: [RouterModule],
  template: `
    <article class="legal mx-auto max-w-3xl pb-16 pt-4 lg:pt-10">
      <p class="kicker">Last updated: [DATE]</p>
      <h1 class="display mt-4 text-[clamp(2.5rem,6vw,4.5rem)]">Refunds and withdrawal</h1>
      <p data-test="placeholder">
        <strong>Short version:</strong> the personal AI roadmap is a EUR 19 one-off digital purchase. It is delivered
        immediately, and at checkout you agree to that and acknowledge losing the 14-day right of withdrawal once delivery
        starts. You still get a fix, a new delivery or a refund if something goes wrong on our side.
      </p>

      <h2>1. If you did not receive the roadmap, or it is faulty</h2>
      <p>
        Examples: the payment went through but the roadmap never appeared; the download link doesn't work; the content is
        blank, broken, for the wrong profession, or otherwise not what was described.
      </p>
      <p>What we do:</p>
      <ol>
        <li><strong>Fix or re-deliver</strong> within a reasonable time, at no cost to you, normally within [3 working days].</li>
        <li>If we cannot fix it, or do not fix it within a reasonable time, or the problem is serious: a <strong>full refund of EUR 19</strong>, paid within <strong>14 days</strong> of our decision (and no later than 14 days after you tell us that you end the contract), to the same payment method.</li>
        <li>If we never deliver the roadmap, you can end the contract and get a full refund.</li>
      </ol>
      <p>These are your rights under Directive (EU) 2019/770 and Estonian law. Nothing on this page limits them.</p>

      <h2>2. If supply never started</h2>
      <p>You can withdraw within 14 days of purchase and get a full refund (see <a routerLink="/terms">Terms, section 6</a>).</p>

      <h2>3. Other refunds</h2>
      <p>
        Once the roadmap has been delivered without a fault, there is no automatic refund just because you changed your
        mind, because you agreed at checkout to immediate delivery and acknowledged losing the right of withdrawal.
        [OPTIONAL – Olen decides: "If you are unhappy for any reason, email us within [14] days and we will refund you as a
        goodwill gesture." A voluntary guarantee can help sales but it is a business decision.]
      </p>

      <h2>4. How to ask for a refund</h2>
      <p>Email <strong>[EMAIL]</strong> with the subject "Refund" and include:</p>
      <ul>
        <li>the email address you used at checkout,</li>
        <li>the date of purchase and, if you have it, the receipt / Stripe payment ID,</li>
        <li>what went wrong (a screenshot helps).</li>
      </ul>
      <p>
        We confirm we have received it within [2 working days]. Refunds go through Stripe to your original payment method.
        Your bank may take a few more days to show it.
      </p>

      <h2>5. Complaints</h2>
      <p>If you are not satisfied with our answer, see <a routerLink="/terms">Terms section 8</a> (Tarbijavaidluste komisjon / TTJA).</p>
    </article>
  `,
  styleUrl: './legal.css'
})
export class RefundPageComponent {}
