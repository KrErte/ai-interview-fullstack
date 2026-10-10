import { Component, EventEmitter, Input, OnDestroy, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';

import { RiskService } from '../../services/risk.service';

/**
 * Max CV file size, mirroring the backend.
 *
 * The backend (POST /api/cv/extract-text) sets no spring.servlet.multipart.*
 * limits, so Spring Boot's defaults apply: max-file-size 1MB (1024 * 1024
 * bytes, files strictly larger are rejected with 413) and max-request-size 10MB.
 * nginx allows 20m, so Spring's 1MB is the effective limit. Update this if the
 * backend ever configures spring.servlet.multipart.max-file-size.
 */
export const CV_MAX_BYTES = 1024 * 1024;

/**
 * Allowed extensions. The backend decides PDF vs text by the file NAME only
 * (originalFilename ends with ".pdf" -> PDFBox, anything else is read as UTF-8
 * text), so the extension is what must be right; MIME types are unreliable
 * across browsers/OSes (e.g. an empty type for .txt on some systems).
 */
export const CV_ALLOWED_EXTENSIONS = ['.pdf', '.txt'] as const;
export const CV_ACCEPT = '.pdf,.txt,application/pdf,text/plain';

export const CV_UPLOAD_COPY = {
  button: 'Upload CV (PDF or .txt)',
  hint: 'PDF or .txt, up to 1 MB. The extracted text replaces what is in the field.',
  loading: 'Extracting text from your CV...',
  unsupportedType: 'Unsupported file type. Please upload your CV as a PDF or .txt file.',
  tooLarge: (sizeLabel: string) =>
    `This file is too large (${sizeLabel}). The maximum CV file size is ${formatBytes(CV_MAX_BYTES)}.`,
  tooLargeServer: `This file is too large. The maximum CV file size is ${formatBytes(CV_MAX_BYTES)}.`,
  emptyFile: 'This file is empty. Please choose a CV file that contains text.',
  network: "We couldn't reach the server to read your CV. Check your connection and try again.",
  extractionFailed: "We couldn't extract text from your CV. Try another file, or paste your CV text instead.",
  emptyText:
    "We couldn't find any text in this file. If it's a scanned PDF, please paste your CV text instead.",
  filled: (fileName: string) => `CV text filled from ${fileName}`,
  replaced: (fileName: string) => `CV text replaced from ${fileName}`
};

/** "1 MB", "1.5 MB", "820 KB", "12 bytes" (1 KB = 1024 bytes, like Spring's DataSize). */
export function formatBytes(bytes: number): string {
  if (bytes >= 1024 * 1024) {
    return `${Number((bytes / (1024 * 1024)).toFixed(1))} MB`;
  }
  if (bytes >= 1024) {
    return `${Math.round(bytes / 1024)} KB`;
  }
  return `${bytes} bytes`;
}

export function hasAllowedCvExtension(fileName: string): boolean {
  const lower = (fileName || '').toLowerCase();
  return CV_ALLOWED_EXTENSIONS.some(ext => lower.endsWith(ext) && lower.length > ext.length);
}

/** Returns an error message for a file that must not be uploaded, or null if it is OK. */
export function validateCvFile(file: File): string | null {
  if (!hasAllowedCvExtension(file.name)) return CV_UPLOAD_COPY.unsupportedType;
  if (file.size > CV_MAX_BYTES) return CV_UPLOAD_COPY.tooLarge(formatBytes(file.size));
  if (file.size === 0) return CV_UPLOAD_COPY.emptyFile;
  return null;
}

/** Maps an upload error to user-facing copy. */
export function cvUploadErrorMessage(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) return CV_UPLOAD_COPY.network;
    if (err.status === 413) return CV_UPLOAD_COPY.tooLargeServer;
  }
  return CV_UPLOAD_COPY.extractionFailed;
}

