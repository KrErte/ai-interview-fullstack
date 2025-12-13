import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import {
  DEMO_OBSERVER_LOG,
  ObserverLogEntry,
  ObserverLogService,
} from './observer-log.service';

@Component({
  selector: 'app-observer-log-panel',
  templateUrl: './observer-log-panel.component.html',
})
export class ObserverLogPanelComponent implements OnInit, OnDestroy {
  // Template expects this to exist as a component property.
  readonly DEMO_OBSERVER_LOG = DEMO_OBSERVER_LOG;

  entries: ObserverLogEntry[] = [];
  private sub?: Subscription;

  constructor(private readonly logService: ObserverLogService) {}

  ngOnInit(): void {
    // Old panel expects get$(); service supports it for compatibility.
    this.sub = this.logService.get$().subscribe((all) => {
      this.entries = all ?? [];
    });

    // Optional: emit a single demo log so you instantly see it in UI
    if (this.DEMO_OBSERVER_LOG) {
      this.logService.log('START', 'ObserverLogPanel connected (demo)', { source: 'observer-log-panel' }, 'DEBUG');
    }
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  clear(): void {
    this.logService.clear();
  }

  // If your template uses label mapping, keep it deterministic.
  typeLabel(t: ObserverLogEntry['type']): string {
    switch (t) {
      case 'START':
        return 'Start';
      case 'SUBMIT':
        return 'Submit';
      case 'NEXT_Q':
        return 'Next question';
      case 'SIGNAL':
        return 'Signal';
      case 'ERROR':
        return 'Error';
      case 'DECISION':
        return 'Decision';
      case 'SCORING':
        return 'Scoring';
      case 'API':
        return 'API';
      case 'STATE':
        return 'State';
      case 'UI':
        return 'UI';
      case 'PERSISTENCE':
        return 'Persistence';
      case 'GUARD':
        return 'Guard';
      default:
        return String(t);
    }
  }
}
