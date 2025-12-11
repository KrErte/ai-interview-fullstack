import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  TRAINING_TASK_DETAILS,
  TrainingApiService,
  TrainingStatus,
  TrainingTask
} from '../../core/services/training-api.service';

@Component({
  selector: 'app-training',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './training.component.html'
})
export class TrainingComponent implements OnInit {
  loading = false;
  error = '';
  tasks: TrainingTaskView[] = [];
  status?: TrainingStatus;

  private readonly taskDetails = TRAINING_TASK_DETAILS;

  constructor(private training: TrainingApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load() {
    this.loading = true;
    this.error = '';
    this.training.getTrainingStatus().subscribe({
      next: (status) => {
        this.status = status;
        this.tasks = this.buildTaskViews(status);
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Failed to load training data.';
      }
    });
  }

  toggle(task: TrainingTaskView) {
    this.training.setTaskCompleted(task.taskKey, !task.completed).subscribe({
      next: (status) => {
        this.status = status;
        this.tasks = this.buildTaskViews(status);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Unable to update task.';
      }
    });
  }

  nextTask(): TrainingTaskView | undefined {
    return this.tasks.find(t => !t.completed);
  }

  private buildTaskViews(status: TrainingStatus): TrainingTaskView[] {
    return status.tasks.map(task => ({
      ...task,
      ...this.lookupDetails(task.taskKey)
    }));
  }

  private lookupDetails(taskKey: string): TaskDetails {
    const details = this.taskDetails[taskKey];
    if (details) return details;

    const prettyKey = taskKey
      .split('-')
      .map(part => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');

    return { title: prettyKey, description: '' };
  }
}

type TaskDetails = { title: string; description: string };
type TrainingTaskView = TrainingTask & TaskDetails;

