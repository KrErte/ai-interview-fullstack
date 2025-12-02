import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Question } from '../models/question.model';
import { CvQuestionService } from '../services/cv-question.service';
import {
  CandidatePlanService,
  PracticeBlock,
  RoadmapStep,
  CandidatePlanResponse
} from '../services/candidate-plan.service';

@Component({
  selector: 'app-candidate',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './candidate.component.html',
  styleUrls: ['./candidate.component.css']
})
export class CandidateComponent {

  cvText: string = '';
  cvFileName: string | null = null;
  cvUploadError: string | null = null;

  questions: Question[] = [];
  practiceBlocks: PracticeBlock[] = [];
  roadmap: RoadmapStep[] = [];

  loadError: string | null = null;
  isThinking: boolean = false;

  constructor(
    private cvService: CvQuestionService,
    private candidatePlanService: CandidatePlanService
  ) {}

  /* ===== PDF UPLOAD ===== */

  onCvFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    this.cvUploadError = null;

    if (!file) {
      return;
    }

    if (file.type !== 'application/pdf') {
      this.cvUploadError = 'Palun vali PDF fail (PDF).';
      return;
    }

    this.cvFileName = file.name;
    this.isThinking = true;

    this.cvService.uploadCvPdf(file).subscribe({
      next: (res) => {
        this.cvText = res.text ?? '';
        this.isThinking = false;

        if (!this.cvText || this.cvText.trim().length === 0) {
          this.cvUploadError = 'PDF-ist ei õnnestunud teksti lugeda.';
        }
      },
      error: (err) => {
        console.error(err);
        this.isThinking = false;
        this.cvUploadError = 'CV üleslaadimine ebaõnnestus.';
      }
    });
  }

  /* ===== AI PLAN ===== */

  generateCandidatePlan(): void {
    if (!this.cvText || this.cvText.trim().length < 30) {
      this.loadError = 'Lisa vähemalt lühike CV tekst (min ~30 märki).';
      return;
    }

    this.loadError = null;
    this.isThinking = true;

    this.candidatePlanService.generatePlan(this.cvText).subscribe({
      next: (resp: CandidatePlanResponse) => {
        this.questions = resp.questions || [];
        this.practiceBlocks = resp.practiceBlocks || [];
        this.roadmap = resp.roadmap || [];
        this.isThinking = false;
      },
      error: (err) => {
        console.error(err);
        this.isThinking = false;
        this.loadError = 'Treeningplaani genereerimine ebaõnnestus.';
      }
    });
  }
}
