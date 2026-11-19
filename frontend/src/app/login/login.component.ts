import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, EventEmitter, OnInit, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { RevealDirective } from '../landing/shared/reveal.directive';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RevealDirective],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  @Output() authenticated = new EventEmitter<void>();

  loading = false;
  error = '';
  showPassword = false;

  readonly loginForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
    remember: [true],
  });

  ngOnInit(): void {
    const savedEmail = localStorage.getItem('cdg-remembered-email');
    if (savedEmail) {
      this.loginForm.patchValue({
        email: savedEmail,
        remember: true,
      });
    }
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  async submit(): Promise<void> {
    if (this.loginForm.invalid || this.loading) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.error = '';

    const { email, password, remember } = this.loginForm.getRawValue();

    try {
      await this.auth.login(email, password);

      if (remember) {
        localStorage.setItem('cdg-remembered-email', email.trim());
      } else {
        localStorage.removeItem('cdg-remembered-email');
      }

      this.authenticated.emit();
      await this.router.navigate(['/dashboard']);
    } catch {
      this.error = 'Email ou mot de passe incorrect.';
    } finally {
      this.loading = false;
      this.changeDetectorRef.markForCheck();
    }
  }
}
