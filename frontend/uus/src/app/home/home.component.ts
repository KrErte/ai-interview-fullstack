import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { PingService } from '../services/ping.service';
import { CommonModule } from '@angular/common';

@Component({
  standalone: true,
  imports: [RouterModule, CommonModule],
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent {
  pingResult = '';
  pingError = '';
  loading = false;

  constructor(private pingService: PingService) {}

  pingBackend() {
    this.pingResult = '';
    this.pingError = '';
    this.loading = true;
    this.pingService.ping().subscribe({
      next: res => {
        this.pingResult = res;
        this.loading = false;
      },
      error: err => {
        this.pingError = err.message || 'Error contacting backend';
        this.loading = false;
      },
    });
  }
}
