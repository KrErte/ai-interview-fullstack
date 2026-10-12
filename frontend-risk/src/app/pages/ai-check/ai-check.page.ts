import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { AiReadinessService } from '../../services/ai-readiness.service';
import {
  AssessmentResult,
  ProfessionSummary,
  Question,
  RiskBand
} from '../../models/ai-readiness.model';

type Step = 'profession' | 'questions' | 'loading' | 'result';

const BAND_STYLE: Record<RiskBand, { label: string; stroke: string; text: string; chip: string }> = {
  LOW: { label: 'Madal', stroke: '#10b981', text: 'text-emerald-400', chip: 'bg-emerald-500/10 text-emerald-300 border-emerald-500/30' },
  MODERATE: { label: 'Mõõdukas', stroke: '#f59e0b', text: 'text-amber-400', chip: 'bg-amber-500/10 text-amber-300 border-amber-500/30' },
  HIGH: { label: 'Kõrge', stroke: '#f97316', text: 'text-orange-400', chip: 'bg-orange-500/10 text-orange-300 border-orange-500/30' },
  VERY_HIGH: { label: 'Väga kõrge', stroke: '#ef4444', text: 'text-rose-400', chip: 'bg-rose-500/10 text-rose-300 border-rose-500/30' }
};

@Component({
  selector: 'app-ai-check-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="min-h-screen">
      <!-- Header -->
      <div class="text-center mb-10" *ngIf="step !== 'result'">
        <p class="text-sm font-medium uppercase tracking-wider text-emerald-400 mb-3">AI valmisoleku test</p>
        <h1 class="text-4xl font-bold text-slate-100 mb-4">
          Kui asendatav oled sa <span class="gradient-text">AI poolt</span>?
        </h1>
        <p class="text-lg text-slate-400 max-w-2xl mx-auto">
          Vasta {{ questions.length || 11 }} küsimusele ja saad hinnangu,
          isikliku AI õppimise roadmapi ning oma eriala plaani. Sisselogimist pole vaja.
        </p>
      </div>

      <div *ngIf="error" class="max-w-2xl mx-auto mb-6 rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-rose-300 text-sm">
        {{ error }}
      </div>

      <!-- STEP 1: profession -->
      <section *ngIf="step === 'profession'" class="max-w-3xl mx-auto">
        <div *ngIf="lastResult" class="mb-6 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 rounded-2xl border border-slate-800 bg-slate-900/50 px-5 py-4">
          <p class="text-sm text-slate-300">
            Sul on salvestatud tulemus: <strong>{{ lastResult.professionName }}</strong>,
            asendatavus {{ lastResult.replaceabilityScore }}/100.
          </p>
          <button (click)="showLastResult()" class="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-200 hover:bg-slate-800">
            Vaata tulemust
          </button>
        </div>

        <h2 class="text-xl font-semibold text-slate-100 mb-4">1. Vali oma amet</h2>
        <input
          type="search"
          [(ngModel)]="search"
          placeholder="Otsi ametit..."
          class="w-full mb-5 rounded-xl border border-slate-700 bg-slate-800/50 px-4 py-3 text-slate-200 placeholder-slate-500 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none"
        />
        <div class="grid sm:grid-cols-2 gap-3">
          <button
            *ngFor="let p of filteredProfessions()"
            (click)="selectProfession(p)"
            class="text-left rounded-2xl border px-5 py-4 transition-all card-hover"
            [ngClass]="p.id === professionId
              ? 'border-emerald-500/60 bg-emerald-500/10'
              : 'border-slate-800 bg-slate-900/40 hover:border-slate-700'"
          >
            <p class="font-medium text-slate-100">{{ p.name }}</p>
            <p class="text-xs text-slate-500 mt-1">{{ p.category }}</p>
          </button>
        </div>
        <p *ngIf="!loadingCatalog && filteredProfessions().length === 0" class="text-slate-500 text-sm mt-4">
          Ei leidnud. Vali "Muu / ei leia oma ametit".
        </p>
        <p *ngIf="loadingCatalog" class="text-slate-500 text-sm">Laen...</p>
      </section>

      <!-- STEP 2: questions -->
      <section *ngIf="step === 'questions' && currentQuestion as q" class="max-w-2xl mx-auto">
        <div class="mb-6">
          <div class="flex justify-between text-sm text-slate-400 mb-2">
            <span>{{ selectedProfessionName() }}</span>
            <span>{{ questionIndex + 1 }} / {{ questions.length }}</span>
          </div>
          <div class="h-2 rounded-full bg-slate-800 overflow-hidden">
            <div class="h-full bg-gradient-to-r from-emerald-500 to-teal-400 transition-all duration-300"
                 [style.width.%]="((questionIndex + 1) / questions.length) * 100"></div>
          </div>
        </div>

        <div class="rounded-3xl border border-slate-800/50 bg-gradient-to-br from-slate-900/80 to-slate-900/40 p-6 sm:p-8">
          <h2 class="text-xl sm:text-2xl font-semibold text-slate-100 mb-2">{{ q.text }}</h2>
          <p class="text-sm text-slate-500 mb-6">{{ q.help }}</p>
          <div class="space-y-3">
            <button
              *ngFor="let o of q.options"
              (click)="answer(q.id, o.id)"
              class="w-full text-left rounded-xl border px-5 py-4 transition-all"
              [ngClass]="answers[q.id] === o.id
                ? 'border-emerald-500/60 bg-emerald-500/10 text-slate-100'
                : 'border-slate-700 bg-slate-800/40 text-slate-300 hover:border-slate-600 hover:bg-slate-800'"
            >
              {{ o.label }}
            </button>
          </div>

          <label *ngIf="isLastQuestion()" class="mt-6 flex items-start gap-3 text-sm text-slate-400 cursor-pointer">
            <input type="checkbox" [(ngModel)]="personalize" class="mt-1 accent-emerald-500" />
            <span>Lisa AI kirjutatud isiklik kommentaar (vastused saadetakse keelemudelile, ilma nime ja e-postita)</span>
          </label>

          <div class="mt-8 flex justify-between">
            <button (click)="back()" class="rounded-lg px-4 py-2 text-sm text-slate-400 hover:text-slate-100">
              ← Tagasi
            </button>
            <button
              *ngIf="isLastQuestion()"
              (click)="submit()"
              [disabled]="!answers[q.id]"
              class="rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 px-6 py-3 font-semibold text-white shadow-lg shadow-emerald-500/25 disabled:opacity-50"
            >
              Näita tulemust
            </button>
          </div>
        </div>
      </section>

      <!-- STEP 3: loading -->
      <section *ngIf="step === 'loading'" class="flex flex-col items-center py-20 text-slate-400">
        <div class="h-12 w-12 rounded-full border-4 border-slate-700 border-t-emerald-500 animate-spin mb-4"></div>
        Arvutan tulemust ja koostan roadmapi...
      </section>

      <!-- STEP 4: result -->
      <section *ngIf="step === 'result' && result as r" class="space-y-8">
        <div class="text-center">
          <p class="text-sm font-medium uppercase tracking-wider text-slate-500 mb-2">{{ r.professionName }}</p>
          <h1 class="text-3xl sm:text-4xl font-bold text-slate-100">Sinu AI tulemus</h1>
        </div>

        <!-- Scores -->
        <div class="grid md:grid-cols-2 gap-6">
          <div class="rounded-3xl border border-slate-800/50 bg-slate-900/50 p-6 flex flex-col items-center text-center">
            <p class="text-sm text-slate-400 mb-4">Asendatavus AI poolt</p>
            <svg viewBox="0 0 120 120" class="w-40 h-40">
              <circle cx="60" cy="60" r="50" fill="none" stroke="#1e293b" stroke-width="12" />
              <circle cx="60" cy="60" r="50" fill="none" stroke-width="12" stroke-linecap="round"
                      [attr.stroke]="band(r).stroke"
                      [attr.stroke-dasharray]="circumference"
                      [attr.stroke-dashoffset]="circumference * (1 - r.replaceabilityScore / 100)"
                      transform="rotate(-90 60 60)" />
              <text x="60" y="66" text-anchor="middle" class="fill-slate-100" font-size="28" font-weight="700">
                {{ r.replaceabilityScore }}
              </text>
            </svg>
            <span class="mt-3 inline-block rounded-full border px-3 py-1 text-xs font-medium" [ngClass]="band(r).chip">
              {{ band(r).label }}
            </span>
            <p class="mt-4 text-slate-300">{{ r.headline }}</p>
            <p class="mt-3 text-xs text-slate-500">
              Ameti ülesannete AI-eksponeeritus {{ r.taskExposureScore }}
              {{ r.personalAdjustment >= 0 ? '+' : '−' }} {{ abs(r.personalAdjustment) }} sinu vastuste põhjal
            </p>
          </div>

          <div class="rounded-3xl border border-slate-800/50 bg-slate-900/50 p-6 flex flex-col">
            <p class="text-sm text-slate-400 mb-4 text-center">AI valmisolek</p>
            <p class="text-5xl font-bold text-center text-slate-100">{{ r.readinessScore }}<span class="text-xl text-slate-500">/100</span></p>
            <p class="text-center mt-2 text-emerald-400 font-medium">{{ r.readinessLevel }}</p>
            <div class="mt-6 h-3 rounded-full bg-slate-800 overflow-hidden">
              <div class="h-full bg-gradient-to-r from-sky-500 to-emerald-400 animate-progress" [style.width.%]="r.readinessScore"></div>
            </div>
            <p class="mt-6 text-sm text-slate-400">
              Üldise roadmapi läbimiseks kulub sinu tempoga umbes
              <strong class="text-slate-200">{{ r.totalWeeks }} nädalat</strong>.
            </p>
          </div>
        </div>

        <div *ngIf="r.personalNote" class="rounded-3xl border border-emerald-500/30 bg-emerald-500/5 p-6">
          <p class="text-sm font-medium text-emerald-400 mb-2">Isiklik kommentaar</p>
          <p class="text-slate-200 leading-relaxed whitespace-pre-line">{{ r.personalNote }}</p>
        </div>
        <p *ngIf="!r.personalNote && personalNoteRequested" class="text-center text-sm text-slate-500">
          AI isiklik kommentaar pole praegu saadaval. Ülejäänud tulemus on arvutatud tavapäraselt.
        </p>

        <!-- Tasks -->
        <div class="rounded-3xl border border-slate-800/50 bg-slate-900/50 p-6">
          <h2 class="text-xl font-semibold text-slate-100 mb-1">Mis sinu tööst läheb AI-le</h2>
          <p class="text-sm text-slate-500 mb-5">Ülesanded, nende osakaal tööajast ja hinnanguline automatiseeritavus.</p>
          <div class="space-y-4">
            <div *ngFor="let t of r.tasks">
              <div class="flex justify-between gap-4 text-sm mb-1">
                <span class="text-slate-300">{{ t.name }} <span class="text-slate-500">· {{ t.share }}% ajast</span></span>
                <span class="text-slate-400 shrink-0">{{ t.automationPercent }}%</span>
              </div>
              <div class="h-2 rounded-full bg-slate-800 overflow-hidden">
                <div class="h-full rounded-full"
                     [ngClass]="t.automationPercent >= 70 ? 'bg-rose-500/80' : t.automationPercent >= 40 ? 'bg-amber-500/80' : 'bg-emerald-500/80'"
                     [style.width.%]="t.automationPercent"></div>
              </div>
            </div>
          </div>
        </div>

        <div class="grid md:grid-cols-3 gap-6">
          <div class="rounded-3xl border border-slate-800/50 bg-slate-900/50 p-6">
            <h3 class="font-semibold text-slate-100 mb-3">Sinu inimlik eelis</h3>
            <ul class="space-y-2 text-sm text-slate-300 list-disc list-inside">
              <li *ngFor="let h of r.humanEdge">{{ h }}</li>
            </ul>
          </div>
          <div class="rounded-3xl border border-slate-800/50 bg-slate-900/50 p-6">
            <h3 class="font-semibold text-slate-100 mb-3">Tööriistad, mida proovida</h3>
            <div class="flex flex-wrap gap-2">
              <span *ngFor="let tool of r.aiTools" class="rounded-full border border-slate-700 bg-slate-800/60 px-3 py-1 text-xs text-slate-300">{{ tool }}</span>
            </div>
          </div>
          <div class="rounded-3xl border border-slate-800/50 bg-slate-900/50 p-6">
            <h3 class="font-semibold text-slate-100 mb-3">Lähedased rollid tulevikus</h3>
            <ul class="space-y-2 text-sm text-slate-300 list-disc list-inside">
              <li *ngFor="let p of r.pivotRoles">{{ p }}</li>
            </ul>
          </div>
        </div>

        <!-- General roadmap -->
        <div class="rounded-3xl border border-slate-800/50 bg-slate-900/50 p-6">
          <h2 class="text-xl font-semibold text-slate-100 mb-1">Üldine AI õppimise roadmap</h2>
          <p class="text-sm text-slate-500 mb-6">Sobib igale erialale. Etapid, mille oled juba läbinud, on märgitud.</p>
          <ol class="relative border-l border-slate-800 ml-3 space-y-8">
            <li *ngFor="let s of r.generalRoadmap" class="ml-6">
              <span class="absolute -left-3 flex h-6 w-6 items-center justify-center rounded-full text-xs font-bold"
                    [ngClass]="s.status === 'DONE' ? 'bg-emerald-500 text-white' : s.status === 'CURRENT' ? 'bg-sky-500 text-white ring-4 ring-sky-500/20' : 'bg-slate-800 text-slate-400'">
                {{ s.status === 'DONE' ? '✓' : '' }}
              </span>
              <div class="flex flex-wrap items-center gap-2 mb-1">
                <h3 class="font-semibold" [ngClass]="s.status === 'DONE' ? 'text-slate-500' : 'text-slate-100'">{{ s.title }}</h3>
                <span *ngIf="s.status === 'CURRENT'" class="rounded-full bg-sky-500/10 border border-sky-500/30 px-2 py-0.5 text-xs text-sky-300">Alusta siit</span>
                <span *ngIf="s.status !== 'DONE'" class="text-xs text-slate-500">~{{ s.weeks }} näd</span>
              </div>
              <p class="text-sm text-slate-400 mb-3">{{ s.goal }}</p>
              <ng-container *ngIf="s.status !== 'DONE' || expanded[s.id]">
                <ul class="space-y-1.5 text-sm text-slate-300 list-disc list-inside mb-3">
                  <li *ngFor="let i of s.items">{{ i }}</li>
                </ul>
                <div class="flex flex-wrap gap-2">
                  <a *ngFor="let res of s.resources" [href]="res.url" target="_blank" rel="noopener"
                     class="rounded-lg border border-slate-700 px-3 py-1.5 text-xs text-emerald-300 hover:bg-slate-800">
                    {{ res.title }} ↗
                  </a>
                </div>
              </ng-container>
              <button *ngIf="s.status === 'DONE' && !expanded[s.id]" (click)="expanded[s.id] = true" class="text-xs text-slate-500 hover:text-slate-300">
                Näita sisu
              </button>
            </li>
          </ol>
        </div>

        <!-- Profession roadmap -->
        <div class="rounded-3xl border border-slate-800/50 bg-slate-900/50 p-6">
          <h2 class="text-xl font-semibold text-slate-100 mb-1">Roadmap: {{ r.professionName }}</h2>
          <p class="text-sm text-slate-500 mb-6">Kuidas AI-d just sinu erialal kasutama hakata. Iga etapi lõpus on praktiline projekt.</p>
          <div class="grid md:grid-cols-3 gap-4">
            <div *ngFor="let ph of r.professionRoadmap; let i = index" class="rounded-2xl border border-slate-800 bg-slate-950/40 p-5 flex flex-col">
              <p class="text-xs text-slate-500 mb-1">Etapp {{ i + 1 }}</p>
              <h3 class="font-semibold text-slate-100 mb-3">{{ ph.title }}</h3>
              <ul class="space-y-1.5 text-sm text-slate-300 list-disc list-inside mb-4 flex-1">
                <li *ngFor="let it of ph.items">{{ it }}</li>
              </ul>
              <div class="rounded-xl bg-emerald-500/5 border border-emerald-500/20 p-3 text-sm text-slate-300">
                <span class="text-emerald-400 font-medium">Projekt:</span> {{ ph.project }}
              </div>
            </div>
          </div>
        </div>

        <p class="text-xs text-slate-500 leading-relaxed">
          Metoodika: skoor põhineb ameti tüüpülesannete osakaalul ja nende hinnangulisel automatiseeritavusel
          tänaste AI tööriistadega (sarnaselt OpenAI/UPenn "GPTs are GPTs" ja ILO AI-eksponeerituse uuringutele),
          mida korrigeeritakse sinu vastuste põhjal kuni ±30 punkti. See on suunav hinnang, mitte ennustus. Kõrge skoor
          tähendab, et sinu ülesanded muutuvad, mitte et sinu töökoht kaob.
        </p>

        <div class="flex flex-col sm:flex-row gap-3 justify-center print:hidden">
          <button (click)="restart()" class="rounded-xl border border-slate-700 bg-slate-800/50 px-6 py-3 text-slate-200 hover:bg-slate-800">
            Tee test uuesti
          </button>
          <button (click)="print()" class="rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 px-6 py-3 font-semibold text-white">
            Prindi / salvesta PDF
          </button>
        </div>
      </section>
    </div>
  `
})
export class AiCheckPageComponent implements OnInit {
  readonly circumference = 2 * Math.PI * 50;

  step: Step = 'profession';
  professions: ProfessionSummary[] = [];
  questions: Question[] = [];
  loadingCatalog = true;
  error: string | null = null;

  search = '';
  professionId: string | null = null;
  questionIndex = 0;
  answers: Record<string, string> = {};
  personalize = false;
  personalNoteRequested = false;

  result: AssessmentResult | null = null;
  lastResult: AssessmentResult | null = null;
  expanded: Record<string, boolean> = {};

  constructor(private api: AiReadinessService) {}

  ngOnInit(): void {
    this.lastResult = this.api.loadLastResult();
    forkJoin({ professions: this.api.professions(), questions: this.api.questions() }).subscribe({
      next: ({ professions, questions }) => {
        this.professions = professions;
        this.questions = questions;
        this.loadingCatalog = false;
      },
      error: () => {
        this.loadingCatalog = false;
        this.error = 'Testi laadimine ebaõnnestus. Kontrolli, kas backend töötab.';
      }
    });
  }

  get currentQuestion(): Question | null {
    return this.questions[this.questionIndex] ?? null;
  }

  filteredProfessions(): ProfessionSummary[] {
    const term = this.search.trim().toLowerCase();
    if (!term) {
      return this.professions;
    }
    return this.professions.filter(
      p => p.name.toLowerCase().includes(term) || p.category.toLowerCase().includes(term) || p.id === 'other'
    );
  }

  selectedProfessionName(): string {
    return this.professions.find(p => p.id === this.professionId)?.name ?? '';
  }

  selectProfession(p: ProfessionSummary): void {
    this.professionId = p.id;
    this.questionIndex = 0;
    this.step = 'questions';
  }

  isLastQuestion(): boolean {
    return this.questionIndex === this.questions.length - 1;
  }

  answer(questionId: string, optionId: string): void {
    this.answers = { ...this.answers, [questionId]: optionId };
    if (!this.isLastQuestion()) {
      // A double click schedules two advances; only the first may move on, or a question gets skipped.
      const index = this.questionIndex;
      setTimeout(() => {
        if (this.questionIndex === index) {
          this.questionIndex = index + 1;
        }
      }, 150);
    }
  }

  allAnswered(): boolean {
    return this.questions.every(q => !!this.answers[q.id]);
  }

  back(): void {
    if (this.questionIndex === 0) {
      this.step = 'profession';
    } else {
      this.questionIndex--;
    }
  }

  submit(): void {
    if (!this.professionId) {
      return;
    }
    if (!this.allAnswered()) {
      this.questionIndex = this.questions.findIndex(q => !this.answers[q.id]);
      return;
    }
    this.error = null;
    this.step = 'loading';
    this.api.assess({ professionId: this.professionId, answers: this.answers, personalize: this.personalize }).subscribe({
      next: result => {
        this.result = result;
        this.lastResult = result;
        this.personalNoteRequested = this.personalize;
        this.api.saveLastResult(result);
        this.step = 'result';
        window.scrollTo({ top: 0 });
      },
      error: () => {
        this.error = 'Tulemuse arvutamine ebaõnnestus. Proovi uuesti.';
        this.step = 'questions';
      }
    });
  }

  showLastResult(): void {
    this.result = this.lastResult;
    this.personalNoteRequested = false;
    this.step = 'result';
  }

  restart(): void {
    this.answers = {};
    this.professionId = null;
    this.questionIndex = 0;
    this.result = null;
    this.expanded = {};
    this.step = 'profession';
    window.scrollTo({ top: 0 });
  }

  band(r: AssessmentResult) {
    return BAND_STYLE[r.band];
  }

  abs(n: number): number {
    return Math.abs(n);
  }

  print(): void {
    window.print();
  }
}
