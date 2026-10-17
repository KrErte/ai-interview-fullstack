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

function setup(saved: AssessmentResult | null) {
  localStorage.removeItem('ai_prep_drafts');
  TestBed.configureTestingModule({
    imports: [PrepPageComponent], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
  });
  spyOn(TestBed.inject(AiReadinessService), 'loadLastResult').and.returnValue(saved);
  const track = spyOn(TestBed.inject(AnalyticsService), 'track');
  const f = TestBed.createComponent(PrepPageComponent);
  f.detectChanges();
  return { f, el: f.nativeElement as HTMLElement, http: TestBed.inject(HttpTestingController), track };
}

async function type(f: ReturnType<typeof setup>['f'], el: HTMLElement, field: string, value: string) {
  const input = el.querySelector(`[data-test="prep-${field}"]`) as HTMLInputElement;
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
});
