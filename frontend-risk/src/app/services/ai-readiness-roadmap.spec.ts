import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AiReadinessService, roadmapTeaserLines } from './ai-readiness.service';
import { isPaymentRequired } from './billing.service';
import { environment } from '../../environments/environment';

describe('AiReadinessService paid roadmap', () => {
  let service: AiReadinessService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AiReadinessService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('POSTs professionId and answers to /api/ai-readiness/roadmap', () => {
    let weeks = 0;
    service.fullRoadmap('onet-1', { q1: 'a' }).subscribe(r => (weeks = r.totalWeeks));
    const req = http.expectOne(`${environment.apiBaseUrl}/api/ai-readiness/roadmap`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ professionId: 'onet-1', answers: { q1: 'a' } });
    req.flush({ generalRoadmap: [], professionRoadmap: [], totalWeeks: 12 });
    expect(weeks).toBe(12);
  });

  it('passes a 402 payment_required through as an error', () => {
    let paymentRequired = false;
    service.fullRoadmap('onet-1', {}).subscribe({ error: e => (paymentRequired = isPaymentRequired(e)) });
    http.expectOne(`${environment.apiBaseUrl}/api/ai-readiness/roadmap`)
      .flush({ error: 'payment_required' }, { status: 402, statusText: 'Payment Required' });
    expect(paymentRequired).toBeTrue();
  });

  it('keeps the pending purchase in sessionStorage and clears it', () => {
    expect(service.loadPendingPurchase()).toBeNull();
    service.savePendingPurchase({ professionId: 'onet-1', answers: { q1: 'a' } });
    expect(JSON.parse(sessionStorage.getItem(AiReadinessService.PENDING_PURCHASE_KEY)!).professionId).toBe('onet-1');
    expect(service.loadPendingPurchase()).toEqual({ professionId: 'onet-1', answers: { q1: 'a' } });
    service.clearPendingPurchase();
    expect(service.loadPendingPurchase()).toBeNull();
  });

  it('ignores a malformed pending purchase', () => {
    sessionStorage.setItem(AiReadinessService.PENDING_PURCHASE_KEY, '{"answers":{}}');
    expect(service.loadPendingPurchase()).toBeNull();
    sessionStorage.setItem(AiReadinessService.PENDING_PURCHASE_KEY, 'not json');
    expect(service.loadPendingPurchase()).toBeNull();
  });

  it('turns any teaser shape into display lines', () => {
    expect(roadmapTeaserLines(null)).toEqual([]);
    expect(roadmapTeaserLines('  Week 1: prompts ')).toEqual(['Week 1: prompts']);
    expect(roadmapTeaserLines(['a', ' ', 'b'])).toEqual(['a', 'b']);
    expect(roadmapTeaserLines([{ title: 'Phase 1' }, {}, 'x'])).toEqual(['Phase 1', 'x']);
  });
});
