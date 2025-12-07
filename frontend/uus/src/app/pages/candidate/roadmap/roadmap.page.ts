import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { JobMatchStateService } from '../../../services/job-match-state.service';
import { JobMatchResult } from '../../../services/job-match.service';
import { SkillPlanDay, SkillPlanRequest } from '../../../models/skill-plan.model';
import { SkillPlanService } from '../../../services/skill-plan.service';
import { AuthService } from '../../../services/auth.service';
import { RoadmapService, RoadmapTask } from '../../../services/roadmap.service';
import { TrainingProgressResponse, TrainingProgressService } from '../../../services/training-progress.service';

interface RoadmapDayView extends SkillPlanDay {
  completed?: boolean;
  taskKey?: string;
}

@Component({
  selector: 'app-roadmap-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './roadmap.page.html',
  styleUrls: ['./roadmap.page.scss']
})
export class RoadmapPageComponent implements OnInit {
  lastResult: JobMatchResult | null = null;
  matchPercent: number | null = null;
  trainingSessionId: string | null = null;

  planOptions = [7, 14, 30];
  selectedDays = 14;

  overallGoal = '';
  roadmap: RoadmapDayView[] = [];

  loadingPlan = false;
  loadingRoadmap = false;
  loadError: string | null = null;
  planError: string | null = null;

  constructor(
    private readonly jobMatchState: JobMatchStateService,
    private readonly skillPlanService: SkillPlanService,
    private readonly authService: AuthService,
    private readonly roadmapService: RoadmapService,
    private readonly trainingProgressService: TrainingProgressService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.lastResult = this.jobMatchState.getLastResult();
    this.matchPercent = this.toPercent(this.lastResult?.matchScore ?? null);
    this.trainingSessionId = this.jobMatchState.getCurrentTrainingSessionId();

    const email = this.authService.getCurrentUserEmail();
    if (!email) {
      this.loadError = 'Please log in again to load your roadmap.';
      return;
    }

    this.fetchRoadmap(email);
    this.fetchProgress(email);
  }

  generateRoadmap(): void {
    if (!this.lastResult) {
      this.planError = 'No recent job match found. Run the analysis first.';
      return;
    }

    const payload: SkillPlanRequest = {
      email: this.authService.getCurrentUserEmail() ?? '',
      jobMatcherSummary: this.lastResult.summary ?? '',
      focusSkills: this.lastResult.missingSkills ?? []
    };

    this.loadingPlan = true;
    this.planError = null;

    this.skillPlanService.generatePlan(payload).subscribe({
      next: (res) => {
        const days = res?.days ?? [];
        this.overallGoal = res?.overallGoal ?? '';
        this.roadmap = this.selectedDays > 0 ? days.slice(0, this.selectedDays) : days;
        this.loadingPlan = false;
      },
      error: (err) => {
        this.planError = err?.error?.message || 'Could not generate roadmap. Please try again.';
        this.loadingPlan = false;
      }
    });
  }

  startTraining(): void {
    this.goToCoach();
  }

  goToCoach(): void {
    this.router.navigate(['/candidate/coach']);
  }

  goToJobMatch(): void {
    this.router.navigate(['/candidate/job-match']);
  }

  private fetchRoadmap(email: string): void {
    this.loadingRoadmap = true;
    this.loadError = null;

    this.roadmapService.getRoadmap(email).subscribe({
      next: (tasks) => {
        this.roadmap = this.mapRoadmapTasks(tasks);
        this.loadingRoadmap = false;
      },
      error: () => {
        this.loadError = 'Could not load your roadmap. Please try again.';
        this.loadingRoadmap = false;
      }
    });
  }

  private fetchProgress(email: string): void {
    this.trainingProgressService.getProgress(email).subscribe({
      next: (progress: TrainingProgressResponse) => {
        if (!this.lastResult && (progress.lastMatchScore != null || progress.lastMatchSummary)) {
          this.lastResult = {
            matchScore: progress.lastMatchScore ?? null,
            summary: progress.lastMatchSummary ?? '',
            missingSkills: []
          };
          this.matchPercent = this.toPercent(progress.lastMatchScore);
        } else if (this.matchPercent == null && progress.lastMatchScore != null) {
          this.matchPercent = this.toPercent(progress.lastMatchScore);
        }
      },
      error: () => {
        // silent fallback; keep UI usable with local state
      }
    });
  }

  private mapRoadmapTasks(tasks: RoadmapTask[]): RoadmapDayView[] {
    if (!tasks || !tasks.length) {
      return [];
    }

    return tasks.map((task, index) => ({
      dayNumber: index + 1,
      title: task.title || `Roadmap step ${index + 1}`,
      description: task.description || task.taskKey,
      practiceTask: task.description || 'Practice this focus area.',
      completed: task.completed,
      taskKey: task.taskKey
    }));
  }

  private toPercent(score: number | null | undefined): number | null {
    if (score == null) {
      return null;
    }
    return score > 1 ? Math.round(score) : Math.round(score * 100);
  }
}

