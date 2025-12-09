import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { JobAnalysisComponent } from './job-analysis.component';
import { AuthService } from '../../services/auth.service';
import { environment } from '../../../environments/environment';

describe('JobAnalysisComponent', () => {
  let component: JobAnalysisComponent;
  let httpMock: HttpTestingController;

  const authStub = {
    getCurrentUserEmail: () => 'user@test.com'
  } as Partial<AuthService>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [JobAnalysisComponent, HttpClientTestingModule],
      providers: [{ provide: AuthService, useValue: authStub }]
    });

    const fixture = TestBed.createComponent(JobAnalysisComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should call backend with valid payload and set result', () => {
    component.form.patchValue({
      cvText: 'cv text',
      jobDescription: 'job description'
    });

    component.analyze();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/job-analysis`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      email: 'user@test.com',
      cvText: 'cv text',
      jobDescription: 'job description'
    });

    req.flush({ matchScore: 0.72, summary: 'Good fit' });

    expect(component.loading).toBeFalse();
    expect(component.error).toBe('');
    expect(component.result?.matchScore).toBe(0.72);
  });

  it('should not call backend when form invalid (missing jobDescription)', () => {
    component.form.patchValue({
      cvText: 'cv text',
      jobDescription: ''
    });

    component.analyze();

    httpMock.expectNone(`${environment.apiBaseUrl}/api/job-analysis`);
    expect(component.error).toBe('Please paste a job description.');
  });

  it('should handle backend error', () => {
    component.form.patchValue({
      cvText: 'cv text',
      jobDescription: 'job description'
    });

    component.analyze();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/job-analysis`);
    req.flush({ message: 'Analysis failed' }, { status: 500, statusText: 'Server Error' });

    expect(component.loading).toBeFalse();
    expect(component.error).toBe('Analysis failed');
  });
});

