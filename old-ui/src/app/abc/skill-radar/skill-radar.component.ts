import { Component, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
@Component({
  selector: 'app-skill-radar',
  standalone: true,
  templateUrl: './skill-radar.component.html',
  styleUrls: ['./skill-radar.component.scss'],
})
export class SkillRadarComponent implements AfterViewInit {
  @ViewChild('radarCanvas',{static:true}) radarCanvas!: ElementRef<HTMLCanvasElement>;
  ngAfterViewInit() {}
}
