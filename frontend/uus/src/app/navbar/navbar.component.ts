import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';   // 👈 lisa see

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent {

  constructor(
    public auth: AuthService,    // public -> saab HTML-is kasutada
    private router: Router
  ) {}

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);  // viib login ekraanile
  }
}
