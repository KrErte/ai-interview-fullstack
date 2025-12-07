import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { JobService } from '../../services/job.service';

@Component({
  selector: 'app-upload-cv',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './upload-cv.component.html'
})
export class UploadCvComponent {
  selectedFile?: File;
  uploading = false;
  cvText = '';
  headline = '';
  skills: string[] = [];
  experienceSummary = '';
  message = '';
  error = '';

  constructor(private jobService: JobService) {
    this.cvText = this.jobService.getCachedCv();
  }

  onFileChange(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length) {
      this.selectedFile = input.files[0];
      this.message = `Ready to upload: ${this.selectedFile.name}`;
    }
  }

  upload() {
    if (!this.selectedFile || this.uploading) return;
    this.uploading = true;
    this.error = '';
    this.message = '';

    this.jobService.uploadCv(this.selectedFile).subscribe({
      next: (res) => {
        this.cvText = res.text || '';
        this.headline = res.headline || '';
        this.skills = res.skills || [];
        this.experienceSummary = res.experienceSummary || '';
        this.jobService.setCachedCv(this.cvText);
        this.uploading = false;
        this.message = 'CV uploaded and parsed successfully.';
      },
      error: (err) => {
        this.uploading = false;
        this.error = err?.error?.message || 'Failed to upload CV.';
      }
    });
  }

  get summary(): string {
    if (!this.cvText) return '';
    return this.cvText.length > 800 ? `${this.cvText.slice(0, 800)}…` : this.cvText;
  }
}

