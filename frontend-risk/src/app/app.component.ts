import { Component, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { NavbarComponent } from './components/navbar/navbar.component';
import { filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, NavbarComponent],
  template: `
    <div class="min-h-screen bg-slate-950 text-slate-100">
      <!-- Navbar -->
      <app-navbar *ngIf="showNavbar"></app-navbar>

      <!-- Main Content -->
      <main class="relative">
        <!-- Subtle background gradient -->
        <div class="absolute inset-0 bg-gradient-to-b from-slate-950 via-slate-950 to-slate-900 pointer-events-none"></div>
        <div class="absolute top-0 left-1/2 -translate-x-1/2 w-[800px] h-[600px] bg-emerald-500/5 rounded-full blur-3xl pointer-events-none"></div>

        <!-- Page content -->
        <div class="relative mx-auto max-w-4xl px-6 py-12">
          <router-outlet></router-outlet>
        </div>
      </main>

      <!-- Footer -->
      <footer class="border-t border-slate-800/50 py-8 mt-12">
        <div class="mx-auto max-w-4xl px-6 text-center">
          <p class="text-sm text-slate-500">
            Futureproof Risk Assessment &middot; Powered by AI
          </p>
        </div>
      </footer>
    </div>
  `
})
export class AppComponent {
  showNavbar = true;

  constructor(private router: Router, destroyRef: DestroyRef) {
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(destroyRef)
      )
      .subscribe((event: NavigationEnd) => {
        const path = event.urlAfterRedirects.split('?')[0];
        this.showNavbar = path !== '/login' && path !== '/register';
      });
  }
}
