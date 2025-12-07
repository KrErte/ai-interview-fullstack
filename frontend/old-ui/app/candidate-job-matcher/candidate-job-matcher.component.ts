// src/app/candidate-job-matcher/candidate-job-matcher.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

// PDF.js import – toimib pdfjs-dist@3.9.179 juures
import * as pdfjsLib from 'pdfjs-dist/build/pdf';

import { JobAnalysisService } from '../services/job-analysis.service';
import { JobAnalysisRequest } from '../models/job-analysis-request.model';
import { JobAnalysisResponse } from '../models/job-analysis-response.model';
import { generateRandomSample } from './mock-samples';

// pdf.js worker
// (vajalik, et PDF parsing töötaks ilma eraldi worker-faili bundeldamata)
(pdfjsLib as any).GlobalWorkerOptions.workerSrc =
  `https://cdnjs.cloudflare.com/ajax/libs/pdf.js/${(pdfjsLib as any).version}/pdf.worker.min.js`;

interface JobMatcherHistoryEntry {
  timestamp: string;
  email: string | null;
  scorePercent: number | null;
  summary: string | null;
  jobDescriptionSnippet: string;
}

@Component({
  selector: 'app-candidate-job-matcher',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './candidate-job-matcher.component.html',
  styleUrls: ['./candidate-job-matcher.component.css']
})
export class CandidateJobMatcherComponent implements OnInit {

  email: string | null = null;
  cvText: string = '';
  jobDescription: string = '';

  loading = false;
  error: string | null = null;
  result: JobAnalysisResponse | null = null;

  // PDF upload state
  cvFileName: string | null = null;
  pdfError: string | null = null;

  // dev/test helper info
  lastSampleName: string | null = null;
  devInfo: string | null = null;

  constructor(
    private jobAnalysisService: JobAnalysisService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.email =
      localStorage.getItem('aimentor.email') ||
      localStorage.getItem('userEmail') ||
      localStorage.getItem('email') ||
      null;
  }

  // ---- AI-like näidisandmete generaator ----
  useRandomSample(): void {
    const sample = generateRandomSample();
    this.email = sample.email;
    this.cvText = sample.cvText;
    this.jobDescription = sample.jobDescription;
    this.lastSampleName = sample.name;
    this.devInfo = `Näidisandmed: ${sample.name} · ${sample.email}`;
    this.result = null;
    this.error = null;
    this.cvFileName = null;
    this.pdfError = null;
  }

  // Lisa hunnik testajalugu profiili vaate jaoks
  seedFakeHistory(count: number = 10): void {
    const history: JobMatcherHistoryEntry[] = [];
    for (let i = 0; i < count; i++) {
      const sample = generateRandomSample();
      history.push({
        timestamp: new Date(Date.now() - i * 3600_000).toISOString(),
        email: sample.email,
        scorePercent: Math.floor(Math.random() * 101),
        summary: `Testanalüüs – ${sample.name} sobivus ${sample.email}`,
        jobDescriptionSnippet: sample.jobDescription.substring(0, 160)
      });
    }
    localStorage.setItem('candidateJobMatcher.history', JSON.stringify(history));
    this.devInfo = `Lisati ${count} testanalüüsi ajalugu (localStorage). Vaata "Minu profiil" vaadet.`;
  }

  // ---- PDF üleslaadimine ----
  async onPdfSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files[0];

    this.pdfError = null;

    if (!file) return;
    if (file.type !== 'application/pdf') {
      this.pdfError = 'Palun vali PDF fail.';
      return;
    }

    this.cvFileName = file.name;

    try {
      const arrayBuffer = await file.arrayBuffer();
      const text = await this.extractTextFromPdf(arrayBuffer);
      this.cvText = text;
    } catch (err) {
      console.error('PDF parsing error', err);
      this.pdfError = 'PDF lugemine ebaõnnestus.';
    }
  }

  private async extractTextFromPdf(arrayBuffer: ArrayBuffer): Promise<string> {
    const pdf = await (pdfjsLib as any).getDocument({ data: arrayBuffer }).promise;
    let fullText = '';

    for (let pageNum = 1; pageNum <= pdf.numPages; pageNum++) {
      const page = await pdf.getPage(pageNum);
      const content = await page.getTextContent();
      const strings = content.items.map((it: any) => it.str);
      fullText += strings.join(' ') + '\n';
    }

    return fullText;
  }

  // ---- Analüüsi käivitamine ----
  analyzeSuitability(): void {
    this.error = null;

    if (!this.cvText.trim() || !this.jobDescription.trim()) {
      this.error = 'Palun sisesta nii CV tekst kui töökuulutuse kirjeldus.';
      return;
    }

    const payload: JobAnalysisRequest = {
      email: this.email,
      cvText: this.cvText,
      jobDescription: this.jobDescription
    };

    this.loading = true;
    this.result = null;

    this.jobAnalysisService.analyze(payload).subscribe({
      next: (res) => {
        this.loading = false;

        // Backend saadab "score" (0–1 või 0–100). Konverteerime 0–100 protsendiks.
        let scorePercent: number | null = null;
        if (res.score != null) {
          scorePercent = res.score <= 1 ? Math.round(res.score * 100) : Math.round(res.score);
        }

        // laiendame tulemust scorePercent väljadega, et HTML saaks mugavalt kasutada
        const resultWithPercent: JobAnalysisResponse = {
          ...res,
          scorePercent
        };
        this.result = resultWithPercent;

        // --- salvestame treeneri payload ---
        const trainerPayload = {
          email: this.email,
          cvText: this.cvText,
          jobDescription: this.jobDescription,
          analysis: {
            score: scorePercent,
            summary: res.summary,
            missingSkills: res.missingSkills ?? [],
            roadmap: res.roadmap ?? [],
            suggestedImprovements: res.suggestedImprovements ?? []
          }
        };
        localStorage.setItem(
          'candidateJobMatcher.trainerPayload',
          JSON.stringify(trainerPayload)
        );

        // --- LISAME AJALUKKU (profiili vaate jaoks) ---
        const historyEntry: JobMatcherHistoryEntry = {
          timestamp: new Date().toISOString(),
          email: this.email,
          scorePercent,
          summary: res.summary,
          jobDescriptionSnippet: (this.jobDescription || '').substring(0, 160)
        };

        const rawHistory = localStorage.getItem('candidateJobMatcher.history');
        const history: JobMatcherHistoryEntry[] = rawHistory ? JSON.parse(rawHistory) : [];
        history.unshift(historyEntry);           // viimane ettepoole
        const trimmed = history.slice(0, 20);    // hoia max 20 kirjet
        localStorage.setItem('candidateJobMatcher.history', JSON.stringify(trimmed));
      },
      error: (err) => {
        console.error('Job analysis error', err);
        this.loading = false;
        this.error = 'Analüüs ebaõnnestus. Palun proovi hiljem uuesti.';
      }
    });
  }

  clearForm(): void {
    this.cvText = '';
    this.jobDescription = '';
    this.result = null;
    this.error = null;
    this.cvFileName = null;
    this.pdfError = null;
    this.devInfo = null;
    this.lastSampleName = null;
  }

  goToTrainer(): void {
    this.router.navigate(['/trainer']);
  }
}
