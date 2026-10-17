import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PrepPageComponent } from './prep.page';
import { AiReadinessService } from '../../services/ai-readiness.service';
import { AnalyticsService } from '../../services/analytics.service';
import { AssessmentResult } from '../../models/ai-readiness.model';
import { environment } from '../../../environments/environment';

const RESULT = {
  professionId: 'accountant', professionName: 'Accountant',
  tasks: [
    { name: 'Bookkeeping entries and invoice processing', share: 30, automationPercent: 90 },
    { name: 'Reconciling bank statements and accounts', share: 20, automationPercent: 80 },
    { name: 'Reporting', share: 15, automationPercent: 70 },
    { name: 'Tax and regulatory advice', share: 20, automationPercent: 35 },
    { name: 'Working with clients', share: 15, automationPercent: 20 }
  ],
  humanEdge: ['Accountability and sign-off'], aiTools: ['Excel Copilot', 'AI invoice recognition']
} as unknown as AssessmentResult;

const QUESTIONS_URL = `${environment.apiBaseUrl}/api/ai-readiness/professions/accountant/interview-questions`;
const STATUS_URL = `${environment.apiBaseUrl}/api/ai-readiness/mock-interview/status`; // answered in setup()
const MOCK_URL = `${environment.apiBaseUrl}/api/ai-readiness/mock-interview`;
const SIX = [0, 1, 2, 3, 4, 5].map(i => ({ question: `Question ${i + 1}?`, lookFor: 'x', strongAnswer: 'y' }));

function setup(saved: AssessmentResult | null, provider: 'anthropic' | 'openai' | null = null) {
  localStorage.removeItem('ai_prep_drafts');
  TestBed.configureTestingModule({
    imports: [PrepPageComponent], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
  });
  spyOn(TestBed.inject(AiReadinessService), 'loadLastResult').and.returnValue(saved);
  const track = spyOn(TestBed.inject(AnalyticsService), 'track');
  const f = TestBed.createComponent(PrepPageComponent);
  f.detectChanges();
  const http = TestBed.inject(HttpTestingController);
  if (saved) {
    http.expectOne(STATUS_URL).flush({ provider });
    f.detectChanges();
  }
  return { f, el: f.nativeElement as HTMLElement, http, track };
}

async function type(f: ReturnType<typeof setup>['f'], el: HTMLElement, field: string, value: string) {
  const selector = field === 'mock-answer-field' ? '[data-test="mock-answer"]' : `[data-test="prep-${field}"]`;
  const input = el.querySelector(selector) as HTMLInputElement;
  input.value = value;
  input.dispatchEvent(new Event('input'));
  f.detectChanges();
  await f.whenStable();
  f.detectChanges();
}

