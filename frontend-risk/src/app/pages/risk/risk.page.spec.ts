import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { RiskPageComponent } from './risk.page';
import { environment } from '../../../environments/environment';

describe('RiskPageComponent CV upload wiring', () => {
  const CV_KEY = 'risk_cv_text';
  let fixture: ComponentFixture<RiskPageComponent>;
  let el: HTMLElement;
  let http: HttpTestingController;
  let savedCv: string | null;

  const cvTextarea = () => el.querySelector('#cv-text') as HTMLTextAreaElement;
  const analyzeButton = () =>
    Array.from(el.querySelectorAll('button')).find(b => b.textContent?.includes('Analyze My Job Match')) as HTMLButtonElement;
  const fileInput = () => el.querySelector('[data-testid="cv-upload-input"]') as HTMLInputElement;
  const pick = (file: File) => {
    const dt = new DataTransfer();
    dt.items.add(file);
    fileInput().files = dt.files;
    fileInput().dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };

  beforeEach(() => {
    savedCv = localStorage.getItem(CV_KEY);
    localStorage.setItem(CV_KEY, 'old cached CV');
    TestBed.configureTestingModule({
      imports: [RiskPageComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(RiskPageComponent);
    el = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => {
    http.verify();
    if (savedCv === null) localStorage.removeItem(CV_KEY);
    else localStorage.setItem(CV_KEY, savedCv);
  });

  it('renders the upload control under the CV field', () => {
    expect(cvTextarea().value).toBe('old cached CV');
    expect(el.querySelector('label[for="cv-upload-input"]')?.textContent?.trim()).toBe('Upload CV (PDF or .txt)');
  });

  it('disables Analyze while extracting, then replaces the CV text and persists it to localStorage', () => {
    fixture.componentInstance.form.controls.jobDescription.setValue('Senior Angular developer');
    fixture.detectChanges();
    expect(analyzeButton().disabled).toBeFalse();

    pick(new File(['Jane Doe'], 'jane.txt', { type: 'text/plain' }));
    expect(analyzeButton().disabled).toBeTrue();
    expect(cvTextarea().readOnly).toBeTrue();
    expect(el.textContent).toContain('Extracting text from your CV...');

    // Clicking Analyze while uploading must not start an analysis.
    fixture.componentInstance.runAssessment();
    http.expectNone(`${environment.apiBaseUrl}/api/job-analysis`);

    const req = http.expectOne(`${environment.apiBaseUrl}/api/cv/extract-text`);
    expect(req.request.method).toBe('POST');
    expect((req.request.body as FormData).get('file')).toEqual(jasmine.any(File));
    req.flush({ text: 'Jane Doe\nAngular developer' });
    fixture.detectChanges();

    expect(cvTextarea().value).toBe('Jane Doe\nAngular developer');
    expect(localStorage.getItem(CV_KEY)).toBe('Jane Doe\nAngular developer');
    expect(el.textContent).toContain('CV text replaced from jane.txt');
    expect(analyzeButton().disabled).toBeFalse();
    expect(cvTextarea().readOnly).toBeFalse();
  });

  it('re-enables Analyze and keeps the CV text after a failed upload', () => {
    fixture.componentInstance.form.controls.jobDescription.setValue('Senior Angular developer');
    pick(new File(['%PDF-1.4'], 'broken.pdf', { type: 'application/pdf' }));
    http.expectOne(`${environment.apiBaseUrl}/api/cv/extract-text`)
      .flush(null, { status: 500, statusText: 'Internal Server Error' });
    fixture.detectChanges();
    expect(cvTextarea().value).toBe('old cached CV');
    expect(localStorage.getItem(CV_KEY)).toBe('old cached CV');
    expect(el.textContent).toContain("We couldn't extract text from your CV.");
    expect(analyzeButton().disabled).toBeFalse();
  });
});
