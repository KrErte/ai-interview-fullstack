import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { UploadCvComponent } from './upload-cv.component';
import { JobService } from '../../services/job.service';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../services/auth.service';

describe('UploadCvComponent', () => {
  let component: UploadCvComponent;
  let httpMock: HttpTestingController;

  const authStub = {
    getCurrentUserEmail: () => 'user@test.com'
  } as Partial<AuthService>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [UploadCvComponent, HttpClientTestingModule],
      providers: [{ provide: AuthService, useValue: authStub }]
    });

    const fixture = TestBed.createComponent(UploadCvComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should upload PDF and parse response', () => {
    const file = new File(['pdfdata'], 'cv.pdf', { type: 'application/pdf' });
    component.selectedFile = file;

    component.upload();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/cv/extract-text`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    const sentFile = (req.request.body as FormData).get('file') as File;
    expect(sentFile.name).toBe('cv.pdf');

    req.flush({
      text: 'Parsed text',
      headline: 'Engineer',
      skills: ['Angular'],
      experienceSummary: 'Summary'
    });

    expect(component.uploading).toBeFalse();
    expect(component.cvText).toBe('Parsed text');
    expect(component.headline).toBe('Engineer');
    expect(component.skills).toEqual(['Angular']);
    expect(component.experienceSummary).toBe('Summary');
    expect(component.message).toContain('successfully');
    expect(component.error).toBe('');
  });

  it('should handle upload error and stop loading', () => {
    const file = new File(['pdfdata'], 'cv.pdf', { type: 'application/pdf' });
    component.selectedFile = file;

    component.upload();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/cv/extract-text`);
    req.flush({ message: 'Upload failed' }, { status: 500, statusText: 'Server Error' });

    expect(component.uploading).toBeFalse();
    expect(component.error).toBe('Upload failed');
  });

  it('should not call backend when no file selected', () => {
    component.upload();

    httpMock.expectNone(`${environment.apiBaseUrl}/api/cv/extract-text`);
    expect(component.uploading).toBeFalse();
  });
});

