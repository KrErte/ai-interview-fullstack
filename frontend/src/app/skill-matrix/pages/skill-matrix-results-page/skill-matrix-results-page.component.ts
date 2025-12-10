import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { SkillMatrixApiService } from '../../services/skill-matrix-api.service';
import {
  AggregatedResult,
  SkillScore,
} from '../../models/evaluation.model';

@Component({
  selector: 'app-skill-matrix-results-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './skill-matrix-results-page.component.html',
  styleUrls: ['./skill-matrix-results-page.component.scss'],
})
export class SkillMatrixResultsPageComponent implements OnInit {
  sessionId = '';
  result?: AggregatedResult;
  loading = false;
  error = '';

  constructor(
    private route: ActivatedRoute,
    private api: SkillMatrixApiService
  ) {}

  ngOnInit(): void {
    this.sessionId = this.route.snapshot.paramMap.get('sessionId') ?? '';
    if (this.sessionId) {
      this.loadResult();
    }
  }

  loadResult(): void {
    if (!this.sessionId) {
      return;
    }

    this.loading = true;
    this.error = '';

    this.api.getFinalResult(this.sessionId).subscribe({
      next: res => {
        this.result = res;
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.error =
          err?.error?.message ||
          'Failed to load final skill matrix. Please try again.';
      },
    });
  }

  trackSkill(_index: number, skill: SkillScore): string {
    return skill.skillKey;
  }
}


