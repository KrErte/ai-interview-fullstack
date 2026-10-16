import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';

/** Full-page navigation to another origin (the payment provider). Wrapped so specs can spy on it. */
@Injectable({ providedIn: 'root' })
export class BrowserRedirectService {
  private readonly document = inject(DOCUMENT);

  assign(url: string): void {
    this.document.defaultView?.location.assign(url);
  }
}
