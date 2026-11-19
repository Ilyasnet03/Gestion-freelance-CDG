import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService, AuthUser } from './api.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  readonly currentUser = signal<AuthUser | null>(null);
  readonly token = computed(() => this.api.token);
  readonly role = computed(() => this.currentUser()?.role ?? null);
  readonly isAuthenticated = computed(() => !!this.currentUser() && !!this.token());

  constructor() {
    this.checkInitialSession();
  }

  async checkInitialSession(): Promise<AuthUser | null> {
    if (!this.api.token) {
      return null;
    }
    try {
      const user = await this.api.get<AuthUser>('/api/auth/me');
      this.currentUser.set(user);
      return user;
    } catch {
      this.api.clearToken();
      this.currentUser.set(null);
      return null;
    }
  }

  async login(email: string, password: string): Promise<AuthUser> {
    const response = await this.api.post<{
      token: string;
      utilisateur: AuthUser;
    }>('/api/auth/login', {
      email: email.trim(),
      motDePasse: password,
    });

    this.api.setToken(response.token);
    this.currentUser.set(response.utilisateur);
    return response.utilisateur;
  }

  logout(): void {
    this.api.clearToken();
    this.currentUser.set(null);
    this.router.navigate(['/login']).catch(() => {});
  }
}
