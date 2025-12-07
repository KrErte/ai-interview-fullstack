import {
  Component,
  Input,
  OnInit,
  OnDestroy,
  ViewChild,
  ElementRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import Chart from 'chart.js/auto';
import { environment } from '../../environments/environment';
import { AuthService } from '../services/auth.service';

export interface SkillItem {
  skillName: string;
  level: number;
  category: string;
  completedTasks?: number;
  totalTasks?: number;
}

export interface SkillMatrixResponse {
  skills: SkillItem[];
  averageLevel: number;
  currentLevel: string;
  nextLevel: string;
  topGaps?: SkillItem[];
}

@Component({
  selector: 'app-skill-matrix',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './skill-matrix.component.html',
  styleUrls: ['./skill-matrix.component.scss'],
})
export class SkillMatrixComponent implements OnInit, OnDestroy {
  @Input() email: string | null = null;

  private readonly apiBaseUrl = environment.apiBaseUrl || environment.apiUrl;

  skills: SkillItem[] = [];
  topGaps: SkillItem[] = [];
  currentLevel = 'Junior';
  nextLevel = 'Mid-level';

  isLoading = false;
  errorMessage = '';

  // Radar chart
  @ViewChild('radarCanvas') radarCanvas?: ElementRef<HTMLCanvasElement>;
  private radarChart?: Chart;

  // kategooria -> emoji ikoon
  private readonly categoryIcons: Record<string, string> = {
    soft: '🤝',
    tech: '💻',
    leadership: '🧠',
    communication: '🗣',
    ownership: '🚀',
  };

  // AI coach popup state
  isCoachOpen = false;
  isCoachLoading = false;
  coachResponse: string | null = null;
  selectedSkillName: string | null = null;

  constructor(private http: HttpClient, private auth: AuthService) {}

  ngOnInit(): void {
    const effectiveEmail =
      this.email ||
      this.auth.getCurrentUserEmail() ||
      localStorage.getItem('aiim_email') ||
      'demo-user@example.com';

    this.loadSkillMatrix(effectiveEmail);
  }

  ngOnDestroy(): void {
    if (this.radarChart) {
      this.radarChart.destroy();
    }
  }

  getIconFor(skill: SkillItem): string {
    return this.categoryIcons[skill.category] || '📘';
  }

  // ==========================
  //   BACKENDIST MATRIX
  // ==========================

  private loadSkillMatrix(email: string): void {
    this.isLoading = true;
    this.errorMessage = '';

    const params = new HttpParams().set('email', email);

    this.http
      .get<SkillMatrixResponse>(`${this.apiBaseUrl}/api/skills/matrix`, { params })
      .subscribe({
        next: (response) => {
          this.skills = response.skills || [];
          this.topGaps = response.topGaps || [];

          if (response.currentLevel) {
            this.currentLevel = response.currentLevel;
          }
          if (response.nextLevel) {
            this.nextLevel = response.nextLevel;
          }

          this.isLoading = false;
          this.buildRadarChart();
        },
        error: (err) => {
          console.error('Skill-matrixi laadimine ebaõnnestus', err);
          this.errorMessage = 'Skill-matrixi laadimine ebaõnnestus.';
          this.isLoading = false;
        },
      });
  }

  private buildRadarChart(): void {
    if (!this.radarCanvas || this.skills.length === 0) {
      return;
    }

    const ctx = this.radarCanvas.nativeElement.getContext('2d');
    if (!ctx) {
      return;
    }

    if (this.radarChart) {
      this.radarChart.destroy();
    }

    const labels = this.skills.map((s) => s.skillName);
    const data = this.skills.map((s) => s.level);

    this.radarChart = new Chart(ctx, {
      type: 'radar',
      data: {
        labels,
        datasets: [
          {
            label: 'Oskuste tase (%)',
            data,
            fill: true,
            pointRadius: 4,
            pointHoverRadius: 6,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          r: {
            suggestedMin: 0,
            suggestedMax: 100,
            ticks: {
              stepSize: 20,
            },
            grid: {
              circular: true,
            },
          },
        },
        plugins: {
          legend: {
            display: false,
          },
        },
      },
    });
  }

  // ==========================
  //        AI COACH
  // ==========================

  onSkillClick(skill: SkillItem): void {
    const email =
      this.email ||
      this.auth.getCurrentUserEmail() ||
      localStorage.getItem('aiim_email') ||
      'demo-user@example.com';

    this.selectedSkillName = skill.skillName;
    this.isCoachOpen = true;
    this.isCoachLoading = true;
    this.coachResponse = null;

    const body = {
      email,
      skillName: skill.skillName,
    };

    this.http
      .post(`${this.apiBaseUrl}/api/skills/coach`, body, {
        responseType: 'text',
      })
      .subscribe({
        next: (response) => {
          this.coachResponse = response;
          this.isCoachLoading = false;
        },
        error: (err) => {
          console.error('AI coach viga', err);
          this.coachResponse = 'AI coachi vastuse küsimine ebaõnnestus.';
          this.isCoachLoading = false;
        },
      });
  }

  closeCoach(): void {
    this.isCoachOpen = false;
    this.coachResponse = null;
    this.selectedSkillName = null;
    this.isCoachLoading = false;
  }
}