/**
 * File input that uploads a CV to RiskService.uploadCv and writes the
 * extracted text into the given form control (the risk page's cvText).
 */
@Component({
  selector: 'app-cv-upload',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="mt-3">
      <div class="flex flex-wrap items-center gap-3">
        <input
          #fileInput
          id="cv-upload-input"
          type="file"
          class="peer sr-only"
          [attr.accept]="accept"
          [disabled]="uploading || disabled"
          aria-describedby="cv-upload-hint cv-upload-status"
          (change)="onFileSelected($event)"
          data-testid="cv-upload-input"
        />
        <label
          for="cv-upload-input"
          class="inline-flex items-center gap-2 rounded-xl border border-slate-700 bg-slate-800/50 px-4 py-2 text-sm font-medium text-slate-200 transition-all hover:border-emerald-500 peer-focus-visible:ring-2 peer-focus-visible:ring-emerald-500/40"
          [class.cursor-pointer]="!(uploading || disabled)"
          [class.opacity-50]="uploading || disabled"
          [class.cursor-not-allowed]="uploading || disabled"
          data-testid="cv-upload-label"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
          </svg>
          {{ copy.button }}
        </label>
        <span id="cv-upload-hint" class="text-xs text-slate-500">{{ copy.hint }}</span>
      </div>

      <div id="cv-upload-status" aria-live="polite" class="mt-2 text-sm">
        <p *ngIf="uploading" class="flex items-center gap-2 text-slate-300" data-testid="cv-upload-loading">
          <span class="inline-block h-4 w-4 rounded-full border-2 border-emerald-500 border-t-transparent animate-spin" aria-hidden="true"></span>
          {{ copy.loading }}
        </p>
        <p *ngIf="!uploading && errorMessage" class="text-rose-400" role="alert" data-testid="cv-upload-error">
          {{ errorMessage }}
        </p>
        <p *ngIf="!uploading && !errorMessage && successMessage" class="text-emerald-400" data-testid="cv-upload-success">
          {{ successMessage }}
        </p>
      </div>
    </div>
  `
})
export class CvUploadComponent implements OnDestroy {
  /** The control that receives the extracted CV text (risk page: form.controls.cvText). */
  @Input({ required: true }) target!: FormControl<string | null>;
  /** Extra external disable (e.g. while a job match analysis runs). */
  @Input() disabled = false;
  /** Emits true when an upload starts and false when it ends (success, error or destroy). */
  @Output() uploadingChange = new EventEmitter<boolean>();

  readonly accept = CV_ACCEPT;
  readonly copy = CV_UPLOAD_COPY;

  uploading = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  private sub: Subscription | null = null;

  constructor(private riskService: RiskService) {}

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.[0] ?? null;
    // Reset right away so selecting the same file again fires (change) again.
    if (input) input.value = '';

    if (!file || this.uploading || this.disabled) return;

    this.errorMessage = null;
    this.successMessage = null;

    const invalid = validateCvFile(file);
    if (invalid) {
      this.errorMessage = invalid;
      return;
    }

    this.setUploading(true);
    this.sub = this.riskService.uploadCv(file).subscribe({
      next: res => {
        const text = typeof res?.text === 'string' ? res.text.trim() : '';
        if (!text) {
          this.errorMessage = CV_UPLOAD_COPY.emptyText;
        } else {
          const hadText = !!(this.target.value && this.target.value.trim());
          this.target.setValue(text);
          this.target.markAsDirty();
          this.successMessage = hadText ? CV_UPLOAD_COPY.replaced(file.name) : CV_UPLOAD_COPY.filled(file.name);
        }
        this.setUploading(false);
      },
      error: err => {
        this.errorMessage = cvUploadErrorMessage(err);
        this.setUploading(false);
      }
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    if (this.uploading) this.setUploading(false);
  }

  private setUploading(value: boolean): void {
    this.uploading = value;
    if (!value) this.sub = null;
    this.uploadingChange.emit(value);
  }
}
