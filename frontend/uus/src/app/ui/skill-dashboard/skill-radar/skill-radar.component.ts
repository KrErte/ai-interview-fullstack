import { Component, ElementRef, ViewChild, AfterViewInit } from '@angular/core';

@Component({
  selector: 'app-skill-radar',
  standalone: true,
  templateUrl: './skill-radar.component.html',
  styleUrls: ['./skill-radar.component.scss'],
})
export class SkillRadarComponent implements AfterViewInit {
  @ViewChild('radarCanvas', { static: true })
  radarCanvas!: ElementRef<HTMLCanvasElement>;

  ngAfterViewInit(): void {
    // Siia hiljem Chart.js või muu radar-chart
    const canvas = this.radarCanvas.nativeElement;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      return;
    }

    // Väike placeholder jaotus, et kaart ei oleks tühi
    ctx.font = '12px system-ui';
    ctx.textAlign = 'center';
    ctx.fillText('Soft skill radar (WIP)', canvas.width / 2, canvas.height / 2);
  }
}
