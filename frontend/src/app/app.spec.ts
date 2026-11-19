import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';
import { AuthService } from './auth.service';

describe('App', () => {
  beforeEach(async () => {
    sessionStorage.clear();
    localStorage.removeItem('cdg-remembered-email');

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter(routes)],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the public landing page sections', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('#accueil h1')?.textContent).toContain('Gérez vos freelances');
    expect(compiled.querySelector('#fonctionnalites')).not.toBeNull();
    expect(compiled.querySelector('#processus')).not.toBeNull();
    expect(compiled.querySelector('#connexion')).not.toBeNull();
  });

  it('should keep the public landing page at / for an authenticated user', () => {
    const fixture = TestBed.createComponent(App);
    TestBed.inject(AuthService).currentUser.set({
      id: 1,
      email: 'user@cdg.ma',
      prenom: 'Utilisateur',
      nom: 'CDG',
      role: 'ADMIN',
    });
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('#accueil')).not.toBeNull();
  });

  it('should display the dashboard after login data loads', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.autoDetectChanges();
    const app = fixture.componentInstance;
    const http = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    const loadViewSpy = vi.spyOn(app, 'loadView');

    const email = fixture.nativeElement.querySelector('#email') as HTMLInputElement;
    const password = fixture.nativeElement.querySelector('#password') as HTMLInputElement;
    email.value = 'admin@cdg.ma';
    email.dispatchEvent(new Event('input', { bubbles: true }));
    password.value = 'password123';
    password.dispatchEvent(new Event('input', { bubbles: true }));
    fixture.detectChanges();

    const loginButton = fixture.nativeElement.querySelector('.login-btn') as HTMLButtonElement;
    expect(loginButton.disabled).toBe(false);
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(
      new Event('submit', { bubbles: true, cancelable: true }),
    );

    http.expectOne('/api/auth/login').flush({
      token: 'test-token',
      utilisateur: {
        id: 1,
        email: 'admin@cdg.ma',
        prenom: 'Ilyas',
        nom: 'BENLOUALI',
        role: 'ADMIN',
      },
    });

    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne('/api/dashboard/kpi').flush({ projetsActifsCount: 1 });
    http.expectOne('/api/projets').flush([
      { id: 1, nom: 'Projet de démonstration' },
    ]);
    await loadViewSpy.mock.results[0].value;
    await fixture.whenStable();

    const rendered = fixture.nativeElement as HTMLElement;
    expect(rendered.querySelector('.app-shell')).not.toBeNull();
    expect(rendered.textContent).toContain('Projet de démonstration');
    expect(router.url).toBe('/dashboard');
    http.verify();
  });
});
