import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PrepWaitlistCardComponent } from './prep-waitlist-card.component';
import { AnalyticsService } from '../../services/analytics.service';
import { environment } from '../../../environments/environment';

const URL = `${environment.apiBaseUrl}/api/waitlist`;

function setup() {
  TestBed.configureTestingModule({
    imports: [PrepWaitlistCardComponent], providers: [provideRouter([{ path: 'prep', children: [] }]), provideHttpClient(), provideHttpClientTesting()]
  });
  const analytics = TestBed.inject(AnalyticsService);
  const track = spyOn(analytics, 'track');
  const f = TestBed.createComponent(PrepWaitlistCardComponent);
  f.componentInstance.professionName = 'Accountants and Auditors';
  f.componentInstance.professionId = 'onet-13-2011-00';
  f.detectChanges();
  return { f, el: f.nativeElement as HTMLElement, http: TestBed.inject(HttpTestingController), track };
}

async function openAndType(f: ReturnType<typeof setup>['f'], el: HTMLElement, email: string) {
  (el.querySelector('[data-test="prep-open"]') as HTMLButtonElement).click();
  f.detectChanges();
  await f.whenStable();
  const input = el.querySelector('[data-test="prep-email"]') as HTMLInputElement;
  input.value = email;
  input.dispatchEvent(new Event('input'));
  f.detectChanges();
  await f.whenStable();
  f.detectChanges();
}

describe('PrepWaitlistCardComponent', () => {
  it('shows the offer, that it is a free beta with the later price, and links to /prep; no form until opened', () => {
    const { el, track } = setup();
    expect(el.textContent).toContain('Interview or review coming up?');
    expect(el.textContent).toContain('Accountants and Auditors');
    expect(el.querySelector('[data-test="prep-price"]')!.textContent).toContain('Free while in beta');
    const start = el.querySelector('[data-test="prep-start"]') as HTMLAnchorElement;
    expect(start.getAttribute('href')).toBe('/prep');
    start.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
    expect(track).toHaveBeenCalledWith('prep-cta-click', { profession: 'onet-13-2011-00', source: 'result' });
    expect(el.querySelector('[data-test="prep-email"]')).toBeNull();
  });

  it('opening the email list counts it; joining posts the email with product, profession and source, then thanks', async () => {
    const { f, el, http, track } = setup();
    await openAndType(f, el, ' pat@example.org ');
    expect(track).toHaveBeenCalledWith('prep-waitlist-open', { profession: 'onet-13-2011-00', source: 'result' });

    (el.querySelector('[data-test="prep-submit"]') as HTMLButtonElement).click();
    const req = http.expectOne(URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'pat@example.org', product: 'interview-prep', professionId: 'onet-13-2011-00', source: 'result' });
    req.flush(null, { status: 204, statusText: 'No Content' });
    f.detectChanges();

    expect(el.querySelector('[data-test="prep-done"]')!.textContent).toContain('You are on the list');
    expect(track).toHaveBeenCalledWith('prep-waitlist-join', { profession: 'onet-13-2011-00', source: 'result' });
  });

  it('an error keeps the form and says what to do; 429 says to wait', async () => {
    const { f, el, http, track } = setup();
    await openAndType(f, el, 'pat@example.org');

    (el.querySelector('[data-test="prep-submit"]') as HTMLButtonElement).click();
    http.expectOne(URL).flush({ error: 'rate_limited' }, { status: 429, statusText: 'Too Many Requests' });
    f.detectChanges();
    expect(el.querySelector('[role="alert"]')!.textContent).toContain('Try again in an hour');
    expect(el.querySelector('[data-test="prep-email"]')).not.toBeNull();

    f.detectChanges();
    await f.whenStable();
    (el.querySelector('[data-test="prep-submit"]') as HTMLButtonElement).click();
    http.expectOne(URL).flush(null, { status: 500, statusText: 'Server Error' });
    f.detectChanges();
    expect(el.querySelector('[role="alert"]')!.textContent).toContain('Could not save your email');
    expect(track).not.toHaveBeenCalledWith('prep-waitlist-join', jasmine.anything());
  });
});
