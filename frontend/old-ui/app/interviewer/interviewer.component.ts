import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { QuestionService } from '../services/question.service';
import { Question } from '../models/question.model';
import {
  CvQuestionService,
  GenerateQuestionsRequest,
} from '../services/cv-question.service';

@Component({
  selector: 'app-interviewer',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './interviewer.component.html',
  styleUrls: ['./interviewer.component.css'],
})
export class InterviewerComponent implements OnDestroy {
  // PDF + CV tekst
  email: string = '';
  cvText: string = '';
  cvFileName: string | null = null;
  cvUploadError: string | null = null;

  // AI genereeritud küsimused
  questions: Question[] = [];
  loadingQuestions = false;
  loadError: string | null = null;

  // Valitud küsimus + vastus
  question: string = 'Tell me about yourself.';
  answer: string = '';

  // AI “thinking”
  isThinking: boolean = false;
  private typingTimer: any;

  // Mock evaluation (hiljem seome backendiga)
  evaluation: {
    score: number;
    tone: string;
    missingPoints: string[];
    tips: string;
  } | null = null;

  constructor(
    private questionService: QuestionService, // hetkel ainult olemas, kui hiljem tahame hinnangut siit ka kutsuda
    private cvService: CvQuestionService
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
      this.cvUploadError = 'Palun vali PDF fail.';
      return;
    }

    this.cvFileName = file.name;
    this.isThinking = true;

    this.cvService.uploadCvPdf(file).subscribe({
      next: (res) => {
        // 🔥 SIIN TÄIDAME CV TEKSTI
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
      },
    });
  }

  /* ===== CV → AI küsimused ===== */

  loadQuestionsFromCv(): void {
    if (!this.cvText || this.cvText.trim().length < 30) {
      this.loadError = 'Lisa vähemalt lühike CV tekst (min ~30 märki).';
      return;
    }

    this.loadError = null;
    this.loadingQuestions = true;
    this.isThinking = true;

    const req: GenerateQuestionsRequest = {
      cvText: this.cvText,
      technicalCount: 6,
      softCount: 4,
    };

    // ⬇️ SIIN OLI VIGA: enne kutsus QuestionService.generateCvQuestions(...)
    // Kasutame nüüd õigesti CvQuestionService.generateQuestions(...)
    this.cvService.generateQuestions(req).subscribe({
      next: (qs) => {
        this.questions = qs || [];
        this.loadingQuestions = false;
        this.isThinking = false;

        if (this.questions.length > 0) {
          this.useQuestion(this.questions[0]);
        }
      },
      error: (err) => {
        console.error(err);
        this.loadingQuestions = false;
        this.isThinking = false;
        this.loadError = 'AI küsimuste genereerimine ebaõnnestus.';
      },
    });
  }

  useQuestion(q: Question): void {
    this.question = q.text;
    this.answer = '';
    this.evaluation = null;
  }

  /* ===== Vastuse sisestamine + mock hindamine (kohalik) ===== */

  onAnswerChange(): void {
    this.evaluation = null;

    if (this.typingTimer) {
      clearTimeout(this.typingTimer);
    }

    if (!this.answer || this.answer.trim().length === 0) {
      return;
    }

    this.isThinking = true;

    this.typingTimer = setTimeout(() => {
      this.isThinking = false;
      this.mockEvaluate();
    }, 1200);
  }

  private mockEvaluate(): void {
    if (!this.answer || this.answer.trim().length < 10) {
      this.evaluation = {
        score: 42,
        tone: 'ebaselge, lühike',
        missingPoints: [
          'Vastus on liiga lühike.',
          'Lisa 1–2 konkreetset näidet oma töökogemusest.',
        ],
        tips: 'Kasuta STAR struktuuri (Situation, Task, Action, Result).',
      };
      return;
    }

    this.evaluation = {
      score: 78,
      tone: 'enesekindel, sõbralik',
      missingPoints: [
        'Puuduvad konkreetsed numbrid ja tulemused.',
        'Rõhuta rohkem oma rolli meeskonnas.',
      ],
      tips:
        'Lisa 1–2 mõõdetavat saavutust ja lõpeta vastus tugeva kokkuvõttega.',
    };
  }

  ngOnDestroy(): void {
    if (this.typingTimer) {
      clearTimeout(this.typingTimer);
    }
  }
}
