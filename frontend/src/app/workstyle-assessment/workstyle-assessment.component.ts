// src/app/workstyle-assessment/workstyle-assessment.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { RouterModule } from '@angular/router';

interface WorkstyleSession {
  id: string;
  email: string;
  createdAt: string;
  updatedAt: string;
  currentQuestion: string | null;
  answersJson: string | null;
  completed: boolean;
}

@Component({
  selector: 'app-workstyle-assessment',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './workstyle-assessment.component.html',
  styleUrls: ['./workstyle-assessment.component.scss'],
})
export class WorkstyleAssessmentComponent implements OnInit {
  session: WorkstyleSession | null = null;
  answer = '';
  loading = false;
  error = '';
  summaryList: { question: string; answer: string }[] = [];

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.startSession();
  }

  get email(): string {
    return localStorage.getItem('email') || '';
  }

  startSession() {
    this.loading = true;
    this.error = '';
    this.http.post<WorkstyleSession>(`/api/workstyle/start?email=${encodeURIComponent(this.email)}`, {})
      .subscribe({
        next: (session) => {
          this.session = session;
          this.syncSummary();
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Failed to start workstyle session.';
          this.loading = false;
        }
      });
  }

  submitAnswer() {
    if (!this.session || !this.answer.trim()) return;
    this.loading = true;
    this.http.post<WorkstyleSession>(`/api/workstyle/answer`, {
      sessionId: this.session.id,
      answer: this.answer
    }).subscribe({
      next: (session) => {
        this.session = session;
        this.answer = '';
        this.syncSummary();
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to submit answer.';
        this.loading = false;
      }
    });
  }

  syncSummary() {
    if (this.session?.answersJson) {
      try {
        this.summaryList = JSON.parse(this.session.answersJson);
      } catch {
        this.summaryList = [];
      }
    } else {
      this.summaryList = [];
    }
  }
}
