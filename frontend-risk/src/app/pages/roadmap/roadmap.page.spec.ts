import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { RoadmapPageComponent } from './roadmap.page';
import { environment } from '../../../environments/environment';

const URL = `${environment.apiBaseUrl}/api/ai-readiness/roadmap`;

describe('RoadmapPageComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [RoadmapPageComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => {
    http.verify();
    sessionStorage.clear();
    localStorage.clear();
  });

  function render() {
    const f = TestBed.createComponent(RoadmapPageComponent);
    f.detectChanges();
    return f;
  }

  it('shows the locked card with a link back to the paywall on 402 payment_required', () => {
    localStorage.setItem('ai_readiness_last_answers', JSON.stringify({ professionId: 'onet-1', answers: { q1: 'a' } }));
    const f = render();
    const req = http.expectOne(URL);
    expect(req.request.body).toEqual({ professionId: 'onet-1', answers: { q1: 'a' } });
    req.flush({ error: 'payment_required' }, { status: 402, statusText: 'Payment Required' });
    f.detectChanges();
    const el = f.nativeElement as HTMLElement;
    expect(el.querySelector('[data-test="locked"]')).not.toBeNull();
    expect(el.querySelector('[data-test="to-paywall"]')!.getAttribute('href')).toBe('/ai-test?resume=paywall');
    // the answers are kept so the result page can restore the paywall
    expect(sessionStorage.getItem('ai_readiness_pending_purchase')).toContain('onet-1');
  });

  it('shows the empty state without saved answers and makes no request', () => {
    const f = render();
    expect((f.nativeElement as HTMLElement).querySelector('[data-test="empty"]')).not.toBeNull();
  });

  it('shows a retryable error for other failures and ready on success', () => {
    sessionStorage.setItem('ai_readiness_pending_purchase', JSON.stringify({ professionId: 'onet-1', answers: {} }));
    const f = render();
    http.expectOne(URL).flush(null, { status: 500, statusText: 'Server Error' });
    f.detectChanges();
    const el = f.nativeElement as HTMLElement;
    expect(el.querySelector('[data-test="error"]')).not.toBeNull();
    (el.querySelector('[data-test="error"] button') as HTMLButtonElement).click();
    http.expectOne(URL).flush({ generalRoadmap: [], professionRoadmap: [], totalWeeks: 3 });
    f.detectChanges();
    expect(el.querySelector('[data-test="ready"]')).not.toBeNull();
  });
});
