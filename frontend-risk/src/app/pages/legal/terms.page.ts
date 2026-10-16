import { Component, DestroyRef, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SaleConfig, saleConfig } from './legal-config';
import { BillingStatusService } from '../../services/billing-status.service';
import { WAIVER_LABEL, WAIVER_TEXT } from '../../models/billing.model';

/** Terms of use. Keep the description of the score in line with the home page and AiReadinessScorer. */
@Component({
  selector: 'app-terms-page',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <article class="legal mx-auto max-w-3xl pb-16 pt-4 lg:pt-10">
      <p class="kicker">Last updated 14 October 2026</p>
      <h1 class="display mt-4 text-[clamp(2.5rem,6vw,4.5rem)]">Terms of use</h1>

      <h2>About these terms</h2>
      <p>
        careerrisk.ee is provided by Kristo Erte, a private individual in Estonia. By using the site you
        agree to these terms. Contact: <a href="mailto:m6rnaja@gmail.com">m6rnaja&#64;gmail.com</a>.
      </p>

      <h2>What the test is, and what it is not</h2>
      <p>
        The result estimates how much of a profession's typical tasks today's AI tools can do: each task's share of the
        working week multiplied by how automatable it is, then adjusted by at most &plusmn;30 points from your answers. It
        is a catalog model for information and learning.
      </p>
      <ul>
        <li>It is not a forecast and not the probability that you will lose your job. A high score means those tasks will change, not that the job disappears.</li>
        <li>It is not career, legal, financial or employment advice. Decisions you make based on it are your own.</li>
        <li>Do not use it to make decisions about other people, such as hiring, firing or pay.</li>
      </ul>

      <h2>The service</h2>
      <p>
        The test is free and needs no account. We may change, pause or stop any part of the service at any time, and we do
        not promise that it is always available or free of errors.
      </p>

      <h2>Accounts</h2>
      <p>
        If you create an account, give accurate information and keep your password safe. An account is for one person.
        We may suspend or delete accounts that break these terms. You can ask us to delete your account at any time.
      </p>

      <h2>Acceptable use</h2>
      <ul>
        <li>Do not attack the site, try to get around its security or access other people's data.</li>
        <li>Do not use automated tools in a way that overloads the service.</li>
        <li>Do not use the service for anything unlawful.</li>
      </ul>
      <p>Linking to the site, quoting your own results and sharing them is welcome.</p>

      <h2>Data sources and content</h2>
      <p>
        Occupation and task data come from O*NET 29.3 (U.S. Department of Labor / ETA, CC BY 4.0). Task time shares and
        observed AI use come from the Anthropic Economic Index (CC BY). Human exposure labels come from Eloundou et al.
        (2024), <em>Science</em>. Automation estimates combine a language-model rating with those labels and are our own
        estimates, not those of the data providers. The rest of the site's text and design belong to careerrisk.ee.
      </p>

      <h2>AI-generated text</h2>
      <p>
        The optional AI comment and the occupation-specific practice projects are written with language models. They can be
        wrong or incomplete; treat them as suggestions.
      </p>

      <h2>Liability</h2>
      <p>
        The service is provided as it is. To the extent Estonian law allows, we are not liable for indirect loss or for
        decisions made on the basis of the results. Nothing in these terms limits liability that cannot be limited by law,
        including for intent or gross negligence, or your mandatory rights as a consumer.
      </p>

      <!-- FP-65 round 3 (drafts/terms-refund.md Part A). Rendered only when billing is on and the seller/VAT config is set. -->
<div *ngIf="!loaded" data-test="terms-sale-loading" aria-busy="true" role="status" class="mt-6 space-y-3">
        <span class="sr-only">Loading…</span>
        <div class="h-4 w-2/3 animate-pulse rounded bg-slate-800/60" aria-hidden="true"></div>
        <div class="h-4 w-full animate-pulse rounded bg-slate-800/60" aria-hidden="true"></div>
        <div class="h-4 w-5/6 animate-pulse rounded bg-slate-800/60" aria-hidden="true"></div>
      </div>
      <section *ngIf="sale as c" id="terms-of-sale" aria-labelledby="terms-of-sale-title" data-test="terms-of-sale">
      <h2 id="terms-of-sale-title">Terms of sale – Personal AI roadmap</h2>
      <p><em>Last updated: {{ c.lastUpdated }} · {{ waiverLabel }}</em></p>

      <h3>1. Who you are buying from</h3>
      <div class="overflow-x-auto">
        <table>
          <tbody>
            <tr><th scope="row">Seller</th><td>{{ c.sellerName }}</td></tr>
            <tr *ngIf="c.sellerRegCode"><th scope="row">Registry code</th><td>{{ c.sellerRegCode }}</td></tr>
            <tr *ngIf="c.sellerVatNumber"><th scope="row">VAT number</th><td>{{ c.sellerVatNumber }}</td></tr>
            <tr><th scope="row">Address</th><td>{{ c.sellerAddress }}</td></tr>
            <tr><th scope="row">Email</th><td><a [href]="'mailto:' + c.sellerEmail">{{ c.sellerEmail }}</a></td></tr>
            <tr><th scope="row">Website</th><td>careerrisk.ee</td></tr>
          </tbody>
        </table>
      </div>

      <h3>2. What the free service is and what you can buy</h3>
      <p>
        <strong>Free (no purchase needed):</strong> the careerrisk.ee test. You pick your profession (about 900 occupations,
        based on O*NET), answer 11 questions and get a result showing how much of the profession's typical tasks today's AI
        tools can do. As our existing terms say, the result is a catalog model for information and learning.
        <strong>It is not a forecast</strong> and not the probability that you will lose your job.
      </p>
      <p>
        <strong>Paid: "Personal AI roadmap" (one-off purchase, EUR 19).</strong> This is digital content, delivered online,
        for the profession you chose in the test. The free result and our job pages show phase 1 of the profession plan in
        full (title, steps and project), and the titles of phases 2 and 3. The purchase is about convenience: the same plan,
        paced and in one place, with tools to follow it. The purchase unlocks:
      </p>
      <ul>
        <li><strong>The full roadmap on one paced page:</strong> all 3 phases of the profession plan, each with a practice project, and the general 5-stage learning path paced by your weekly hours.</li>
        <li><strong>Plan view:</strong> the whole plan on one page, where you tick off steps and projects. Your ticks are saved <strong>in this browser</strong> only, not in your account.</li>
        <li><strong>PDF:</strong> a printable version of the plan that you can save as PDF using your browser's print function.</li>
        <li><strong>Calendar file (.ics):</strong> a weekly practice reminder you can add to your calendar.</li>
      </ul>
      <p>
        Access is tied to the account you used for the purchase. You need to log in before you pay. The plan is the same for
        everyone in a profession; your answers set where the general path starts and how it is paced. <em>Plan steps and
        practice projects are written with language models. They can be wrong or incomplete, so treat them as suggestions.</em>
      </p>
      <p>
        The roadmap is learning material. It is not career, legal, financial or employment advice, and it does not guarantee
        any job, income or outcome.
      </p>

      <h3>3. Price and payment</h3>
      <ul>
        <li><strong data-test="sale-price">Price: EUR 19 {{ c.vatLabel }}</strong>. This is the final total, shown before you pay.</li>
        <li><strong>One-off payment. This is not a subscription.</strong> Nothing renews and you will not be charged again.</li>
        <li>Payment is processed by <strong>Stripe</strong>. We never see or store your full card details. What we receive and keep is described in the <a routerLink="/privacy">privacy policy</a>.</li>
      </ul>

      <h3>4. Delivery</h3>
      <p>
        The roadmap is digital content that is <strong>not</strong> delivered on a physical medium. It is made available
        <strong>immediately</strong> after your payment is confirmed: on screen at careerrisk.ee, when you are logged in to the
        account you used for the purchase. If you cannot access it within 24 hours, contact us (see section 7).
      </p>

      <h3>5. Confirmation (durable medium)</h3>
      <p>
        After payment, Stripe sends a receipt email on our behalf to the address you gave at checkout. It contains the seller,
        the product, the price, the date, a link to these terms of sale and this text:
      </p>
      <p data-test="withdrawal-kept">You keep your right of withdrawal until we have sent you the confirmation of your consent.</p>
      <blockquote><p data-test="receipt-waiver"><strong>Waiver, {{ waiverLabel }}</strong> – <strong>&#9745; {{ waiverText }}</strong></p></blockquote>

      <h3>6. Right of withdrawal (14 days) and how it is lost</h3>
      <p>
        <strong>Your right.</strong> As a consumer in the EU you normally have the right to withdraw from an online purchase
        within <strong>14 days</strong> without giving a reason. For digital content not supplied on a physical medium, the
        14 days start on the day the contract is concluded (Directive 2011/83/EU art. 9; Võlaõigusseadus &sect; 49 lg 1&sup3;).
      </p>
      <p>
        <strong>When the right is lost.</strong> For digital content not supplied on a physical medium, you lose the right of
        withdrawal <strong>once supply has started</strong>, but only if you:
      </p>
      <ol>
        <li>gave your <strong>prior express consent</strong> to supply starting before the 14-day period ends, <strong>and</strong></li>
        <li><strong>acknowledged</strong> that you thereby lose your right of withdrawal,</li>
      </ol>
      <p>
        and we have sent you confirmation of this on a durable medium (Directive 2011/83/EU art. 16(m), as amended by
        Directive (EU) 2019/2161; Võlaõigusseadus &sect; 53 lg 4 p 7&sup1; and &sect; 55 lg 1–2).
      </p>
      <p>At checkout we ask for this with an unticked checkbox. <strong>You cannot buy without ticking it.</strong> The exact wording is:</p>
      <blockquote>
        <p data-test="terms-waiver"><strong>Waiver, {{ waiverLabel }}</strong> – <strong>&#9744; {{ waiverText }}</strong></p>
        <p>We store the waiver version and the time you ticked it with your order. The same text appears in your receipt (&sect;5).</p>
      </blockquote>
      <p>
        <strong>If supply has not started</strong> (for example, payment went through but the roadmap was never made
        available), you can still withdraw within 14 days. Use the form in Annex 1 or any clear statement sent to
        {{ c.sellerEmail }}. We refund the full amount within 14 days of receiving your notice, using the same payment method.
      </p>
      <p>
        Losing the right of withdrawal <strong>does not affect</strong> your rights if the content is missing or faulty
        (section 7 and the <a routerLink="/refund">refunds page</a>).
      </p>

      <h3>7. Faulty or missing content; complaints</h3>
      <p>
        See <a routerLink="/refund">/refund</a>. Your statutory rights for digital content that does not conform to the
        contract (Directive (EU) 2019/770; Võlaõigusseadus, digital content chapter) are not excluded or limited by these terms.
      </p>

      <h3>8. Complaints and dispute resolution</h3>
      <ul>
        <li>Contact us first at {{ c.sellerEmail }}. We reply within 5 working days and aim to resolve the issue within 14 days.</li>
        <li>If we cannot agree, you can apply to the <strong>Consumer Disputes Committee (Tarbijavaidluste komisjon)</strong> at the Consumer Protection and Technical Regulatory Authority (Tarbijakaitse ja Tehnilise Järelevalve Amet, TTJA): <a href="https://ttja.ee/tarbijavaidluste-komisjon" target="_blank" rel="noopener">ttja.ee/tarbijavaidluste-komisjon</a>. The procedure is free of charge for consumers.</li>
        <li>Consumers in other EU countries can find their national dispute resolution bodies here: <a href="https://consumer-redress.ec.europa.eu/dispute-resolution-bodies" target="_blank" rel="noopener">consumer-redress.ec.europa.eu/dispute-resolution-bodies</a></li>
        <li><em>(Note: the EU Online Dispute Resolution (ODR) platform closed on 20 July 2025 under Regulation (EU) 2024/3228, so we do not link to it.)</em></li>
        <li>You can also go to court.</li>
      </ul>

      <h3>9. Governing law</h3>
      <p>
        Estonian law applies. If you are a consumer, you keep the mandatory protections of the country where you live and
        can bring a claim there. (This matches the terms of use above.)
      </p>

      <h3>10. Contact</h3>
      <p>{{ c.sellerName }}, {{ c.sellerAddress }}, {{ c.sellerEmail }}.</p>

      <h3 id="withdrawal-form">Annex 1 – Model withdrawal form</h3>
      <p><em>(Directive 2011/83/EU Annex I(B). Relevant only where the right of withdrawal still applies, e.g. supply has not started.)</em></p>
      <blockquote>
        <p>To: {{ c.sellerName }}, {{ c.sellerAddress }}, {{ c.sellerEmail }}</p>
        <p>I hereby give notice that I withdraw from my contract for the following digital content: Personal AI roadmap (careerrisk.ee)</p>
        <p>Ordered on: ____________<br />Name of consumer: ____________<br />Email used at checkout: ____________<br />
          Address of consumer: ____________<br />Signature (only if this form is sent on paper): ____________<br />Date: ____________</p>
      </blockquote>
      </section>

      <h2>Changes and law</h2>
      <p>
        We may update these terms; the date at the top shows the current version. Estonian law applies. If you are a
        consumer, you keep the mandatory protections of the country where you live and can bring a claim there.
      </p>
      <p>How we handle personal data is described in the <a routerLink="/privacy">privacy policy</a>.</p>
    </article>
  `,
  styleUrl: './legal.css'
})
export class TermsPageComponent implements OnInit {
  /** Same constant as the paywall checkbox (FP-65: one versioned waiver text). */
  /** Terms of sale config; null hides the sale section (billing off or seller/VAT config missing). */
  /** null while loading and when billing is off or the seller/VAT config is incomplete. */
  sale: SaleConfig | null = null;
  /** /features has answered (true, false or failed); until then a neutral skeleton is shown. */
  loaded = false;

  constructor(private billingStatus: BillingStatusService, private destroyRef: DestroyRef) {}

  ngOnInit(): void {
    // the general content renders at once; the sale content appears once /features has answered
    this.billingStatus.billingEnabled().pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(on => {
        this.sale = saleConfig(on);
        this.loaded = true;
      });
  }
  readonly waiverText = WAIVER_TEXT;
  readonly waiverLabel = WAIVER_LABEL;
}
