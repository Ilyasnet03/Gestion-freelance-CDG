import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { LoginComponent } from './login.component';
import { AuthService } from '../auth.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authService: AuthService;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create LoginComponent', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with invalid empty form and remember true', () => {
    expect(component.loginForm.valid).toBe(false);
    expect(component.loginForm.controls.remember.value).toBe(true);
    expect(component.loginForm.controls.email.value).toBe('');
    expect(component.loginForm.controls.password.value).toBe('');
  });

  it('should validate email format', () => {
    component.loginForm.controls.email.setValue('invalid-email');
    expect(component.loginForm.controls.email.valid).toBe(false);

    component.loginForm.controls.email.setValue('prenom.nom@cdg.ma');
    expect(component.loginForm.controls.email.valid).toBe(true);
  });

  it('should toggle password visibility and accessibility attributes', () => {
    expect(component.showPassword).toBe(false);
    component.togglePasswordVisibility();
    expect(component.showPassword).toBe(true);
    component.togglePasswordVisibility();
    expect(component.showPassword).toBe(false);
  });

  it('should not call auth.login if form is invalid', async () => {
    const loginSpy = vi.spyOn(authService, 'login');
    await component.submit();
    expect(loginSpy).not.toHaveBeenCalled();
    expect(component.loading).toBe(false);
  });

  it('should call auth.login and navigate to /dashboard on success', async () => {
    const loginSpy = vi.spyOn(authService, 'login').mockResolvedValue({
      id: 1,
      email: 'admin@cdg.ma',
      prenom: 'Ilyas',
      nom: 'BENLOUALI',
      role: 'ADMIN',
    });
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    component.loginForm.setValue({
      email: 'admin@cdg.ma',
      password: 'password123',
      remember: true,
    });

    await component.submit();

    expect(loginSpy).toHaveBeenCalledWith('admin@cdg.ma', 'password123');
    expect(navigateSpy).toHaveBeenCalledWith(['/dashboard']);
    expect(component.error).toBe('');
    expect(component.loading).toBe(false);
  });

  it('should show error banner when auth.login fails', async () => {
    vi.spyOn(authService, 'login').mockRejectedValue(new Error('Invalid credentials'));

    component.loginForm.setValue({
      email: 'admin@cdg.ma',
      password: 'wrongpassword',
      remember: true,
    });

    await component.submit();
    await fixture.whenStable();

    expect(component.error).toBe('Email ou mot de passe incorrect.');
    expect(component.loading).toBe(false);
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain(
      'Email ou mot de passe incorrect.',
    );
  });
});
