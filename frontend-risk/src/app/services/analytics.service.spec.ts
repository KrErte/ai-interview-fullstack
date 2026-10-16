import { TestBed } from '@angular/core/testing';
import { AnalyticsService } from './analytics.service';

describe('AnalyticsService', () => {
  const w = window as unknown as { umami?: unknown };
  let service: AnalyticsService;

  beforeEach(() => {
    delete w.umami;
    service = TestBed.inject(AnalyticsService);
  });
  afterEach(() => delete w.umami);

  it('calls window.umami.track with the event and data when Umami is loaded', () => {
    const track = jasmine.createSpy('track');
    w.umami = { track };
    service.track('paywall_view', { profession: 'x' });
    expect(track).toHaveBeenCalledOnceWith('paywall_view', { profession: 'x' });
  });

  it('passes undefined data through when none is given', () => {
    const track = jasmine.createSpy('track');
    w.umami = { track };
    service.track('checkout_start');
    expect(track).toHaveBeenCalledOnceWith('checkout_start', undefined);
  });

  it('is a no-op without window.umami', () => {
    expect(() => service.track('paywall_view')).not.toThrow();
  });

  it('is a no-op when umami has no track function or track throws', () => {
    w.umami = {};
    expect(() => service.track('paywall_view')).not.toThrow();
    w.umami = { track: () => { throw new Error('blocked'); } };
    expect(() => service.track('paywall_view')).not.toThrow();
  });
});
