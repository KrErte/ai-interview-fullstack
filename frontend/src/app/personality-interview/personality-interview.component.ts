// src/app/personality-interview/personality-interview.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  PersonalityTurn,
  PersonalityQuestionResponse,
} from '../models/personality-question.model';
import { PersonalityChatService } from '../services/personality-chat.service';
import { AuthService } from '../services/auth.service';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-personality-interview',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './personality-interview.component.html',
  styleUrls: ['./personality-interview.component.scss'],
})
export class PersonalityInterviewComponent implements OnInit {
  email: string | null = null;

  loading = false;
  error: string | null = null;

  currentQuestion: string | null = null;
  answerText = '';

  history: PersonalityTurn[] = [];
  finished = false;

  constructor(
    private chatService: PersonalityChatService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.email = this.authService.getCurrentUserEmail();
    this.startInterview();
  }

  startInterview(): void {
    this.loading = true;
    this.error = null;
    this.history = [];
    this.finished = false;
    this.currentQuestion = null;
    this.answerText = '';

    this.chatService
      .startInterview(this.email)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (res: PersonalityQuestionResponse) => {
          this.currentQuestion = res.question;
          this.finished = res.isFinal;
        },
        error: () => {
          this.error = 'Isiksuse intervjuu käivitamine ebaõnnestus. Proovi uuesti.';
        },
      });
  }

  submitAnswer(): void {
    if (
      !this.currentQuestion ||
      !this.answerText.trim() ||
      this.finished ||
      this.loading
    ) {
      return;
    }

    const answer = this.answerText.trim();
    const question = this.currentQuestion;

    this.loading = true;
    this.error = null;

    this.chatService
      .answerQuestion({
        email: this.email,
        question,
        answer,
        history: this.history,
      })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (res: PersonalityQuestionResponse) => {
          // lükka eelmine Q/A ajalukku
          this.history.push({ question, answer });
          this.answerText = '';

          this.currentQuestion = res.isFinal ? null : res.question;
          this.finished = res.isFinal;
        },
        error: () => {
          this.error =
            'GPT-l läks midagi valesti. Sinu vastust ei salvestatud – proovi uuesti.';
        },
      });
  }
}
