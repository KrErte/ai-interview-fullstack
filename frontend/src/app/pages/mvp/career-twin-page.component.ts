import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  CareerTwinEntry,
  CareerTwinInsightsResponse,
  MvpApiService,
} from '../../core/services/mvp-api.service';

@Component({
  selector: 'app-career-twin-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './career-twin-page.component.html',
})
export class CareerTwinPageComponent implements OnInit {
  entryText = '';

  entries: CareerTwinEntry[] = [];
  insight?: string;

  loadingEntries = false;
  loadingInsight = false;
  saving = false;
  error = '';

  constructor(private mvpApi: MvpApiService) {}

  ngOnInit(): void {
    this.loadEntries();
    this.loadInsight();
  }

  loadEntries() {
    this.loadingEntries = true;
    this.error = '';
    this.mvpApi.getCareerTwinEntries(5).subscribe({
      next: (entries) => {
        this.entries = entries || [];
        this.loadingEntries = false;
      },
      error: (err) => {
        this.error =
          err?.error?.message ||
          'Failed to load recent entries. Please try again later.';
        this.loadingEntries = false;
      },
    });
  }

  loadInsight() {
    this.loadingInsight = true;
    this.mvpApi.getCareerTwinInsights().subscribe({
      next: (res: CareerTwinInsightsResponse) => {
        this.insight = res?.insight;
        this.loadingInsight = false;
      },
      error: () => {
        // Insights are a nice-to-have, so keep this silent in the UI
        this.loadingInsight = false;
      },
    });
  }

  saveEntry() {
    if (!this.entryText.trim()) {
      this.error = 'Write a quick note before saving.';
      return;
    }

    this.saving = true;
    this.error = '';

    this.mvpApi.appendCareerTwinEntry(this.entryText.trim()).subscribe({
      next: (res) => {
        const newEntry = res.entry;
        if (newEntry) {
          this.entries = [newEntry, ...this.entries].slice(0, 5);
        }
        this.entryText = '';
        this.saving = false;
      },
      error: (err) => {
        this.error =
          err?.error?.message ||
          'Failed to save entry. Please try again in a moment.';
        this.saving = false;
      },
    });
  }

  formatDate(value: string) {
    return new Date(value).toLocaleString();
  }
}


