import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-ai-feedback-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ai-feedback-panel.component.html',
  styleUrls: ['./ai-feedback-panel.component.scss'], // 👈 SCSS
})
export class AiFeedbackPanelComponent {
  /**
   * AI tagasiside tekst, mille QuestionTrainerComponent alla seob.
   */
  @Input() feedback: string | null = null;

  /**
   * Emit, kui kasutaja tahab järgmise küsimuse juurde liikuda.
   */
  @Output() nextQuestion = new EventEmitter<void>();

  /**
   * Emit, kui kasutaja tahab vastuse tühjendada.
   */
  @Output() clearAnswer = new EventEmitter<void>();

  /**
   * Emit, kui kasutaja tahab sama vastust uuesti hinnata.
   */
  @Output() reEvaluate = new EventEmitter<void>();

  onNextQuestion(): void {
    this.nextQuestion.emit();
  }

  onClearAnswer(): void {
    this.clearAnswer.emit();
  }

  onReEvaluate(): void {
    this.reEvaluate.emit();
  }
}
