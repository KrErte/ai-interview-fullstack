import { Component, DestroyRef, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SaleConfig, saleConfig } from './legal-config';
import { BillingStatusService } from '../../services/billing-status.service';

/**
 * Privacy policy. Every statement here describes what the code and the server actually do;
 * update this page together with any change to data collection (ops/VPS.md has the server side).
 * The payment parts show only while the server sells something (same rule as the terms of sale).
 */
@Component({
  selector: 'app-privacy-page',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <article class="legal mx-auto max-w-3xl pb-16 pt-4 lg:pt-10">
      <p class="kicker">Last updated 17 October 2026</p>
      <h1 class="display mt-4 text-[clamp(2.5rem,6vw,4.5rem)]">Privacy policy</h1>

      <section class="card mt-8 p-6 sm:p-7">
        <h2 class="!mt-0">The short version</h2>
        <ul>
          <li>You can take the test without an account, without an email address and without cookies.</li>
          <li>Your answers are scored on our server and are not stored there. Your last result is kept only in your browser.</li>
          <li>If you take the test through a team invite link, your answers are stored anonymously for that team's totals. Your employer sees only totals for groups of 5 or more people.</li>
          <li>We use self-hosted, cookieless analytics. We do not sell data and we do not show ads.</li>
        </ul>
      </section>

      <h2>Who is responsible</h2>
      <p>
        careerrisk.ee is run by Kristo Erte, a private individual in Estonia, who is the controller of your
        personal data. Contact: <a href="mailto:m6rnaja@gmail.com">m6rnaja&#64;gmail.com</a>.
      </p>

      <h2>What we process and why</h2>

      <h3>Taking the test</h3>
      <p>
        Your profession, your answers to the 11 questions and, if you change them, your task shares are sent to our server
        to calculate the result. They are not saved in our database, unless you take the test through a team invite link
        (see "Team reports" below). Your last result is saved in your browser's local storage so you can open it
        again; clearing your browser data removes it. Logging out does not remove it. If you copy a share link, it contains your profession, the ids of your
        answers and your task shares (not the AI comment), so anyone with the link can see that result. This is in the part
        of the link after #, which browsers do not send to our server, and we do not store it.
      </p>

      <h3>Optional AI comment</h3>
      <p>
        When this option is available and you tick it, we send OpenAI the profession, your scores, the three most
        automatable tasks, the human-edge points and the labels of your answers, so that a language model can write a short
        comment. Your name and email address are not sent. OpenAI may process this data outside the European Economic Area,
        including in the United States. Under OpenAI's API terms, data sent through the API is not used to train its models
        and is kept for up to 30 days for abuse monitoring. Legal basis: your consent, which you give by ticking the box.
      </p>

      <h3>Account (optional)</h3>
      <p>
        If you create an account, we store your name, email address, your password as a one-way hash (we cannot read it)
        and the date the account was created. If you use account-only tools, we also store what you enter there, for
        example CV text or a job description, linked to your account. Your browser keeps your login session, email and name
        in local storage until you log out. Legal basis: providing the service you asked for. We keep account data until you
        ask us to delete it.
      </p>

      <h3 id="teams">Team reports</h3>
      <p>
        An organisation can create a team report and share an invite link with its staff. If you take the test through
        such a link, we store your profession, your answers, your scores, your task shares and the date (not the time) for
        that team. We do not store your name, email address, IP address or any browser identifier with it. Your browser
        keeps a random key so it can update your own answer, for example when you move the task share sliders; that key is
        not linked to you on our side.
      </p>
      <p>
        The person who created the team sees only totals, and only once at least 5 people have answered. Groups of
        professions and tasks with fewer than 5 people are merged or left out, and no single answer is ever shown or
        returned by our API. The report never ranks people or says that anyone will lose their job.
      </p>
      <p>
        For these team answers, the organisation that created the team is the controller and careerrisk.ee is its
        processor: we process the answers only to produce that team's report, under a data processing agreement. The
        organisation's legal basis is its own (for example its obligation under Article 4 of the EU AI Act to ensure AI
        literacy, or its legitimate interest). The data is kept until the team is deleted, which the team's creator can do at
        any time from the report page, or until the organisation asks us to delete it. If you want your answer removed,
        contact the organisation or us.
      </p>
      <p>
        For the person who creates a team, we store the team name, their email address (for the invoice and to contact them
        about the report) and the creation date. careerrisk.ee is the controller for this contact data. Legal basis: taking
        steps to enter into and performing a contract. The email address is never shown to the people who answer. The
        report link is the only key to the report; we store only a one-way hash of it.
      </p>

      <ng-container *ngIf="sale as c">
        <h3 id="payments" data-test="privacy-payments">Buying the full roadmap</h3>
        <p>
          To buy the full roadmap you need an account. For each order we store a purchase record linked to your account:
          your account email address, the product, the amount and currency charged, the Stripe Checkout session id, the
          payment status, that you agreed to get the digital content at once (with the date and the version of that
          text), the country of your billing address as reported by Stripe, refund details if there is a refund, and the
          dates of the order and the payment. The seller, {{ c.sellerName }}, is the controller for this data. Legal
          basis: performing the contract and keeping the accounting records the law requires.
        </p>
        <p>
          The payment is processed by Stripe. We send Stripe your email address, an internal id of your account and the
          order number. You enter your card or other payment details on Stripe's own page; we never see or store them.
          Stripe handles payment data under its own
          <a href="https://stripe.com/privacy" target="_blank" rel="noopener">privacy policy</a>, may process it outside
          the European Economic Area, and is itself responsible for its own legal duties, such as fraud prevention.
        </p>
      </ng-container>

      <h3>Analytics</h3>
      <p>
        We run Umami on our own server. It sets no cookies and does not store your IP address. It records which pages are
        visited, the referring site, browser, operating system, device type and country (derived from the IP address at the
        time of the visit), plus a few events: starting the test, finishing it (with the profession and result band),
        clicking from a job page to the test, a job search that finds nothing (only the field you picked, never
        the text you typed), ticking the AI comment box, the first move of a task share slider, printing, downloading the weekly
        reminder, ticking a roadmap step, and for team reports: creating a team, opening an invite link, adding an answer
        and viewing the report (never the team code, the report key or anyone's answers). We use this to understand how
        the site is used. Legal basis: legitimate interest. Analytics data is not shared with anyone.
      </p>

      <h3>Server logs</h3>
      <p>
        For every request, the web server records the IP address, time, requested address and browser identification.
        We use these logs to keep the service secure and to fix errors. They are kept in size-limited files that are
        overwritten automatically and deleted whenever the service is updated. Legal basis: legitimate interest.
      </p>

      <h2>Cookies and browser storage</h2>
      <p>
        We use no cookies. We use your browser's local storage only for features you use: your last test result with its
        answers and task shares, the roadmap steps you tick, the tasks you mark as already done with AI, the key to your own
        team answer if you answered through a team link and, if you log in, your login session. We do not ask for separate
        consent for this. You can delete it at any time in your browser settings.
      </p>

      <h2>Who else processes data</h2>
      <ul>
        <li><strong>Contabo GmbH</strong> (Germany) provides the server the service runs on.</li>
        <li><strong>OpenAI</strong> receives data only for the optional AI comment, as described above.</li>
        <li *ngIf="sale"><strong>Stripe</strong> processes payments for the full roadmap, as described above.</li>
      </ul>
      <p>Nobody else receives your data. We do not sell it and do not use it for advertising.</p>

      <h2>How long we keep data</h2>
      <ul>
        <li>Test answers: not stored on our server, except answers given through a team invite link, which are kept until the team is deleted.</li>
        <li>Team contact data (team name and the creator's email): until the team is deleted, and longer only if an invoice requires it under accounting law.</li>
        <li>Browser storage: until you clear it, or until a different person logs in on the same browser. Logging out alone does not remove your test result.</li>
        <li *ngIf="sale" data-test="privacy-purchase-retention">Purchase records: as long as accounting law requires, also if you delete your account.</li>
        <li>Account data: until you ask us to delete it. Nightly database backups are kept for 14 days, so deleted data also disappears from backups within 14 days.</li>
        <li>Server logs: as described above.</li>
      </ul>

      <h2>Your rights</h2>
      <p>
        Under the GDPR you can ask for access to your data, correction, deletion, restriction of processing or a copy in a
        portable format, and you can object to processing based on legitimate interest. Where processing is based on
        consent, you can withdraw it at any time. To use these rights, or to delete your account, email
        <a href="mailto:m6rnaja@gmail.com">m6rnaja&#64;gmail.com</a>. We reply within one month.
      </p>
      <p>
        You can also complain to the Estonian Data Protection Inspectorate
        (<a href="https://www.aki.ee" target="_blank" rel="noopener">aki.ee</a>) or to the data protection authority in
        your own country.
      </p>

      <h2>Children</h2>
      <p>The service is not aimed at children under 13. Please do not create an account if you are under 13.</p>

      <h2>Security</h2>
      <p>
        The site is served only over HTTPS. Passwords are stored as hashes, the database cannot be reached from the
        internet, and the server accepts only key-based administrator logins. No system is perfectly secure, but we keep
        the data we hold to a minimum.
      </p>

      <h2>Changes</h2>
      <p>
        If we change how we handle data, we update this page and the date at the top. See also our
        <a routerLink="/terms">terms of use</a>.
      </p>
    </article>
  `,
  styleUrl: './legal.css'
})
export class PrivacyPageComponent implements OnInit {
  /** Seller config while billing is on; null hides the payment parts (billing off, unknown or seller config missing). */
  sale: SaleConfig | null = null;

  constructor(private billingStatus: BillingStatusService, private destroyRef: DestroyRef) {}

  ngOnInit(): void {
    this.billingStatus.billingEnabled().pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(on => (this.sale = saleConfig(on)));
  }
}
