import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-weaknesses-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="rounded-3xl border border-slate-800/50 bg-gradient-to-br from-slate-900/80 to-slate-900/40 p-8 backdrop-blur-sm card-hover">
      <!-- Header -->
      <div class="flex items-center gap-3 mb-6">
        <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-amber-500/10">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
        </div>
        <div>
          <h2 class="text-xl font-semibold text-slate-100">Areas to Improve</h2>
          <p class="text-sm text-slate-500">Key signals that need attention</p>
        </div>
      </div>

      <!-- Weaknesses List -->
      <div class="space-y-4" *ngIf="weaknesses.length > 0; else noWeaknesses">
        <div
          *ngFor="let weakness of displayedWeaknesses; let i = index"
          class="flex items-start gap-4 p-4 rounded-2xl bg-amber-500/5 border border-amber-500/10 group hover:bg-amber-500/10 transition-colors"
        >
          <div class="flex h-8 w-8 items-center justify-center rounded-xl bg-amber-500/20 text-amber-400 font-semibold text-sm shrink-0">
            {{ i + 1 }}
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-slate-200 leading-relaxed">{{ weakness }}</p>
          </div>
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-amber-500/50 group-hover:text-amber-400 transition-colors shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
        </div>
      </div>

      <!-- No Weaknesses Placeholder -->
      <ng-template #noWeaknesses>
        <div class="flex flex-col items-center justify-center py-12 text-center">
          <div class="flex h-16 w-16 items-center justify-center rounded-full bg-emerald-500/10 mb-4">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <p class="text-emerald-400 mb-1">Looking great!</p>
          <p class="text-sm text-slate-500">No critical weaknesses detected in your profile</p>
        </div>
      </ng-template>

      <!-- Coverage Info -->
      <div *ngIf="weaknesses.length > 0" class="mt-6 pt-6 border-t border-slate-800/50">
        <div class="flex items-center justify-between mb-3">
          <span class="text-sm text-slate-400">Signal Coverage</span>
          <span class="text-sm font-medium text-slate-300">{{ coverage | number:'1.0-0' }}%</span>
        </div>
        <div class="h-2 rounded-full bg-slate-800 overflow-hidden">
          <div
            class="h-full rounded-full bg-gradient-to-r from-amber-500 to-orange-400 animate-progress"
            [style.width.%]="coverage"
          ></div>
        </div>
        <p class="text-xs text-slate-500 mt-3">
          These signals are identified from your profile and job requirements analysis.
        </p>
      </div>

      <!-- Show More (if more than 3) -->
      <div *ngIf="weaknesses.length > 3" class="mt-4 pt-4 border-t border-slate-800/50">
        <button
          (click)="showAll = !showAll"
          class="text-sm text-amber-400 hover:text-amber-300 transition-colors flex items-center gap-2"
        >
          <span>{{ showAll ? 'Show less' : 'Show all ' + weaknesses.length + ' areas' }}</span>
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-4 w-4 transition-transform"
            [class.rotate-180]="showAll"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            stroke-width="2"
          >
            <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
          </svg>
        </button>
      </div>
    </div>
  `
})
export class WeaknessesCardComponent {
  @Input() weaknesses: string[] = [];
  @Input() coverage = 0;
  showAll = false;

  get displayedWeaknesses(): string[] {
    return this.showAll ? this.weaknesses : this.weaknesses.slice(0, 3);
  }
}
