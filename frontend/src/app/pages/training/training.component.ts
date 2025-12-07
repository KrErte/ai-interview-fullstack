import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AiService, RoadmapTask } from '../../services/ai.service';
import { TrainingService, TrainingProgress } from '../../services/training.service';

@Component({
  selector: 'app-training',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './training.component.html'
})
export class TrainingComponent implements OnInit {
  loading = false;
  error = '';
  tasks: RoadmapTask[] = [];
  progress?: TrainingProgress;

  constructor(
    private ai: AiService,
    private training: TrainingService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load() {
    this.loading = true;
    this.error = '';
    forkJoin({
      tasks: this.ai.getRoadmap(),
      progress: this.training.getProgress()
    }).subscribe({
      next: ({ tasks, progress }) => {
        this.tasks = tasks || [];
        this.progress = progress;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Failed to load training data.';
      }
    });
  }

  toggle(task: RoadmapTask) {
    this.ai.updateRoadmapTask(task.taskKey, !task.completed).subscribe({
      next: (tasks) => {
        this.tasks = tasks || [];
        this.refreshProgress();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Unable to update task.';
      }
    });
  }

  refreshProgress() {
    this.training.getProgress().subscribe({
      next: (progress) => (this.progress = progress),
      error: () => {}
    });
  }

  nextTask(): RoadmapTask | undefined {
    return this.tasks.find(t => !t.completed);
  }
}

