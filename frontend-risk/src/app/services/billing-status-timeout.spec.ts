import { TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { BillingStatusService } from './billing-status.service';
import { AuthService } from './auth.service';
import { TermsPageComponent } from '../pages/legal/terms.page';
import { RefundPageComponent } from '../pages/legal/refund.page';
import { AiCheckPageComponent } from '../pages/ai-check/ai-check.page';
import { environment } from '../../environments/environment';

const API = `${environment.apiBaseUrl}/api/ai-readiness`;

describe('/features never answers (5 s timeout = billing off)', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
        { provide: AuthService, useValue: { isLoggedIn: () => false } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { fragment: null, queryParamMap: convertToParamMap({}) }, fragment: of(null), queryParamMap: of(convertToParamMap({})) }
        }
      ]
    });
    http = TestBed.inject(HttpTestingController);
  });

  it('billing is off after 5 s, not before', fakeAsync(() => {
    let v: boolean | undefined;
    TestBed.inject(BillingStatusService).billingEnabled().subscribe(x => (v = x));
    http.expectOne(`${API}/features`); // never flushed
    tick(4999);
    expect(v).toBeUndefined();
    tick(1);
    expect(v).toBeFalse();
  }));

  it('/terms and /refund resolve (skeleton gone, no sale content)', fakeAsync(() => {
    const t = TestBed.createComponent(TermsPageComponent);
    const r = TestBed.createComponent(RefundPageComponent);
    t.detectChanges();
    r.detectChanges();
    expect(http.match(`${API}/features`).length).toBe(1); // one shared, cached request
    expect((r.nativeElement as HTMLElement).querySelector('[data-test="refund-loading"]')).not.toBeNull();
    tick(5000);
    t.detectChanges();
    r.detectChanges();
    const te = t.nativeElement as HTMLElement;
    const re = r.nativeElement as HTMLElement;
    expect(te.querySelector('[data-test="terms-sale-loading"]')).toBeNull();
    expect(te.querySelector('[data-test="terms-of-sale"]')).toBeNull();
    expect(re.querySelector('[data-test="refund-loading"]')).toBeNull();
    expect(re.querySelector('[data-test="refund-no-sale"]')).not.toBeNull();
  }));

  it('/ai-test leaves the loading state and the test can start', fakeAsync(() => {
    const f = TestBed.createComponent(AiCheckPageComponent);
    f.detectChanges();
    http.expectOne(`${API}/professions`).flush([{ id: 'onet-1', name: 'Accountant', category: 'Business', featured: true }]);
    http.expectOne(`${API}/questions`).flush([]);
    http.expectOne(`${API}/professions/categories`).flush([]);
    http.expectOne(`${API}/features`); // hangs
    tick(1000);
    expect(f.componentInstance.loadingCatalog).toBeTrue();
    tick(4000);
    f.detectChanges();
    const c = f.componentInstance;
    expect(c.loadingCatalog).toBeFalse();
    expect(c.error).toBeNull();
    expect(c.aiCommentAvailable).toBeFalse();
    expect(c.serverBillingEnabled).toBeFalse();
    c.selectProfession(c.featured[0]);
    expect(c.step).toBe('questions');
    discardPeriodicTasks();
  }));
});