describe('PrepPageComponent', () => {
  afterEach(() => localStorage.removeItem('ai_prep_drafts'));

  it('without a saved result asks for the test first and loads nothing', () => {
    const { el, http } = setup(null);
    expect(el.querySelector('[data-test="prep-no-result"]')!.textContent).toContain('Take the free test first');
    http.expectNone(QUESTIONS_URL);
  });

  it('shows the questions for the profession and three tasks with the most AI potential', async () => {
    const { f, el, http, track } = setup(RESULT);
    http.expectOne(QUESTIONS_URL).flush([{ question: 'How do you use AI in your work today?', lookFor: 'Purpose', strongAnswer: 'Name a tool' }]);
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
    expect(el.querySelector('[data-test="prep-questions"]')!.textContent).toContain('How do you use AI in your work today?');
    expect(el.querySelector('[data-test="prep-story-0"]')!.textContent).toContain('Bookkeeping entries and invoice processing');
    expect(el.querySelector('[data-test="prep-story-2"]')!.textContent).toContain('Reporting');
    expect(el.querySelector('[data-test="prep-story-3"]')).toBeNull();
    expect((el.querySelector('[data-test="prep-not-ai"]') as HTMLTextAreaElement).value).toContain('Working with clients');
    expect(track).toHaveBeenCalledWith('prep-open', { profession: 'accountant' });
    expect((el.querySelector('[data-test="prep-print"]') as HTMLButtonElement).disabled).toBeTrue();
  });

  it('builds the answer from the user input, puts it in the portfolio and keeps the draft in this browser', async () => {
    const { f, el, http, track } = setup(RESULT);
    http.expectOne(QUESTIONS_URL).flush([]);
    await f.whenStable();
    await type(f, el, 'tool-0', 'Excel Copilot');
    await type(f, el, 'before-0', '120');
    await type(f, el, 'after-0', '45');
    await type(f, el, 'check-0', 'I compare every total with the bank statement');

    const answer = el.querySelector('[data-test="prep-answer-0"]')!.textContent!;
    expect(answer).toContain('I use Excel Copilot for this part of my work: bookkeeping entries and invoice processing.');
    expect(answer).toContain('so I save around 1.3 hours each time.');
    expect(el.querySelector('[data-test="prep-portfolio"]')!.textContent).toContain('so I save around 1.3 hours each time.');
    expect((el.querySelector('[data-test="prep-print"]') as HTMLButtonElement).disabled).toBeFalse();
    expect(track).toHaveBeenCalledWith('prep-answer-done', { profession: 'accountant', count: 1 });

    const saved = JSON.parse(localStorage.getItem('ai_prep_drafts')!)['accountant'];
    expect(saved.stories[0].tool).toBe('Excel Copilot');
  });

  it('without the language model the mock interview says it is not switched on and sends nothing', async () => {
    const { f, el, http } = setup(RESULT);
    http.expectOne(QUESTIONS_URL).flush(SIX);
    f.detectChanges();
    expect(el.querySelector('[data-test="mock-off"]')).not.toBeNull();
    expect(el.querySelector('[data-test="mock-score"]')).toBeNull();
    http.expectNone(MOCK_URL);
  });

  it('with the language model: prefills Q1 from the story, scores it and shows the level, gaps and a stronger version', async () => {
    const { f, el, http, track } = setup(RESULT, 'anthropic');
    http.expectOne(QUESTIONS_URL).flush(SIX);
    f.detectChanges();
    await f.whenStable();
    expect((el.querySelector('[data-test="mock-answer"]') as HTMLTextAreaElement).value).toBe('');
    await type(f, el, 'tool-0', 'Excel Copilot');
    await type(f, el, 'before-0', '120');
    await type(f, el, 'after-0', '45');
    await type(f, el, 'check-0', 'I compare every total with the bank statement');

    const answer = el.querySelector('[data-test="mock-answer"]') as HTMLTextAreaElement;
    expect(answer.value).toContain('I use Excel Copilot for this part of my work');
    expect(el.querySelector('[data-test="prep-mock"]')!.textContent).toContain('sent to Anthropic (Claude) to be scored');
    (el.querySelector('[data-test="mock-score"]') as HTMLButtonElement).click();
    const req = http.expectOne(MOCK_URL);
    expect(req.request.body.professionId).toBe('accountant');
    expect(req.request.body.questionIndex).toBe(0);
    expect(req.request.body.answer).toContain('so I save around 1.3 hours');
    req.flush({ level: 'Adoptive', score: 3, strengths: ['Real tool'], missing: ['Say who uses the result'],
      improvedAnswer: 'I use Excel Copilot for reconciliations and save [minutes] each week.' });
    f.detectChanges();

    const result = el.querySelector('[data-test="mock-result"]')!.textContent!;
    expect(result).toContain('Level 3/4: Adoptive');
    expect(result).toContain('Say who uses the result');
    expect(el.querySelector('[data-test="mock-q-0"]')!.textContent).toContain('3/4');
    expect(track).toHaveBeenCalledWith('mock-score', { profession: 'accountant', question: 1, level: 'Adoptive' });

    (el.querySelector('[data-test="mock-use"]') as HTMLButtonElement).click();
    f.detectChanges();
    await f.whenStable();
    expect((el.querySelector('[data-test="mock-answer"]') as HTMLTextAreaElement).value).toContain('save [minutes] each week');
    const saved = JSON.parse(localStorage.getItem('ai_prep_drafts')!)['accountant'];
    expect(saved.mock.results[0].level).toBe('Adoptive');
  });

  it('a 503 or 429 from the mock interview gives a clear message and keeps the answer', async () => {
    const { f, el, http } = setup(RESULT, 'anthropic');
    http.expectOne(QUESTIONS_URL).flush(SIX);
    f.detectChanges();
    await f.whenStable();
    await type(f, el, 'mock-answer-field', 'I use ChatGPT to draft client emails and I always edit them myself.');

    (el.querySelector('[data-test="mock-score"]') as HTMLButtonElement).click();
    http.expectOne(MOCK_URL).flush({ error: 'ai_unavailable' }, { status: 503, statusText: 'Service Unavailable' });
    f.detectChanges();
    expect(el.querySelector('[role="alert"]')!.textContent).toContain('not available right now');

    (el.querySelector('[data-test="mock-score"]') as HTMLButtonElement).click();
    http.expectOne(MOCK_URL).flush({ error: 'rate_limited' }, { status: 429, statusText: 'Too Many Requests' });
    f.detectChanges();
    expect(el.querySelector('[role="alert"]')!.textContent).toContain('Try again in an hour');
    expect((el.querySelector('[data-test="mock-answer"]') as HTMLTextAreaElement).value).toContain('draft client emails');
  });
});
