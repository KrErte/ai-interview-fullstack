import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-job-seeker-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './job-seeker-sidebar.component.html',
  styleUrls: ['./job-seeker-sidebar.component.scss']
})
export class JobSeekerSidebarComponent {}
