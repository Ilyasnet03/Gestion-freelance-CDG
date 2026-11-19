import { Component, EventEmitter, Output } from '@angular/core';
import { LoginComponent } from '../../login/login.component';

@Component({
  selector: 'app-landing-login-section',
  standalone: true,
  imports: [LoginComponent],
  templateUrl: './landing-login-section.component.html',
  styleUrl: './landing-login-section.component.scss',
})
export class LandingLoginSectionComponent {
  @Output() authenticated = new EventEmitter<void>();
}
