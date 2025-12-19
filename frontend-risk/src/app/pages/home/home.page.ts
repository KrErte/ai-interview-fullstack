import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="min-h-[80vh] flex flex-col items-center justify-center text-center px-4">
      <!-- Hero Section -->
      <div class="max-w-3xl mx-auto">
        <!-- Logo Animation -->
        <div class="relative mb-8">
          <div class="absolute inset-0 bg-emerald-500/20 rounded-full blur-3xl animate-pulse-slow"></div>
          <div class="relative inline-flex h-24 w-24 items-center justify-center rounded-3xl bg-gradient-to-br from-emerald-500 to-teal-600 shadow-2xl shadow-emerald-500/30">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
          </div>
        </div>

        <!-- Title -->
        <h1 class="text-5xl sm:text-6xl font-bold text-slate-100 mb-6 leading-tight">
          <span class="gradient-text">Futureproof</span><br/>
          Your Career
        </h1>

        <!-- Subtitle -->
        <p class="text-xl text-slate-400 mb-12 max-w-xl mx-auto leading-relaxed">
          AI-powered career risk assessment that helps you identify gaps,
          leverage strengths, and build a personalized roadmap to success.
        </p>

        <!-- CTA Buttons -->
        <div class="flex flex-col sm:flex-row items-center justify-center gap-4">
          <a
            [routerLink]="auth.isLoggedIn() ? '/risk' : '/login'"
            class="group flex items-center gap-3 rounded-2xl bg-gradient-to-r from-emerald-500 to-teal-500 px-8 py-4 font-semibold text-white shadow-xl shadow-emerald-500/25 transition-all hover:shadow-emerald-500/40 hover:scale-[1.02]"
          >
            Get Started
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 transition-transform group-hover:translate-x-1" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
            </svg>
          </a>

          <a
            *ngIf="!auth.isLoggedIn()"
            routerLink="/register"
            class="flex items-center gap-2 rounded-2xl border border-slate-700 bg-slate-800/50 px-8 py-4 font-semibold text-slate-300 transition-all hover:border-slate-600 hover:bg-slate-800 hover:text-white"
          >
            Create Account
          </a>
        </div>
      </div>

      <!-- Features Grid -->
      <div class="grid md:grid-cols-3 gap-6 mt-20 max-w-4xl mx-auto w-full">
        <div class="rounded-2xl border border-slate-800/50 bg-slate-900/40 p-6 text-left card-hover">
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-500/10 mb-4">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
            </svg>
          </div>
          <h3 class="text-lg font-semibold text-slate-100 mb-2">Risk Analysis</h3>
          <p class="text-slate-400 text-sm leading-relaxed">
            Get a clear picture of your career readiness with AI-powered risk scoring.
          </p>
        </div>

        <div class="rounded-2xl border border-slate-800/50 bg-slate-900/40 p-6 text-left card-hover">
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-500/10 mb-4">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          </div>
          <h3 class="text-lg font-semibold text-slate-100 mb-2">Actionable Insights</h3>
          <p class="text-slate-400 text-sm leading-relaxed">
            Understand your strengths and weaknesses with detailed, actionable feedback.
          </p>
        </div>

        <div class="rounded-2xl border border-slate-800/50 bg-slate-900/40 p-6 text-left card-hover">
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-amber-500/10 mb-4">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
            </svg>
          </div>
          <h3 class="text-lg font-semibold text-slate-100 mb-2">Personal Roadmap</h3>
          <p class="text-slate-400 text-sm leading-relaxed">
            Get a customized learning path to address gaps and accelerate your growth.
          </p>
        </div>
      </div>
    </div>
  `
})
export class HomePageComponent {
  constructor(public auth: AuthService) {}
}
