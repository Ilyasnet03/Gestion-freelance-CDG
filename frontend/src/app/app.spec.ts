import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MatDialog } from '@angular/material/dialog';
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

  it('opens project task details in a dialog', async () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    app.user = {
      id: 1,
      email: 'admin@cdg.ma',
      prenom: 'Ilyas',
      nom: 'BENLOUALI',
      role: 'ADMIN',
    };
    fixture.detectChanges();
    app.currentView = 'projects';
    fixture.detectChanges();
    await TestBed.inject(Router).navigateByUrl('/dashboard');
    fixture.detectChanges();

    const http = TestBed.inject(HttpTestingController);
    const opening = app.openDetails({ id: 42, nom: 'Projet de test' });
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne('/api/projets/42/taches').flush([
      { id: 7, titre: 'Tâche visible', statut: 'EN_COURS', avancement: 25 },
    ]);
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne('/api/projets/42/affectations').flush([]);
    await opening;
    fixture.detectChanges();

    const dialog = TestBed.inject(MatDialog);
    expect(dialog.openDialogs).toHaveLength(1);
    expect(document.querySelector('.cdk-overlay-container')?.textContent).toContain(
      'Tâche visible',
    );
    dialog.closeAll();
    http.verify();
  });

  it('opens timesheet entries in a dialog', async () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    app.user = {
      id: 2,
      email: 'cdp.alami@cdg.ma',
      prenom: 'Karim',
      nom: 'EL ALAMI',
      role: 'CHEF_PROJET',
    };
    fixture.detectChanges();
    app.currentView = 'timesheets';
    fixture.detectChanges();
    await TestBed.inject(Router).navigateByUrl('/dashboard');
    fixture.detectChanges();

    const http = TestBed.inject(HttpTestingController);
    const opening = app.openDetails({ id: 9 });
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne('/api/timesheets/9').flush({
      id: 9,
      semaineDebut: '2026-03-02',
      lignes: [
        {
          dateSaisie: '2026-03-02',
          projetNom: 'Projet de test',
          tacheTitre: 'Saisie visible',
          heures: 7,
        },
      ],
    });
    await opening;
    fixture.detectChanges();

    const dialog = TestBed.inject(MatDialog);
    expect(dialog.openDialogs).toHaveLength(1);
    expect(document.querySelector('.cdk-overlay-container')?.textContent).toContain(
      'Saisie visible',
    );
    dialog.closeAll();
    http.verify();
  });

  it('routes a freelance user to missions instead of the management dashboard', async () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    app.user = {
      id: 8,
      email: 'freelance@cdg.ma',
      prenom: 'Nora',
      nom: 'Rivière',
      role: 'FREELANCE',
    };
    const http = TestBed.inject(HttpTestingController);

    const loading = app.loadView();
    expect(app.currentView).toBe('my-missions');
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne('/api/mes-missions').flush([]);
    await loading;

    http.expectNone('/api/dashboard/kpi');
    http.expectNone('/api/projets');
    http.verify();
  });

  it('keeps project mutations and timesheet validation exclusive to the project manager', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;

    app.user = {
      id: 1,
      email: 'admin@cdg.ma',
      prenom: 'Admin',
      nom: 'CDG',
      role: 'ADMIN',
    };
    app.currentView = 'projects';
    expect(app.canCreate).toBe(false);
    app.currentView = 'timesheets';
    expect(app.actionLabel({ statut: 'SOUMISE' })).toBe('Détails');
    expect(app.canConfirmPayment({ paiementId: 2, paiementStatut: 'EN_ATTENTE' })).toBe(false);

    app.user = {
      id: 2,
      email: 'chef@cdg.ma',
      prenom: 'Chef',
      nom: 'CDG',
      role: 'CHEF_PROJET',
    };
    app.currentView = 'projects';
    expect(app.canCreate).toBe(true);
    app.currentView = 'timesheets';
    expect(app.actionLabel({ statut: 'SOUMISE' })).toBe('Valider');
    expect(app.canConfirmPayment({ paiementId: 2, paiementStatut: 'EN_ATTENTE' })).toBe(true);
    expect(app.canConfirmPayment({ paiementId: 2, paiementStatut: 'PAYE' })).toBe(false);
  });

  it('associates the project manager payment action with its timesheet', async () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    app.user = {
      id: 2,
      email: 'chef@cdg.ma',
      prenom: 'Chef',
      nom: 'CDG',
      role: 'CHEF_PROJET',
    };
    app.currentView = 'timesheets';
    const http = TestBed.inject(HttpTestingController);

    const loading = app.loadView();
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne('/api/timesheets').flush([
      { id: 10, statut: 'VALIDEE', semaineDebut: '2026-03-02' },
    ]);
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne('/api/paiements').flush([
      { id: 20, timesheetId: 10, statut: 'EN_ATTENTE' },
    ]);
    await loading;

    expect(app.rows[0]['paiementId']).toBe(20);
    expect(app.canConfirmPayment(app.rows[0])).toBe(true);
    http.verify();
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
