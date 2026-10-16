import { Component, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { NavbarComponent } from './components/navbar/navbar.component';
import { filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, NavbarComponent],
  template: `
    <div class="flex min-h-screen flex-col">
      <app-navbar *ngIf="showNavbar"></app-navbar>

      <main class="flex-1">
        <div class="mx-auto max-w-[1680px] px-4 py-10 sm:px-10 sm:py-14 xl:px-16">
          <router-outlet></router-outlet>
        </div>
      </main>

      <footer class="site-footer border-t border-line">
        <div class="mx-auto flex max-w-[1680px] flex-col gap-2 px-4 py-8 sm:flex-row sm:items-center sm:justify-between sm:px-10 xl:px-16">
          <p class="flex items-center gap-2 font-extrabold tracking-tight">
            <span class="h-2 w-2 rounded-full bg-accent"></span>careerrisk.ee
          </p>
          <p class="kicker">
            <a href="/jobs" class="hover:text-fg">All jobs</a> &middot; <a routerLink="/why" class="hover:text-fg">Why I built this</a>
            &middot; a catalog model, not a forecast
            &middot; <a routerLink="/privacy" class="hover:text-fg">Privacy</a> &middot; <a routerLink="/terms" class="hover:text-fg">Terms</a>
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
