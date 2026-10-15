import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <nav class="sticky top-0 z-50 border-b border-line bg-[rgba(11,13,18,0.85)] backdrop-blur-md print:hidden">
      <div class="mx-auto flex max-w-[1680px] items-center justify-between gap-4 px-4 py-4 sm:px-10 xl:px-16">
        <a routerLink="/" class="flex items-center gap-2 text-[19px] font-extrabold tracking-tight">
          <span class="h-2.5 w-2.5 rounded-full bg-accent"></span>
          careerrisk.ee
        </a>

        <div class="flex items-center gap-5 text-[14px]">
          <a routerLink="/ai-test" routerLinkActive="!text-accent" class="text-muted transition-colors hover:text-fg">Take the test</a>

          <ng-container *ngIf="auth.isLoggedIn(); else authLinks">
            <span class="kicker hidden max-w-[18ch] truncate md:inline">
              {{ auth.getCurrentUserName() || auth.getCurrentUserEmail() }}
            </span>
            <button (click)="logout()" class="btn-ghost !px-4 !py-2 !text-[13px]">Log out</button>
          </ng-container>

          <ng-template #authLinks>
            <a routerLink="/login" class="btn-ghost !px-4 !py-2 !text-[13px]">Log in</a>
          </ng-template>
        </div>
      </div>
    </nav>
  `
})
export class NavbarComponent {
  constructor(
    public auth: AuthService,
    private router: Router
  ) {}

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
