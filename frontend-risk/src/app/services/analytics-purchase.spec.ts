import { AnalyticsService } from './analytics.service';

describe('AnalyticsService.trackPurchaseOnce', () => {
  let svc: AnalyticsService;
  let track: jasmine.Spy;
  beforeEach(() => {
    sessionStorage.clear();
    svc = new AnalyticsService();
    track = jasmine.createSpy('track');
    (window as unknown as { umami?: unknown }).umami = { track };
  });
  afterEach(() => {
    delete (window as unknown as { umami?: unknown }).umami;
    sessionStorage.clear();
  });

  it('sends once per session id', () => {
    expect(svc.trackPurchaseOnce('cs_1')).toBeTrue();
    expect(svc.trackPurchaseOnce('cs_1')).toBeFalse();
    expect(svc.trackPurchaseOnce('cs_2')).toBeTrue();
    expect(track).toHaveBeenCalledTimes(2);
  });

  it('uses a fallback key without a session id', () => {
    svc.trackPurchaseOnce(null);
    svc.trackPurchaseOnce('');
    expect(track).toHaveBeenCalledTimes(1);
    expect(sessionStorage.getItem('fp_purchase_tracked:no-session-id')).toBe('1');
  });

  it('no-op (and nothing stored) without Umami', () => {
    delete (window as unknown as { umami?: unknown }).umami;
    expect(svc.trackPurchaseOnce('cs_1')).toBeFalse();
    expect(sessionStorage.length).toBe(0);
  });
});
