import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

/**
 * Privacy policy. Every statement here describes what the code and the server actually do;
 * update this page together with any change to data collection (ops/VPS.md has the server side).
 */
@Component({
  selector: 'app-privacy-page',
  standalone: true,
  imports: [RouterModule],
  template: `
    <article class="legal mx-auto max-w-3xl pb-16 pt-4 lg:pt-10">
      <p class="kicker">Last updated 14 October 2026</p>
      <h1 class="display mt-4 text-[clamp(2.5rem,6vw,4.5rem)]">Privacy policy</h1>

      <section class="card mt-8 p-6 sm:p-7">
        <h2 class="!mt-0">The short version</h2>
        <ul>
          <li>You can take the test without an account, without an email address and without cookies.</li>
          <li>Your answers are scored on our server and are not stored there. Your last result is kept only in your browser.</li>
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
        to calculate the result. They are not saved in our database. Your last result is saved in your browser's local
        storage so you can open it again; clearing your browser data removes it.
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

      <h3>Analytics</h3>
      <p>
        We run Umami on our own server. It sets no cookies and does not store your IP address. It records which pages are
        visited, the referring site, browser, operating system, device type and country (derived from the IP address at the
        time of the visit), plus a few events: starting the test, finishing it (with the profession and result band) and
        clicking from a job page to the test. We use this to understand how the site is used. Legal basis: legitimate
        interest. Analytics data is not shared with anyone.
      </p>

      <h3>Server logs</h3>
      <p>
        For every request, the web server records the IP address, time, requested address and browser identification.
        We use these logs to keep the service secure and to fix errors. They are kept in size-limited files that are
        overwritten automatically and deleted whenever the service is updated. Legal basis: legitimate interest.
      </p>

      <h2>Cookies and browser storage</h2>
      <p>
        We use no cookies. We use your browser's local storage only for your last test result and, if you log in, your
        login session. Both are needed for features you use, so we do not ask for separate consent. You can delete them at
        any time in your browser settings.
      </p>

      <h2>Who else processes data</h2>
      <ul>
        <li><strong>Contabo GmbH</strong> (Germany) provides the server the service runs on.</li>
        <li><strong>OpenAI</strong> receives data only for the optional AI comment, as described above.</li>
      </ul>
      <p>Nobody else receives your data. We do not sell it and do not use it for advertising.</p>

      <h2>How long we keep data</h2>
      <ul>
        <li>Test answers: not stored on our server.</li>
        <li>Browser storage: until you clear it, or until a different person logs in on the same browser. Logging out alone does not remove your test result.</li>
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
export class PrivacyPageComponent {}
