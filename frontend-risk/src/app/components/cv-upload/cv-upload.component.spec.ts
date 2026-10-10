import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject } from 'rxjs';

import {
  CvUploadComponent,
  CV_ACCEPT,
  CV_MAX_BYTES,
  CV_UPLOAD_COPY,
  cvUploadErrorMessage,
  formatBytes,
  hasAllowedCvExtension,
  validateCvFile
} from './cv-upload.component';
import { RiskService } from '../../services/risk.service';
import { CvUploadResponse } from '../../models/risk.model';

describe('cv-upload helpers', () => {
  it('formats sizes with 1024-based units', () => {
    expect(formatBytes(CV_MAX_BYTES)).toBe('1 MB');
    expect(formatBytes(1.5 * 1024 * 1024)).toBe('1.5 MB');
    expect(formatBytes(2048)).toBe('2 KB');
    expect(formatBytes(12)).toBe('12 bytes');
  });

  it('accepts only .pdf / .txt names, case-insensitively', () => {
    expect(hasAllowedCvExtension('cv.pdf')).toBeTrue();
    expect(hasAllowedCvExtension('My CV.PDF')).toBeTrue();
    expect(hasAllowedCvExtension('cv.txt')).toBeTrue();
    expect(hasAllowedCvExtension('cv.docx')).toBeFalse();
    expect(hasAllowedCvExtension('cv.pdf.exe')).toBeFalse();
    expect(hasAllowedCvExtension('.pdf')).toBeFalse();
    expect(hasAllowedCvExtension('cv')).toBeFalse();
  });

  it('allows exactly the limit and rejects one byte more (Spring rejects size > max-file-size)', () => {
    expect(validateCvFile(new File([new Uint8Array(CV_MAX_BYTES)], 'cv.pdf'))).toBeNull();
    expect(validateCvFile(new File([new Uint8Array(CV_MAX_BYTES + 1)], 'cv.pdf')))
      .toBe(CV_UPLOAD_COPY.tooLarge('1 MB'));
  });

  it('rejects a 0-byte file', () => {
    expect(validateCvFile(new File([], 'cv.txt'))).toBe(CV_UPLOAD_COPY.emptyFile);
  });

  it('maps HTTP errors: 0 -> network, 413 -> too large, others -> extraction failed', () => {
    expect(cvUploadErrorMessage(new HttpErrorResponse({ status: 0 }))).toBe(CV_UPLOAD_COPY.network);
    expect(cvUploadErrorMessage(new HttpErrorResponse({ status: 413 }))).toBe(CV_UPLOAD_COPY.tooLargeServer);
    expect(cvUploadErrorMessage(new HttpErrorResponse({ status: 500 }))).toBe(CV_UPLOAD_COPY.extractionFailed);
    expect(cvUploadErrorMessage(new HttpErrorResponse({ status: 400 }))).toBe(CV_UPLOAD_COPY.extractionFailed);
    expect(cvUploadErrorMessage(new Error('boom'))).toBe(CV_UPLOAD_COPY.extractionFailed);
  });
});

