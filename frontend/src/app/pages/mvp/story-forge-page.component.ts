import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  MvpApiService,
  Story,
  StoryPayload,
} from '../../core/services/mvp-api.service';

@Component({
  selector: 'app-story-forge-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './story-forge-page.component.html',
})
export class StoryForgePageComponent implements OnInit {
  stories: Story[] = [];
  loading = false;
  saving = false;
  deletingId: string | null = null;
  error = '';

  showForm = false;
  editingStory: Story | null = null;

  form: StoryPayload = {
    title: '',
    situation: '',
    task: '',
    action: '',
    result: '',
    tag: '',
  };

  constructor(private mvpApi: MvpApiService) {}

  ngOnInit(): void {
    this.loadStories();
  }

  loadStories() {
    this.loading = true;
    this.error = '';
    this.mvpApi.getStories().subscribe({
      next: (stories) => {
        this.stories = stories || [];
        this.loading = false;
      },
      error: (err) => {
        this.error =
          err?.error?.message ||
          'Failed to load stories. Please try again in a moment.';
        this.loading = false;
      },
    });
  }

  startNewStory() {
    this.editingStory = null;
    this.showForm = true;
    this.form = {
      title: '',
      situation: '',
      task: '',
      action: '',
      result: '',
      tag: '',
    };
  }

  editStory(story: Story) {
    this.editingStory = story;
    this.showForm = true;
    this.form = {
      title: story.title,
      situation: story.situation,
      task: story.task,
      action: story.action,
      result: story.result,
      tag: story.tag,
    };
  }

  cancelForm() {
    this.showForm = false;
    this.editingStory = null;
  }

  saveStory() {
    if (!this.form.title.trim()) {
      this.error = 'Please add a title for your story.';
      return;
    }

    this.saving = true;
    this.error = '';

    const payload: StoryPayload = {
      title: this.form.title.trim(),
      situation: this.form.situation.trim(),
      task: this.form.task.trim(),
      action: this.form.action.trim(),
      result: this.form.result.trim(),
      tag: this.form.tag?.trim() || '',
    };

    const request$ = this.editingStory
      ? this.mvpApi.updateStory(this.editingStory.id, payload)
      : this.mvpApi.createStory(payload);

    request$.subscribe({
      next: (story) => {
        if (this.editingStory) {
          this.stories = this.stories.map((s) =>
            s.id === story.id ? story : s
          );
        } else {
          this.stories = [story, ...this.stories];
        }
        this.saving = false;
        this.showForm = false;
        this.editingStory = null;
      },
      error: (err) => {
        this.error =
          err?.error?.message ||
          'Failed to save story. Please try again in a moment.';
        this.saving = false;
      },
    });
  }

  deleteStory(story: Story) {
    if (!story.id) {
      return;
    }

    this.deletingId = story.id;
    this.error = '';

    this.mvpApi.deleteStory(story.id).subscribe({
      next: () => {
        this.stories = this.stories.filter((s) => s.id !== story.id);
        this.deletingId = null;
      },
      error: (err) => {
        this.error =
          err?.error?.message ||
          'Failed to delete story. Please try again in a moment.';
        this.deletingId = null;
      },
    });
  }

  formatDate(value?: string) {
    if (!value) return '';
    return new Date(value).toLocaleString();
  }
}


