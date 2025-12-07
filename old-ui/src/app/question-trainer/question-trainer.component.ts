import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export type SkillLevel = 'Junior' | 'Mid' | 'Senior' | 'Architect';
export type RoadmapStatus = 'NEXT' | 'IN_PROGRESS' | 'COMPLETED';

export interface SoftSkillRoadmapItem {
  id: string;
  title: string;
  subtitle: string;
  level: SkillLevel;
  status: RoadmapStatus;
  tags: string[];
  questions: string[];
  currentQuestionIndex: number;
}

@Component({
  selector: 'app-question-trainer',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './question-trainer.component.html',
  styleUrls: ['./question-trainer.component.scss']
})
export class QuestionTrainerComponent {
  // --- Demo roadmap data (UI-only, no backend yet) ---

  nextItems: SoftSkillRoadmapItem[] = [
    {
      id: 'initiative',
      title: 'Tehnilise initsiatiivi vedamine',
      subtitle: 'Senior · leadership · next',
      level: 'Senior',
      status: 'NEXT',
      tags: ['leadership', 'ownership'],
      questions: [
        'Kirjelda olukorda, kus võtsid ise tehnilise initsiatiivi projekti vedamisel.',
        'Kuidas sa hindasid riske ja kaasasid teised tiimiliikmed?',
        'Mis oli tulemuseks ja mida sa sellest kogemusest õppisid?'
      ],
      currentQuestionIndex: 0
    }
  ];

  inProgressItems: SoftSkillRoadmapItem[] = [
    {
      id: 'communication',
      title: 'Selge kommunikatsioon productiga',
      subtitle: 'Mid · communication · in progress',
      level: 'Mid',
      status: 'IN_PROGRESS',
      tags: ['communication', 'product'],
      questions: [
        'Too näide, kus aitasid productil mõista tehnilist piirangut või riski.',
        'Kuidas sa kohandasid oma selgitust erinevatele osapooltele?',
        'Milline oli product-tiimi tagasiside ja mis muutus pärast seda?'
      ],
      currentQuestionIndex: 1
    }
  ];

  completedItems: SoftSkillRoadmapItem[] = [
    {
      id: 'conflict',
      title: 'Tiimikonflikti rahulik lahendamine',
      subtitle: 'Junior · conflict management · done',
      level: 'Junior',
      status: 'COMPLETED',
      tags: ['conflict', 'calm'],
      questions: [
        'Kirjelda olukorda, kus tiimis oli konflikt ja sa aitasid seda lahendada.',
        'Mida sa tegid teisiti võrreldes varasemate kordadega?',
        'Kuidas muutus tiimi dünaamika pärast juhtumit?'
      ],
      currentQuestionIndex: 3
    },
    {
      id: 'ownership',
      title: 'Oma vastutus bugfixi eest',
      subtitle: 'Junior · ownership · done',
      level: 'Junior',
      status: 'COMPLETED',
      tags: ['ownership'],
      questions: [
        'Too näide, kus võtsid täieliku vastutuse kriitilise bugi lahendamisel.'
      ],
      currentQuestionIndex: 1
    }
  ];

  // --- Aktiivne sessioon / progress ---
  activeItem: SoftSkillRoadmapItem | null = null;
  currentQuestionText: string | null = null;
  currentAnswer = '';
  progressPercent = 0;

  // Demo hindamise info
  lastEvaluationMessage: string | null = null;
  demoScore: number | null = null; // 0–1 vahemik, kuvatakse %

  // --- Getterid UI jaoks ---

  // vasaku ROADMAPS menüü allikas – kõik kaardid ühes listis
  get allRoadmapItems(): SoftSkillRoadmapItem[] {
    return [...this.nextItems, ...this.inProgressItems, ...this.completedItems];
  }

  get activeTitle(): string {
    return this.activeItem
      ? this.activeItem.title
      : 'Vali roadmap kaart ja alusta sessiooni';
  }

  get activeSubtitle(): string {
    return this.activeItem ? this.activeItem.subtitle : '';
  }

  get currentQuestionNumber(): number {
    if (!this.activeItem) {
      return 0;
    }
    return Math.min(
      this.activeItem.currentQuestionIndex + 1,
      this.activeItem.questions.length
    );
  }

  get totalQuestions(): number {
    return this.activeItem ? this.activeItem.questions.length : 0;
  }