describe('CvUploadComponent (rendered)', () => {
  let fixture: ComponentFixture<CvUploadComponent>;
  let component: CvUploadComponent;
  let el: HTMLElement;
  let control: FormControl<string | null>;
  let upload$: Subject<CvUploadResponse>;
  let uploadCv: jasmine.Spy;
  let uploadingEvents: boolean[];

  const input = () => el.querySelector('[data-testid="cv-upload-input"]') as HTMLInputElement;
  const text = (id: string) => el.querySelector(`[data-testid="${id}"]`)?.textContent?.trim() ?? null;

  /** Simulates the user picking a file in the native input. */
  const pick = (file: File) => {
    const dt = new DataTransfer();
    dt.items.add(file);
    input().files = dt.files;
    input().dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };
  const txt = (content = 'Jane Doe\nAngular developer', name = 'cv.txt') =>
    new File([content], name, { type: 'text/plain' });

  beforeEach(() => {
    upload$ = new Subject<CvUploadResponse>();
    uploadCv = jasmine.createSpy('uploadCv').and.callFake(() => upload$.asObservable());
    TestBed.configureTestingModule({
      imports: [CvUploadComponent],
      providers: [{ provide: RiskService, useValue: { uploadCv } }]
    });
    fixture = TestBed.createComponent(CvUploadComponent);
    component = fixture.componentInstance;
    el = fixture.nativeElement;
    control = new FormControl<string | null>('');
    fixture.componentRef.setInput('target', control);
    uploadingEvents = [];
    component.uploadingChange.subscribe(v => uploadingEvents.push(v));
    fixture.detectChanges();
  });

  it('renders an accessible, labelled file input with the right accept list', () => {
    expect(input().type).toBe('file');
    expect(input().getAttribute('accept')).toBe(CV_ACCEPT);
    expect(CV_ACCEPT).toBe('.pdf,.txt,application/pdf,text/plain');
    const label = el.querySelector(`label[for="${input().id}"]`);
    expect(label?.textContent?.trim()).toBe('Upload CV (PDF or .txt)');
  });

  it('rejects a wrong file type without uploading', () => {
    pick(new File(['x'], 'cv.docx', { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' }));
    expect(uploadCv).not.toHaveBeenCalled();
    expect(text('cv-upload-error')).toBe(CV_UPLOAD_COPY.unsupportedType);
    expect(control.value).toBe('');
  });

  it('rejects an oversize file without uploading and shows the limit', () => {
    pick(new File([new Uint8Array(CV_MAX_BYTES + 512 * 1024)], 'cv.pdf', { type: 'application/pdf' }));
    expect(uploadCv).not.toHaveBeenCalled();
    expect(text('cv-upload-error')).toBe(
      'This file is too large (1.5 MB). The maximum CV file size is 1 MB.'
    );
  });

  it('shows the loading state, disables the input and ignores a second pick while uploading', () => {
    const file = txt();
    pick(file);
    expect(uploadCv).toHaveBeenCalledOnceWith(file);
    expect(component.uploading).toBeTrue();
    expect(input().disabled).toBeTrue();
    expect(text('cv-upload-loading')).toBe('Extracting text from your CV...');
    expect(uploadingEvents).toEqual([true]);

    component.onFileSelected({ target: { files: [txt('other', 'b.txt')], value: '' } } as unknown as Event);
    expect(uploadCv).toHaveBeenCalledTimes(1);

    upload$.next({ text: 'done' });
    fixture.detectChanges();
    expect(component.uploading).toBeFalse();
    expect(input().disabled).toBeFalse();
    expect(text('cv-upload-loading')).toBeNull();
    expect(uploadingEvents).toEqual([true, false]);
  });

  it('fills an empty CV field with the trimmed extracted text', () => {
    pick(txt());
    upload$.next({ text: '\n  Jane Doe\nAngular developer \n\n' });
    fixture.detectChanges();
    expect(control.value).toBe('Jane Doe\nAngular developer');
    expect(text('cv-upload-success')).toBe('CV text filled from cv.txt');
  });

  it('replaces existing CV text and says so', () => {
    control.setValue('old pasted CV');
    pick(new File(['%PDF-1.4'], 'jane-cv.pdf', { type: 'application/pdf' }));
    upload$.next({ text: 'New CV from PDF', headline: 'Jane' });
    fixture.detectChanges();
    expect(control.value).toBe('New CV from PDF');
    expect(text('cv-upload-success')).toBe('CV text replaced from jane-cv.pdf');
  });

  it('shows an extraction error on a server error and keeps the existing text', () => {
    control.setValue('keep me');
    pick(txt());
    upload$.error(new HttpErrorResponse({ status: 500, statusText: 'Internal Server Error' }));
    fixture.detectChanges();
    expect(text('cv-upload-error')).toBe(CV_UPLOAD_COPY.extractionFailed);
    expect(control.value).toBe('keep me');
    expect(component.uploading).toBeFalse();
    expect(uploadingEvents).toEqual([true, false]);
  });

  it('shows a network error when the server cannot be reached', () => {
    pick(txt());
    upload$.error(new HttpErrorResponse({ status: 0, statusText: 'Unknown Error' }));
    fixture.detectChanges();
    expect(text('cv-upload-error')).toBe(CV_UPLOAD_COPY.network);
  });

  it('shows a message when the extracted text is empty and keeps the existing text', () => {
    control.setValue('keep me');
    pick(new File(['%PDF-1.4'], 'scan.pdf', { type: 'application/pdf' }));
    upload$.next({ text: '  \n\n ' });
    fixture.detectChanges();
    expect(text('cv-upload-error')).toBe(CV_UPLOAD_COPY.emptyText);
    expect(text('cv-upload-success')).toBeNull();
    expect(control.value).toBe('keep me');
  });

  it('resets the file input so the same file can be picked again', () => {
    const file = txt();
    pick(file);
    expect(input().value).toBe('');
    upload$.next({ text: 'first' });
    fixture.detectChanges();
    pick(file);
    expect(uploadCv).toHaveBeenCalledTimes(2);
  });

  it('does nothing while externally disabled', () => {
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();
    expect(input().disabled).toBeTrue();
    component.onFileSelected({ target: { files: [txt()], value: '' } } as unknown as Event);
    expect(uploadCv).not.toHaveBeenCalled();
  });
});
