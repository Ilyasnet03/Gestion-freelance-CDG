import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export type ApiRow = Record<string, unknown>;

export interface AuthUser extends ApiRow {
  id: number;
  email: string;
  prenom: string;
  nom: string;
  role: 'ADMIN' | 'CHEF_PROJET' | 'FREELANCE';
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly tokenKey = 'cdg-token';

  get token(): string | null {
    return sessionStorage.getItem(this.tokenKey);
  }

  setToken(token: string): void {
    sessionStorage.setItem(this.tokenKey, token);
  }

  clearToken(): void {
    sessionStorage.removeItem(this.tokenKey);
  }

  get<T>(path: string): Promise<T> {
    return this.request<T>(path, 'GET');
  }

  post<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>(path, 'POST', body);
  }

  put<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>(path, 'PUT', body);
  }

  patch<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>(path, 'PATCH', body);
  }

  delete<T>(path: string): Promise<T> {
    return this.request<T>(path, 'DELETE');
  }

  async download(path: string, filename: string): Promise<void> {
    const headers = this.headers();
    const blob = await firstValueFrom(this.http.get(path, { headers, responseType: 'blob' }));
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
  }

  private async request<T>(path: string, method: string, body?: unknown): Promise<T> {
    try {
      return await firstValueFrom(
        this.http.request<T>(method, path, {
          headers: this.headers(),
          body,
        }),
      );
    } catch (error: unknown) {
      const message = this.errorMessage(error);
      if (message.status === 401) {
        this.clearToken();
      }
      throw new Error(message.message, { cause: error });
    }
  }

  private headers(): HttpHeaders {
    const token = this.token;
    return token ? new HttpHeaders({ Authorization: `Bearer ${token}` }) : new HttpHeaders();
  }

  private errorMessage(error: unknown): { status: number; message: string } {
    if (error instanceof HttpErrorResponse) {
      const errorBody = error.error as { message?: string; error?: string } | undefined;
      return {
        status: error.status,
        message: errorBody?.message ?? errorBody?.error ?? `Erreur HTTP ${error.status}`,
      };
    }
    return { status: 0, message: 'Le serveur est inaccessible.' };
  }
}
