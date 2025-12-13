import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  InterviewDebugState,
  InterviewDebugStateService
} from '../../core/services/interview-debug-state.service';

@Component({
  selector: 'app-control-room-drawer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './interview-control-room-drawer.component.html',
  styleUrls: ['./interview-control-room-drawer.component.scss']
})
export class InterviewControlRoomDrawerComponent {
  readonly state$ = this.debugState.state$;

  constructor(private debugState: InterviewDebugStateService) {}

  close(): void {
    this.debugState.setDrawerOpen(false);
  }

  formatJson(value: unknown): string {
    try {
      if (value === null || value === undefined) {
        return 'null';
      }
      return JSON.stringify(value, null, 2);
    } catch {
      return '«unserializable»';
    }
  }

  copy(text: string | null | undefined): void {
    if (!text) {
      return;
    }
    if (navigator && 'clipboard' in navigator && navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(text).catch(() => {
        // swallow copy errors
      });
      return;
    }

    try {
      const textarea = document.createElement('textarea');
      textarea.value = text;
      textarea.style.position = 'fixed';
      textarea.style.opacity = '0';
      document.body.appendChild(textarea);
      textarea.focus();
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);
    } catch {
      // ignore
    }
  }

  copyJson(value: unknown): void {
    this.copy(this.formatJson(value));
  }

  buildPostmanBody(state: InterviewDebugState): string {
    const sessionUuid = state.sessionInfo?.sessionUuid ?? '';
    const body = {
      sessionUuid,
      answer: '<answer placeholder>'
    };
    return JSON.stringify(body, null, 2);
  }
}


