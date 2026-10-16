import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

/** Terms of use. Keep the description of the score in line with the home page and AiReadinessScorer. */
@Component({
  selector: 'app-terms-page',
  standalone: true,
  imports: [RouterModule],
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
export class TermsPageComponent {}
