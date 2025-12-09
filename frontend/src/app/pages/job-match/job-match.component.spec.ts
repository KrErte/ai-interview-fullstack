import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { JobMatchComponent } from './job-match.component';
import { JobService } from '../../services/job.service';
import { AuthService } from '../../services/auth.service';
import { environment } from '../../../environments/environment';

describe('JobMatchComponent', () => {
  let component: JobMatchComponent;
  let httpMock: HttpTestingController;

  const authStub = {
    getCurrentUserEmail: () => 'user@test.com'
  } as Partial<AuthService>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [JobMatchComponent, HttpClientTestingModule],
      providers: [{ provide: AuthService, useValue: authStub }]
    });

    const fixture = TestBed.createComponent(JobMatchComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should submit match request with correct payload and map results', () => {
    component.form.patchValue({
      targetRole: 'Engineer',
      location: 'Remote',
      cvText: 'cv text',
      jobDescription: 'job desc',
      minScore: 50
    });

    component.analyze();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/job-match`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      cvText: 'cv text',
      jobDescription: 'job desc',
      targetRole: 'Engineer',
      email: 'user@test.com'
    });

    req.flush([
      { jobTitle: 'Engineer', matchScore: 0.8, summary: 'Great fit' }
    ]);

    expect(component.loading).toBeFalse();
    expect(component.error).toBe('');
    expect(component.matches.length).toBeGreaterThan(0);
    expect(component.selectedMatch?.result.matchScore).toBe(0.8);
  });

  it('should set error on backend failure', () => {
    component.form.patchValue({
      jobDescription: 'job desc'
    });

    component.analyze();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/job-match`);
    req.flush({ message: 'Server error' }, { status: 500, statusText: 'Server Error' });

    expect(component.loading).toBeFalse();
    expect(component.error).toBe('Server error');
  });

  it('should block submit when form invalid (missing jobDescription)', () => {
    component.form.patchValue({
      jobDescription: ''
    });

    component.analyze();

    httpMock.expectNone(`${environment.apiBaseUrl}/api/job-match`);
    expect(component.error).toBe('Add a job description to run the analysis.');
  });
});