  // --- Käivitamine kaardilt / roadmap navilt ---
  startSession(item: SoftSkillRoadmapItem): void {
    if (item.status === 'NEXT') {
      this.moveToInProgress(item);
    }

    this.activeItem = item;
    this.currentQuestionText = item.questions[item.currentQuestionIndex] ?? null;
    this.currentAnswer = '';
    this.lastEvaluationMessage = null;
    this.demoScore = null;
    this.updateProgress();
  }

  // Vastuse esitamine → järgmine küsimus / completed
  submitAnswer(): void {
    if (!this.activeItem || !this.currentQuestionText) {
      return;
    }

    this.activeItem.currentQuestionIndex++;

    if (this.activeItem.currentQuestionIndex >= this.activeItem.questions.length) {
      this.markCompleted(this.activeItem);
      this.activeItem = null;
      this.currentQuestionText = null;
      this.currentAnswer = '';
      this.progressPercent = 0;
      this.lastEvaluationMessage = null;
      this.demoScore = null;
      return;
    }

    this.currentQuestionText =
      this.activeItem.questions[this.activeItem.currentQuestionIndex];
    this.currentAnswer = '';
    this.lastEvaluationMessage = null;
    this.demoScore = null;
    this.updateProgress();
  }

  // Demo "hinda vastust" – puhas frontend, hiljem AI asemele
  evaluateAnswer(): void {
    if (!this.currentAnswer.trim()) {
      this.lastEvaluationMessage =
        'Lisa esmalt oma vastus – siis saab AI seda hiljem hinnata.';
      this.demoScore = null;
      return;
    }

    // Demo: genereerime 60–95% vahele score’i
    const min = 0.6;
    const max = 0.95;
    this.demoScore = Math.random() * (max - min) + min;

    this.lastEvaluationMessage =
      'Demo-tagasiside: hea suund! Lisa veel konkreetseid samme, mida tegid, ning lühike kokkuvõte, mida sellest olukorrast õppisid.';
  }

  // --- Abi meetodid ---
  private updateProgress(): void {
    if (!this.activeItem) {
      this.progressPercent = 0;
      return;
    }
    const answered = this.activeItem.currentQuestionIndex;
    const total = this.activeItem.questions.length || 1;
    this.progressPercent = Math.round((answered / total) * 100);
  }

  private moveToInProgress(item: SoftSkillRoadmapItem): void {
    this.nextItems = this.nextItems.filter(i => i.id !== item.id);

    item.status = 'IN_PROGRESS';
    if (!this.inProgressItems.find(i => i.id === item.id)) {
      this.inProgressItems = [...this.inProgressItems, item];
    }
  }

  private markCompleted(item: SoftSkillRoadmapItem): void {
    this.nextItems = this.nextItems.filter(i => i.id !== item.id);
    this.inProgressItems = this.inProgressItems.filter(i => i.id !== item.id);

    item.status = 'COMPLETED';
    item.currentQuestionIndex = item.questions.length;

    if (!this.completedItems.find(i => i.id === item.id)) {
      this.completedItems = [...this.completedItems, item];
    }
  }

  // Helper, et näidata level tagi kaardil
  getLevelLabel(level: SkillLevel): string {
    switch (level) {
      case 'Junior':
        return 'JUNIOR';
      case 'Mid':
        return 'MID';
      case 'Senior':
        return 'SENIOR';
      case 'Architect':
        return 'ARCHITECT';
    }
  }

  // Helper – staatuse label fookuskaardil
  getStatusLabel(status: RoadmapStatus): string {
    switch (status) {
      case 'NEXT':
        return 'NEXT';
      case 'IN_PROGRESS':
        return 'IN PROGRESS';
      case 'COMPLETED':
        return 'DONE';
    }
  }

  // Helper – “miks see skill oluline on” tekst fookuskaardil
  getImpactText(item: SoftSkillRoadmapItem | null): string {
    if (!item) {
      return '';
    }

    switch (item.id) {
      case 'communication':
        return 'Tugev kommunikatsioon product-tiimiga vähendab misalignment’i ja kiirendab otsuseid.';
      case 'initiative':
        return 'Tehnilise initsiatiivi vedamine näitab senior-level ownership’it ja strateegilist mõtlemist.';
      case 'conflict':
        return 'Konfliktijuhtimine on võtmeoskus tiimi usalduse ja koostöö säilitamiseks.';
      case 'ownership':
        return 'Bugfixi eest vastutuse võtmine tõstab usaldust sinu töö ja süsteemide stabiilsuse vastu.';
      default:
        return 'See fookus-oskuse treening aitab sul liikuda järgmisele tasemele ja tugevdada oma rolli tiimis.';
    }
  }
}
