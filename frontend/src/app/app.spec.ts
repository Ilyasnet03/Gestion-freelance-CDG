import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DialogService } from 'primeng/dynamicdialog';
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
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter(routes),
        DialogService,
      ],
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
    http
      .expectOne('/api/projets/42/taches')
      .flush([{ id: 7, titre: 'Tâche visible', statut: 'EN_COURS', avancement: 25 }]);
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne('/api/projets/42/affectations').flush([]);
    await opening;
    fixture.detectChanges();

    expect(app.detailDialogVisible).toBe(true);
    expect(document.querySelector('.p-dialog')?.textContent).toContain('Tâche visible');
    app.detailDialogVisible = false;
    http.verify();
  });

  it('proposes project hours per planned task and accepts a custom quota', async () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    app.selectedProject = { id: 42, nom: 'Projet', heuresPrevues: 100, nombreTachesPrevues: 4 };
    vi.spyOn(app, 'openDetails').mockResolvedValue();
    fixture.detectChanges();

    const creating = app.createTask();
    fixture.detectChanges();
    await fixture.whenStable();
    const hours = document.querySelector('#field-heuresPrevues') as HTMLInputElement;
    const title = document.querySelector('#field-titre') as HTMLInputElement;
    expect(hours.value).toBe('25');
    title.value = 'Nouvelle tâche';
    title.dispatchEvent(new Event('input'));
    hours.value = '17.25';
    hours.dispatchEvent(new Event('input'));
    hours.closest('form')!.dispatchEvent(new Event('submit'));
    await new Promise((resolve) => setTimeout(resolve, 0));
    const http = TestBed.inject(HttpTestingController);
    const request = http.expectOne('/api/projets/42/taches');
    expect(request.request.body.heuresPrevues).toBe(17.25);
    request.flush({});
    await creating;
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

    expect(app.detailDialogVisible).toBe(true);
    expect(document.querySelector('.p-dialog')?.textContent).toContain('Saisie visible');
    app.detailDialogVisible = false;
    http.verify();
  });

  it('restores cached page data immediately before refreshing', async () => {
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
    const http = TestBed.inject(HttpTestingController);

    const firstLoad = app.loadView();
    await new Promise((resolve) => setTimeout(resolve, 0));
    http
      .expectOne('/api/projets')
      .flush([{ id: 42, nom: 'Projet en cache', avancementGlobal: 25 }]);
    await firstLoad;

    const freelancersLoad = app.selectView('freelancers');
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne('/api/freelances').flush([]);
    await freelancersLoad;

    const cachedLoad = app.selectView('projects');
    expect(app.rows[0]['nom']).toBe('Projet en cache');
    expect(app.loading).toBe(false);
    await new Promise((resolve) => setTimeout(resolve, 0));
    http
      .expectOne('/api/projets')
      .flush([{ id: 42, nom: 'Projet actualisé', avancementGlobal: 50 }]);
    await cachedLoad;

    expect(app.rows[0]['nom']).toBe('Projet actualisé');
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

  it('filters timesheets by status', async () => {
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

    app.setTimesheetStatusFilter('SOUMISE');
    await new Promise((resolve) => setTimeout(resolve, 0));
    http
      .expectOne('/api/timesheets?statut=SOUMISE')
      .flush([{ id: 10, statut: 'SOUMISE', semaineDebut: '2026-03-02' }]);
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne('/api/paiements').flush([]);
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(app.rows).toHaveLength(1);
    expect(app.timesheetStatusFilter).toBe('SOUMISE');
    http.verify();
  });

  it('does not submit a timesheet when confirmation is cancelled', async () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    app.user = {
      id: 8,
      email: 'freelance@cdg.ma',
      prenom: 'Nora',
      nom: 'Rivière',
      role: 'FREELANCE',
    };
    app.currentView = 'timesheets';
    const http = TestBed.inject(HttpTestingController);
    const row = { id: 9, statut: 'BROUILLON' };

    const submission = app.submitSheet(row);
    await new Promise((resolve) => setTimeout(resolve, 0));

    const dialog = TestBed.inject(DialogService);
    expect(dialog.dialogComponentRefMap.size).toBe(1);
    http.expectNone('/api/timesheets/9/soumettre');

    [...dialog.dialogComponentRefMap.keys()][0].close();
    await new Promise((resolve) => setTimeout(resolve, 20));
    await submission;

    expect(row['statut']).toBe('BROUILLON');
    http.verify();
  });

  it('displays the daily threshold warning inside the timesheet dialog', async () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    app.user = { id: 8, email: 'freelance@cdg.ma', prenom: 'Nora', nom: 'CDG', role: 'FREELANCE' };
    app.currentView = 'timesheets';
    app.selectedSheet = { id: 9, statut: 'BROUILLON', semaineDebut: '2026-03-02' };
    app.taskOptions = [{ id: 7, titre: 'Tâche', projetNom: 'Projet' }];
    await TestBed.inject(Router).navigateByUrl('/dashboard');
    fixture.detectChanges();

    const http = TestBed.inject(HttpTestingController);
    const adding = app.addHours();
    await new Promise((resolve) => setTimeout(resolve, 0));
    const dialog = TestBed.inject(DialogService);
    [...dialog.dialogComponentRefMap.keys()][0].close({
      tacheId: 7,
      dateSaisie: '2026-03-02',
      heures: 9,
    });
    await new Promise((resolve) => setTimeout(resolve, 0));
    http
      .expectOne('/api/timesheets/9/lignes')
      .flush({ avertissement: '9 h dépasse votre seuil de 8 h' });
    await new Promise((resolve) => setTimeout(resolve, 0));
    http
      .expectOne('/api/timesheets/9')
      .flush({ id: 9, statut: 'BROUILLON', semaineDebut: '2026-03-02', lignes: [] });
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne('/api/mes-missions?disponibles=true').flush([]);
    await adding;
    fixture.detectChanges();
    expect(document.querySelector('.p-dialog [role="alert"]')?.textContent).toContain(
      '9 h dépasse',
    );
    app.detailDialogVisible = false;
    http.verify();
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
    http
      .expectOne('/api/timesheets')
      .flush([{ id: 10, statut: 'VALIDEE', semaineDebut: '2026-03-02' }]);
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectOne('/api/paiements').flush([{ id: 20, timesheetId: 10, statut: 'EN_ATTENTE' }]);
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
    http.expectOne('/api/projets').flush([{ id: 1, nom: 'Projet de démonstration' }]);
    await loadViewSpy.mock.results[0].value;
    await fixture.whenStable();

    const rendered = fixture.nativeElement as HTMLElement;
    expect(rendered.querySelector('.app-shell')).not.toBeNull();
    expect(rendered.textContent).toContain('Projet de démonstration');
    expect(router.url).toBe('/dashboard');
    http.expectOne('/api/timesheets?statut=VALIDEE').flush([]);
    http.verify();
  });

  it('does not request the admin activity chart for project managers', async () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    await TestBed.inject(Router).navigateByUrl('/dashboard');
    app.user = { id: 2, email: 'chef@cdg.ma', prenom: 'Chef', nom: 'CDG', role: 'CHEF_PROJET' };
    app.dashboard = { projetsActifsCount: 0 };
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('app-admin-dashboard')).toBeNull();
    TestBed.inject(HttpTestingController).expectNone('/api/timesheets?statut=VALIDEE');
  });
});
