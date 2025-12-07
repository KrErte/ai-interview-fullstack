import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JobSeekerUserService } from '../../services/job-seeker-user.service';

@Component({
  selector: 'app-job-seeker-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class JobSeekerDashboardComponent implements OnInit {
  userName = '';

  constructor(private userService: JobSeekerUserService) {}

  ngOnInit(): void {
    const user = this.userService.getCurrentUser();
    this.userName = user ? user.name || user.email : '';
  }
}
