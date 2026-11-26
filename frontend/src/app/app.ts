import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DialogService } from 'primeng/dynamicdialog';
import { PopoverModule } from 'primeng/popover';
import { ProgressBarModule } from 'primeng/progressbar';
import { TableModule } from 'primeng/table';
import { NavigationEnd, Router } from '@angular/router';
import { filter, firstValueFrom } from 'rxjs';
import {
  ActionDialogComponent,
  ActionDialogData,
  ActionDialogField,
  ActionDialogResult,
} from './action-dialog/action-dialog.component';
import { ApiRow, ApiService, AuthUser } from './api.service';
import { AuthService } from './auth.service';
import { LandingPageComponent } from './landing/landing-page.component';
import { AdminDashboardComponent } from './admin-dashboard/admin-dashboard.component';

type ViewId =
  | 'dashboard'
  | 'projects'
  | 'freelancers'
  | 'my-missions'
  | 'timesheets'
  | 'billing'
  | 'my-pvs'
  | 'analysis'
  | 'users'
  | 'settings';

interface MenuItem {
  id: ViewId;
  title: string;
  roles?: AuthUser['role'][];
}

interface Column {
  key: string;
  label: string;
}

interface ViewCache {
  rows: ApiRow[];
  secondaryRows: ApiRow[];
  dashboard: ApiRow | null;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    DialogModule,
    PopoverModule,
    ProgressBarModule,
    TableModule,
    LandingPageComponent,
    AdminDashboardComponent,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(DialogService);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  detailDialogVisible = false;
  hoursWarning = '';
  private pendingConfirmation = false;
  private actionInProgress = false;

  readonly menu: MenuItem[] = [
    { id: 'dashboard', title: "Vue d'ensemble", roles: ['ADMIN', 'CHEF_PROJET'] },
    { id: 'projects', title: 'Projets', roles: ['ADMIN', 'CHEF_PROJET'] },
    { id: 'freelancers', title: 'Freelances', roles: ['ADMIN', 'CHEF_PROJET'] },
    { id: 'my-missions', title: 'Mes missions', roles: ['FREELANCE'] },
    { id: 'timesheets', title: 'Feuilles de temps' },
    { id: 'billing', title: 'Facturation', roles: ['ADMIN', 'CHEF_PROJET'] },
    { id: 'my-pvs', title: 'Mes PV', roles: ['FREELANCE'] },
    { id: 'analysis', title: 'Analyse & alertes', roles: ['ADMIN', 'CHEF_PROJET'] },
    { id: 'users', title: 'Utilisateurs', roles: ['ADMIN'] },
    { id: 'settings', title: 'Paramètres', roles: ['ADMIN'] },
  ];

  readonly columnsByView: Record<ViewId, Column[]> = {
    dashboard: [],
    projects: [
      { key: 'nom', label: 'Projet' },
      { key: 'chefProjetNomComplet', label: 'Chef de projet' },
      { key: 'statut', label: 'Statut' },
      { key: 'dateDebut', label: 'Début' },
      { key: 'dateFinPrevue', label: 'Fin prévue' },
      { key: 'budget', label: 'Budget' },
      { key: 'heuresPrevues', label: 'Heures prévues' },
      { key: 'avancementGlobal', label: 'Avancement' },
    ],
    freelancers: [
      { key: 'nomComplet', label: 'Freelance' },
      { key: 'specialite', label: 'Spécialité' },
      { key: 'tjm', label: 'TJM' },
      { key: 'seuilHeuresJour', label: 'Seuil journalier' },
      { key: 'statut', label: 'Statut' },
      { key: 'email', label: 'E-mail' },
    ],
    'my-missions': [
      { key: 'projetNom', label: 'Projet' },
      { key: 'titre', label: 'Tâche' },
      { key: 'heuresPrevues', label: 'Heures prévues' },
      { key: 'heuresSaisies', label: 'Heures saisies' },
      { key: 'avancement', label: 'Avancement' },
      { key: 'statut', label: 'Statut' },
      { key: 'dateDebut', label: 'Début' },
      { key: 'dateFin', label: 'Fin' },
    ],
    timesheets: [
      { key: 'semaineDebut', label: 'Semaine du' },
      { key: 'freelanceNomComplet', label: 'Freelance' },
      { key: 'projetNom', label: 'Projet' },
      { key: 'totalHeuresSemaine', label: 'Heures' },
      { key: 'statut', label: 'Statut' },
    ],
    billing: [
      { key: 'freelanceNomComplet', label: 'Freelance' },
      { key: 'projetNom', label: 'Projet' },
      { key: 'numeroPv', label: 'PV' },
      { key: 'montant', label: 'Montant' },
      { key: 'statut', label: 'Statut' },
      { key: 'reference', label: 'Référence' },
    ],
    'my-pvs': [
      { key: 'numero', label: 'Numéro' },
      { key: 'projetNom', label: 'Projet' },
      { key: 'semaineDebut', label: 'Semaine' },
      { key: 'montant', label: 'Montant' },
    ],
    analysis: [
      { key: 'dateCreation', label: 'Date' },
      { key: 'niveau', label: 'Niveau' },
      { key: 'projetNom', label: 'Projet' },
      { key: 'message', label: 'Alerte' },
      { key: 'traitee', label: 'État' },
    ],
    users: [
      { key: 'nomComplet', label: 'Nom' },
      { key: 'email', label: 'E-mail' },
      { key: 'role', label: 'Rôle' },
      { key: 'actif', label: 'Statut' },
    ],
    settings: [
      { key: 'cle', label: 'Paramètre' },
      { key: 'valeur', label: 'Valeur' },
    ],
  };

  get user(): AuthUser | null {
    return this.auth.currentUser();
  }
  set user(val: AuthUser | null) {
    this.auth.currentUser.set(val);
  }
  currentView: ViewId = 'dashboard';
  rows: ApiRow[] = [];
  secondaryRows: ApiRow[] = [];
  detailRows: ApiRow[] = [];
  detailTitle = '';
  dashboard: ApiRow | null = null;
  dashboardRefreshVersion = 0;
  projectTypes: ApiRow[] = [];
  loading = false;
  isDashboardRoute = false;
  menuOpen = false;
  showPassword = false;
  remembered = false;
  language: 'FR' | 'EN' | 'AR' = 'FR';
  email = '';
  password = '';
  error = '';
  notice = '';
  timesheetStatusFilter = '';
  selectedSheet: ApiRow | null = null;
  selectedProject: ApiRow | null = null;
  taskOptions: ApiRow[] = [];
  projectAssignments: ApiRow[] = [];
  private readonly viewCache = new Map<string, ViewCache>();
  private readonly activeViewLoads = new Map<string, Promise<void>>();
  private viewLoadGeneration = 0;

  private invalidateViewCaches(...views: ViewId[]): void {
    if (!views.length) {
      this.viewCache.clear();
      return;
    }
    for (const key of [...this.viewCache.keys()]) {
      if (views.some((view) => key === view || key.startsWith(`${view}:`))) {
        this.viewCache.delete(key);
      }
    }
  }

  get title(): string {
    return this.menu.find((item) => item.id === this.currentView)?.title ?? '';
  }

  get columns(): Column[] {
    const columns = this.columnsByView[this.currentView];
    return this.currentView === 'timesheets' && this.user?.role !== 'FREELANCE'
      ? [...columns, { key: 'paiementStatut', label: 'Paiement' }]
      : columns;
  }

  get sectionTitle(): string {
    const titles: Partial<Record<ViewId, string>> = {
      projects: 'Portefeuille projets',
      freelancers: 'Répertoire des freelances',
      'my-missions': 'Mes missions',
      timesheets: 'Suivi des feuilles de temps',
      billing: 'Facturation & règlements',
      'my-pvs': 'Mes procès-verbaux de réception',
      analysis: 'Analyses et alertes',
      users: 'Comptes utilisateurs',
      settings: 'Paramètres métier',
    };
    return titles[this.currentView] ?? this.title;
  }

  get sectionHint(): string {
    if (this.currentView === 'my-missions') {
      return 'Consultez les tâches qui vous sont affectées et leur avancement.';
    }
    if (this.currentView === 'timesheets') {
      return this.user?.role === 'FREELANCE'
        ? 'Saisissez vos heures et soumettez-les à validation.'
        : 'Consultez et traitez les feuilles soumises.';
    }
    if (this.currentView === 'billing') {
      return 'Les PV sont générés après validation des feuilles de temps.';
    }
    if (this.currentView === 'analysis') {
      return 'Suivi des budgets, retards et validations en attente.';
    }
    if (this.currentView === 'users') {
      return 'Gérer les comptes et les rôles de la plateforme.';
    }
    return '';
  }

  get secondaryColumns(): Column[] {
    if (this.currentView === 'billing') {
      return [
        { key: 'numero', label: 'Numéro' },
        { key: 'freelanceNomComplet', label: 'Freelance' },
        { key: 'projetNom', label: 'Projet' },
        { key: 'semaineDebut', label: 'Semaine' },
        { key: 'montant', label: 'Montant' },
      ];
    }
    return [{ key: 'libelle', label: 'Type disponible' }];
  }

  get secondaryColumnKeys(): string[] {
    return [
      ...this.secondaryColumns.map((column) => column.key),
      ...(this.currentView === 'billing' ? ['download'] : []),
    ];
  }

  get columnKeysWithActions(): string[] {
    return [...this.columnKeys, 'actions'];
  }

  get detailColumns(): Column[] {
    if (this.currentView === 'projects') {
      return [
        { key: 'titre', label: 'Tâche' },
        { key: 'freelanceNomComplet', label: 'Freelance' },
        { key: 'statut', label: 'Statut' },
        { key: 'avancement', label: 'Avancement' },
        { key: 'dateDebut', label: 'Début' },
        { key: 'dateFin', label: 'Fin' },
      ];
    }
    if (this.currentView === 'my-missions') {
      return [
        { key: 'titre', label: 'Tâche' },
        { key: 'projetNom', label: 'Projet' },
        { key: 'heuresPrevues', label: 'Heures prévues' },
        { key: 'heuresSaisies', label: 'Heures saisies' },
        { key: 'avancement', label: 'Avancement' },
        { key: 'statut', label: 'Statut' },
        { key: 'dateDebut', label: 'Début' },
        { key: 'dateFin', label: 'Fin' },
      ];
    }
    return [
      { key: 'dateSaisie', label: 'Date' },
      { key: 'projetNom', label: 'Projet' },
      { key: 'tacheTitre', label: 'Tâche' },
      { key: 'heures', label: 'Heures' },
    ];
  }

  get detailColumnKeys(): string[] {
    return [
      ...this.detailColumns.map((column) => column.key),
      ...((this.currentView === 'projects' && this.user?.role === 'CHEF_PROJET') ||
      this.canEditSelectedSheet
        ? ['actions']
        : []),
    ];
  }

  get canEditSelectedSheet(): boolean {
    return (
      this.user?.role === 'FREELANCE' &&
      ['BROUILLON', 'REJETEE'].includes(this.text(this.selectedSheet ?? {}, 'statut'))
    );
  }

  get canCreate(): boolean {
    return (
      (this.currentView === 'projects' && this.user?.role === 'CHEF_PROJET') ||
      (this.currentView === 'timesheets' && this.user?.role === 'FREELANCE') ||
      this.currentView === 'users'
    );
  }

  setLanguage(value: string): void {
    this.language = value === 'AR' ? 'AR' : value === 'EN' ? 'EN' : 'FR';
    document.documentElement.lang = this.language.toLowerCase();
    document.documentElement.dir = this.language === 'AR' ? 'rtl' : 'ltr';
  }

  setTimesheetStatusFilter(value: string): void {
    this.timesheetStatusFilter = ['BROUILLON', 'SOUMISE', 'VALIDEE', 'REJETEE'].includes(value)
      ? value
      : '';
    this.invalidateViewCaches('timesheets');
    void this.loadView();
  }

  get columnKeys(): string[] {
    return this.columns.map((column) => column.key);
  }

  get visibleMenu(): MenuItem[] {
    return this.menu.filter(
      (item) => !item.roles || item.roles.includes(this.user?.role ?? 'FREELANCE'),
    );
  }

  get canManage(): boolean {
    return this.user?.role === 'ADMIN' || this.user?.role === 'CHEF_PROJET';
  }

  ngOnInit(): void {
    this.isDashboardRoute = this.router.url.split(/[?#]/)[0] === '/dashboard';
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((event) => {
        this.isDashboardRoute = event.urlAfterRedirects.split(/[?#]/)[0] === '/dashboard';
        this.changeDetectorRef.markForCheck();
      });

    this.email = localStorage.getItem('cdg-remembered-email') ?? 'admin@cdg.ma';
    this.remembered = Boolean(localStorage.getItem('cdg-remembered-email'));
    if (this.api.token) {
      void this.auth
        .checkInitialSession()
        .then(async (user) => {
          if (user) {
            await this.loadView();
          }
        })
        .finally(() => this.changeDetectorRef.markForCheck());
    }
  }

  async login(): Promise<void> {
    this.error = '';
    this.loading = true;
    try {
      const user = await this.auth.login(this.email, this.password);
      this.user = user;
      if (this.remembered) {
        localStorage.setItem('cdg-remembered-email', this.email.trim());
      } else {
        localStorage.removeItem('cdg-remembered-email');
      }
      await this.loadView();
    } catch (error: unknown) {
      this.setError(error);
      if (!this.api.token) this.logout();
    } finally {
      this.loading = false;
      this.changeDetectorRef.markForCheck();
    }
  }

  onLandingAuthenticated(): void {
    void this.loadView();
  }

  logout(): void {
    this.auth.logout();
    this.password = '';
    this.rows = [];
    this.dashboard = null;
    this.viewCache.clear();
  }

  async selectView(view: ViewId): Promise<void> {
    this.currentView = view;
    this.menuOpen = false;
    this.detailRows = [];
    this.changeDetectorRef.markForCheck();
    await this.loadView();
  }

  async loadView(): Promise<void> {
    this.ensureAccessibleView();
    const generation = ++this.viewLoadGeneration;
    const requestedView = this.currentView;
    const requestedTimesheetStatus = this.timesheetStatusFilter;
    const cacheKey =
      requestedView === 'timesheets' ? `timesheets:${requestedTimesheetStatus}` : requestedView;
    const existingLoad = this.activeViewLoads.get(cacheKey);
    if (existingLoad) {
      await existingLoad;
      return;
    }
    const cached = this.viewCache.get(cacheKey);
    if (cached) {
      this.rows = cached.rows;
      this.secondaryRows = cached.secondaryRows;
      this.dashboard = cached.dashboard;
      this.loading = false;
      this.changeDetectorRef.markForCheck();
    } else {
      this.loading = true;
      this.changeDetectorRef.markForCheck();
    }
    this.error = '';
    const load = (async () => {
      try {
        let dashboard: ApiRow | null = null;
        let rows: ApiRow[] = [];
        let secondaryRows: ApiRow[] = [];

        switch (requestedView) {
          case 'dashboard':
            [dashboard, rows] = await Promise.all([
              this.api.get<ApiRow>('/api/dashboard/kpi'),
              this.api.get<ApiRow[]>('/api/projets'),
            ]);
            break;
          case 'projects':
            rows = await this.api.get<ApiRow[]>('/api/projets');
            break;
          case 'freelancers':
            rows = await this.api.get<ApiRow[]>('/api/freelances');
            break;
          case 'my-missions':
            rows = await this.api.get<ApiRow[]>('/api/mes-missions');
            break;
          case 'timesheets': {
            const timesheetPath = requestedTimesheetStatus
              ? `/api/timesheets?statut=${encodeURIComponent(requestedTimesheetStatus)}`
              : '/api/timesheets';
            rows = await this.api.get<ApiRow[]>(timesheetPath);
            if (this.user?.role !== 'FREELANCE') {
              const payments = await this.api.get<ApiRow[]>('/api/paiements');
              const paymentsByTimesheet = new Map(
                payments.map((payment) => [this.number(payment, 'timesheetId'), payment]),
              );
              rows = rows.map((sheet) => {
                const payment = paymentsByTimesheet.get(this.number(sheet, 'id'));
                return payment
                  ? {
                      ...sheet,
                      paiementId: this.number(payment, 'id'),
                      paiementStatut: this.text(payment, 'statut'),
                    }
                  : sheet;
              });
            }
            break;
          }
          case 'billing':
            [secondaryRows, rows] = await Promise.all([
              this.api.get<ApiRow[]>('/api/pv'),
              this.api.get<ApiRow[]>('/api/paiements'),
            ]);
            break;
          case 'my-pvs':
            rows = await this.api.get<ApiRow[]>('/api/pv');
            break;
          case 'analysis':
            rows = await this.api.get<ApiRow[]>('/api/alertes');
            break;
          case 'users':
            rows = await this.api.get<ApiRow[]>('/api/admin/utilisateurs');
            break;
          case 'settings':
            [rows, secondaryRows] = await Promise.all([
              this.api.get<ApiRow[]>('/api/admin/parametres'),
              this.api.get<ApiRow[]>('/api/admin/types-projet'),
            ]);
            break;
        }

        if (generation !== this.viewLoadGeneration || this.currentView !== requestedView) return;
        this.rows = rows;
        this.secondaryRows = secondaryRows;
        if (dashboard !== null) {
          this.dashboard = dashboard;
          this.dashboardRefreshVersion++;
        }
        this.viewCache.set(cacheKey, {
          rows: [...rows],
          secondaryRows: [...secondaryRows],
          dashboard,
        });
      } catch (error: unknown) {
        if (generation === this.viewLoadGeneration && this.currentView === requestedView) {
          this.setError(error);
          if (!this.api.token) this.logout();
        }
      } finally {
        if (generation === this.viewLoadGeneration && this.currentView === requestedView) {
          this.loading = false;
          this.changeDetectorRef.markForCheck();
        }
      }
    })();

    this.activeViewLoads.set(cacheKey, load);
    try {
      await load;
    } finally {
      if (this.activeViewLoads.get(cacheKey) === load) {
        this.activeViewLoads.delete(cacheKey);
      }
    }
  }

  private ensureAccessibleView(): void {
    if (this.visibleMenu.some((item) => item.id === this.currentView)) return;
    this.currentView = this.user?.role === 'FREELANCE' ? 'my-missions' : 'dashboard';
  }

  async startCreate(): Promise<void> {
    const today = this.dateInputValue(new Date());
    let data: ActionDialogData;
    if (this.currentView === 'projects') {
      try {
        this.projectTypes = await this.api.get<ApiRow[]>('/api/projets/types');
      } catch (error: unknown) {
        this.setError(error);
        return;
      }
      if (!this.projectTypes.length) {
        this.setError('Aucun type de projet n’est disponible. Ajoutez-en un dans les paramètres.');
        return;
      }
      const fields: ActionDialogField[] = [
        { key: 'code', label: 'Code du projet', type: 'text', required: true },
        { key: 'nom', label: 'Nom du projet', type: 'text', required: true },
        {
          key: 'budget',
          label: 'Budget (MAD)',
          type: 'number',
          required: true,
          min: 1,
          step: 0.01,
        },
        {
          key: 'heuresPrevues',
          label: 'Heures prévues du projet',
          type: 'number',
          required: true,
          min: 0.01,
          step: 0.01,
        },
        {
          key: 'nombreTachesPrevues',
          label: 'Nombre de tâches prévues',
          type: 'number',
          required: true,
          min: 1,
          step: 1,
        },
        { key: 'dateDebut', label: 'Date de début', type: 'date', required: true },
        { key: 'dateFinPrevue', label: 'Date de fin prévue', type: 'date', required: true },
        {
          key: 'typeProjetId',
          label: 'Type de projet',
          type: 'select',
          required: true,
          options: this.projectTypes.map((type) => ({
            value: this.number(type, 'id'),
            label: this.text(type, 'libelle'),
          })),
        },
      ];
      fields.push({
        key: 'description',
        label: 'Description',
        type: 'textarea',
      });
      data = {
        title: 'Créer un projet',
        description: 'Renseignez les informations principales du projet.',
        submitLabel: 'Créer le projet',
        fields,
        values: {
          dateDebut: today,
          dateFinPrevue: today,
        },
      };
    } else if (this.currentView === 'timesheets') {
      data = {
        title: 'Créer une feuille de temps',
        description: 'Choisissez le lundi de la semaine à déclarer.',
        submitLabel: 'Créer la feuille',
        fields: [
          { key: 'semaineDebut', label: 'Lundi de la semaine', type: 'date', required: true },
        ],
        values: { semaineDebut: this.currentWeekMonday() },
      };
    } else if (this.currentView === 'users') {
      data = {
        title: 'Créer un compte utilisateur',
        description: 'Définissez l’identité et le rôle du nouvel utilisateur.',
        submitLabel: 'Créer le compte',
        fields: [
          { key: 'prenom', label: 'Prénom', type: 'text', required: true },
          { key: 'nom', label: 'Nom', type: 'text', required: true },
          { key: 'email', label: 'Adresse e-mail', type: 'email', required: true },
          { key: 'motDePasse', label: 'Mot de passe', type: 'password', required: true },
          {
            key: 'role',
            label: 'Rôle',
            type: 'select',
            required: true,
            options: [
              { value: 'ADMIN', label: 'Administrateur' },
              { value: 'CHEF_PROJET', label: 'Chef de projet' },
              { value: 'FREELANCE', label: 'Freelance' },
            ],
          },
          {
            key: 'tjm',
            label: 'TJM (MAD)',
            type: 'number',
            min: 0.01,
            step: 0.01,
            hint: 'Facultatif ; applicable aux freelances',
            visibleWhen: { key: 'role', value: 'FREELANCE' },
          },
          {
            key: 'specialite',
            label: 'Spécialité',
            type: 'text',
            hint: 'Facultatif ; applicable aux freelances',
            visibleWhen: { key: 'role', value: 'FREELANCE' },
          },
        ],
        values: { role: 'FREELANCE' },
      };
    } else {
      return;
    }
    const values = await this.showActionDialog(data);
    if (values) await this.create(values);
  }

  async create(values: ActionDialogResult): Promise<void> {
    this.error = '';
    try {
      if (this.currentView === 'projects') {
        await this.api.post('/api/projets', {
          code: values['code'],
          nom: values['nom'],
          budget: Number(values['budget']),
          heuresPrevues: Number(values['heuresPrevues']),
          nombreTachesPrevues: Number(values['nombreTachesPrevues']),
          dateDebut: values['dateDebut'],
          dateFinPrevue: values['dateFinPrevue'],
          typeProjetId: Number(values['typeProjetId']),
          chefProjetId: this.user?.id,
          description: values['description'] ?? '',
        });
      } else if (this.currentView === 'timesheets') {
        await this.api.post('/api/timesheets', {
          semaineDebut: values['semaineDebut'],
        });
      } else if (this.currentView === 'users') {
        await this.api.post('/api/admin/utilisateurs', {
          nom: values['nom'],
          prenom: values['prenom'],
          email: values['email'],
          motDePasse: values['motDePasse'],
          role: values['role'],
          tjm: values['tjm'] ? Number(values['tjm']) : null,
          specialite: values['specialite'] || null,
        });
      }
      this.notify('Enregistrement effectué.');
      this.invalidateViewCaches();
      await this.loadView();
    } catch (error: unknown) {
      this.setError(error);
    }
  }

  async openDetails(row: ApiRow): Promise<void> {
    const id = this.number(row, 'id');
    await this.closeDetailsDialog();
    this.error = '';
    this.hoursWarning = '';
    try {
      if (this.currentView === 'projects') {
        this.selectedProject = row;
        this.detailTitle = `${this.text(row, 'nom')} — tâches`;
        this.detailRows = await this.api.get<ApiRow[]>(`/api/projets/${id}/taches`);
        this.projectAssignments = this.canManage
          ? await this.api.get<ApiRow[]>(`/api/projets/${id}/affectations`)
          : [];
      } else if (this.currentView === 'my-missions') {
        this.detailTitle = `Mission — ${this.text(row, 'titre')}`;
        this.detailRows = [row];
      } else if (this.currentView === 'timesheets') {
        const sheet = await this.api.get<ApiRow>(`/api/timesheets/${id}`);
        this.selectedSheet = sheet;
        this.detailTitle = `Feuille de temps — ${this.formatDate(this.text(sheet, 'semaineDebut'))}`;
        this.detailRows = Array.isArray(sheet['lignes']) ? (sheet['lignes'] as ApiRow[]) : [];
        if (this.canEditSelectedSheet) {
          this.taskOptions = await this.api.get<ApiRow[]>('/api/mes-missions?disponibles=true');
        } else {
          this.taskOptions = [];
        }
      } else {
        return;
      }
      this.showDetailsDialog();
    } catch (error: unknown) {
      this.setError(error);
    } finally {
      this.actionInProgress = false;
      this.changeDetectorRef.markForCheck();
    }
  }

  private showDetailsDialog(): void {
    this.detailDialogVisible = true;
  }

  private async closeDetailsDialog(): Promise<void> {
    this.detailDialogVisible = false;
  }

  actionLabel(row: ApiRow): string {
    if (this.currentView === 'projects') return 'Détails / tâches';
    if (this.currentView === 'freelancers') return 'Modifier';
    if (this.currentView === 'my-missions') return 'Consulter la mission';
    if (this.currentView === 'timesheets') {
      if (
        this.user?.role === 'FREELANCE' &&
        ['BROUILLON', 'REJETEE'].includes(this.text(row, 'statut'))
      ) {
        return 'Détails / saisir';
      }
      if (this.text(row, 'statut') === 'SOUMISE' && this.user?.role === 'CHEF_PROJET') {
        return 'Valider';
      }
      return 'Détails';
    }
    if (this.currentView === 'billing')
      return this.text(row, 'statut') === 'PAYE' ? 'Payé' : 'Marquer payé';
    if (this.currentView === 'my-pvs') return 'Télécharger PDF';
    if (this.currentView === 'analysis')
      return this.bool(row, 'traitee') ? 'Traitée' : 'Marquer traitée';
    if (this.currentView === 'users') return this.bool(row, 'actif') ? 'Désactiver' : 'Activer';
    if (this.currentView === 'settings') return 'Modifier la valeur';
    return '';
  }

  canConfirmPayment(row: ApiRow): boolean {
    return (
      this.currentView === 'timesheets' &&
      this.user?.role === 'CHEF_PROJET' &&
      Boolean(this.number(row, 'paiementId')) &&
      this.text(row, 'paiementStatut') !== 'PAYE'
    );
  }

  async confirmTimesheetPayment(row: ApiRow): Promise<void> {
    if (!this.canConfirmPayment(row)) return;
    const values = await this.showActionDialog({
      title: 'Confirmer le paiement',
      description: `Confirmez le paiement de la feuille de ${this.text(row, 'freelanceNomComplet')} pour la semaine du ${this.formatDate(this.text(row, 'semaineDebut'))}.`,
      submitLabel: 'Confirmer le paiement',
      fields: [
        { key: 'reference', label: 'Référence du paiement', type: 'text', hint: 'Facultatif' },
      ],
    });
    if (!values) return;
    try {
      await this.api.patch(`/api/paiements/${this.number(row, 'paiementId')}`, {
        statut: 'PAYE',
        datePaiement: this.dateInputValue(new Date()),
        reference: String(values['reference'] ?? '').trim() || null,
      });
      this.notify('Paiement confirmé.');
      row['paiementStatut'] = 'PAYE';
      this.invalidateViewCaches('billing', 'timesheets');
      await this.loadView();
    } catch (error: unknown) {
      this.setError(error);
    }
  }

  async rowAction(row: ApiRow): Promise<void> {
    const id = this.number(row, 'id');
    try {
      if (this.currentView === 'projects') {
        await this.openDetails(row);
      } else if (this.currentView === 'my-missions') {
        await this.openDetails(row);
      } else if (this.currentView === 'freelancers') {
        const values = await this.showActionDialog({
          title: 'Modifier le profil freelance',
          description:
            this.user?.role === 'ADMIN'
              ? `Mettez à jour le TJM et la spécialité de ${this.text(row, 'nomComplet')}.`
              : `Mettez à jour la spécialité de ${this.text(row, 'nomComplet')}.`,
          submitLabel: 'Enregistrer',
          fields: [
            ...(this.user?.role === 'ADMIN'
              ? [
                  {
                    key: 'tjm',
                    label: 'TJM (MAD)',
                    type: 'number' as const,
                    required: true,
                    min: 0.01,
                    step: 0.01,
                  },
                  {
                    key: 'seuilHeuresJour',
                    label: "Seuil d'alerte (h/j)",
                    type: 'number' as const,
                    required: true,
                    min: 0.01,
                    max: 10,
                    step: 0.01,
                    hint: 'Au-delà de 10 h/j, la saisie est bloquée.',
                  },
                ]
              : []),
            { key: 'specialite', label: 'Spécialité', type: 'text', required: true },
          ],
          values: {
            tjm: this.number(row, 'tjm'),
            seuilHeuresJour: this.number(row, 'seuilHeuresJour'),
            specialite: this.text(row, 'specialite'),
          },
        });
        if (!values) return;
        await this.api.put(`/api/freelances/${id}`, {
          tjm: this.user?.role === 'ADMIN' ? Number(values['tjm']) : this.number(row, 'tjm'),
          ...(this.user?.role === 'ADMIN'
            ? { seuilHeuresJour: Number(values['seuilHeuresJour']) }
            : {}),
          specialite: String(values['specialite'] ?? ''),
          statut: this.text(row, 'statut'),
        });
        this.invalidateViewCaches('freelancers');
        await this.loadView();
      } else if (this.currentView === 'timesheets') {
        const status = this.text(row, 'statut');
        if (status === 'SOUMISE' && this.user?.role === 'CHEF_PROJET') {
          if (this.actionInProgress) return;
          const confirmed = await this.confirmAction(
            'Valider la feuille de temps ?',
            'Cette action validera la feuille et générera son procès-verbal de réception.',
            'Confirmer la validation',
          );
          if (!confirmed) return;
          this.actionInProgress = true;
          await this.api.post<ApiRow>(`/api/timesheets/${id}/valider`);
          row['statut'] = 'VALIDEE';
          this.notify('Feuille validée.');
        } else if (this.user?.role === 'FREELANCE' && ['BROUILLON', 'REJETEE'].includes(status)) {
          await this.openDetails(row);
        } else {
          await this.openDetails(row);
        }
        this.invalidateViewCaches();
        await this.loadView();
      } else if (this.currentView === 'billing') {
        if (this.text(row, 'statut') === 'PAYE') return;
        const values = await this.showActionDialog({
          title: 'Enregistrer le paiement',
          description: `Confirmez le paiement de ${this.text(row, 'freelanceNomComplet')} pour ce PV.`,
          submitLabel: 'Confirmer le paiement',
          fields: [
            { key: 'reference', label: 'Référence du paiement', type: 'text', hint: 'Facultatif' },
          ],
        });
        if (!values) return;
        await this.api.patch(`/api/paiements/${id}`, {
          statut: 'PAYE',
          datePaiement: this.dateInputValue(new Date()),
          reference: String(values['reference'] ?? '').trim() || null,
        });
        row['statut'] = 'PAYE';
        this.invalidateViewCaches('billing', 'timesheets');
        await this.loadView();
      } else if (this.currentView === 'my-pvs') {
        await this.api.download(`/api/pv/${id}/pdf`, `PV-CDG-${id}.pdf`);
      } else if (this.currentView === 'analysis') {
        await this.api.patch(`/api/alertes/${id}/traitee`);
        row['traitee'] = true;
        this.invalidateViewCaches('analysis', 'dashboard');
        await this.loadView();
      } else if (this.currentView === 'users') {
        await this.api.patch(`/api/admin/utilisateurs/${id}/statut`);
        row['actif'] = !this.bool(row, 'actif');
        this.invalidateViewCaches('users');
        await this.loadView();
      } else if (this.currentView === 'settings') {
        const values = await this.showActionDialog({
          title: 'Modifier le paramètre',
          description: `Nouvelle valeur pour ${this.text(row, 'cle')}.`,
          fields: [{ key: 'valeur', label: 'Valeur', type: 'text', required: true }],
          values: { valeur: this.text(row, 'valeur') },
        });
        if (!values) return;
        await this.api.put('/api/admin/parametres', {
          cle: this.text(row, 'cle'),
          valeur: String(values['valeur'] ?? ''),
        });
        row['valeur'] = String(values['valeur'] ?? '');
        this.invalidateViewCaches('settings');
        await this.loadView();
      }
      this.notify('Action effectuée.');
    } catch (error: unknown) {
      this.setError(error);
    } finally {
      this.actionInProgress = false;
      this.changeDetectorRef.markForCheck();
    }
  }

  async submitSheet(row: ApiRow): Promise<void> {
    if (this.actionInProgress) return;
    const id = this.number(row, 'id');
    try {
      const confirmed = await this.confirmAction(
        'Soumettre cette feuille ?',
        'La feuille sera transmise au chef de projet pour validation.',
        'Confirmer la soumission',
      );
      if (!confirmed) return;
      this.actionInProgress = true;
      await this.api.post(`/api/timesheets/${id}/soumettre?confirmed=true`);
      row['statut'] = 'SOUMISE';
      this.notify('Feuille soumise.');
      this.invalidateViewCaches();
      await this.loadView();
    } catch (error: unknown) {
      this.setError(error);
    } finally {
      this.actionInProgress = false;
      this.changeDetectorRef.markForCheck();
    }
  }

  async rejectSheet(row: ApiRow): Promise<void> {
    const values = await this.showActionDialog({
      title: 'Rejeter la feuille de temps',
      description: 'Indiquez le motif afin que le freelance puisse corriger sa saisie.',
      submitLabel: 'Rejeter la feuille',
      danger: true,
      fields: [{ key: 'motif', label: 'Motif du rejet', type: 'textarea', required: true }],
    });
    const motif = String(values?.['motif'] ?? '').trim();
    if (!motif) return;
    try {
      await this.api.post(`/api/timesheets/${this.number(row, 'id')}/rejeter`, {
        motif,
      });
      row['statut'] = 'REJETEE';
      this.notify('Feuille rejetée.');
      this.invalidateViewCaches();
      await this.loadView();
    } catch (error: unknown) {
      this.setError(error);
    }
  }

  async addHours(): Promise<void> {
    if (!this.selectedSheet) return;
    if (!this.taskOptions.length) {
      this.setError(
        "Aucune tâche disponible : les heures prévues sont atteintes ou aucune mission n'est affectée.",
      );
      return;
    }
    const values = await this.showActionDialog({
      title: 'Ajouter des heures',
      description:
        'Saisissez le temps consacré à une tâche. Le total journalier ne peut dépasser 10 h.',
      submitLabel: 'Ajouter la saisie',
      fields: [
        {
          key: 'tacheId',
          label: 'Tâche',
          type: 'select',
          required: true,
          options: this.taskOptions.map((task) => ({
            value: this.number(task, 'id'),
            label: `${this.text(task, 'titre')} — ${this.text(task, 'projetNom')} (${Math.max(0, this.number(task, 'heuresPrevues') - this.number(task, 'heuresSaisies')).toFixed(2)} h restantes)`,
          })),
        },
        { key: 'dateSaisie', label: 'Date', type: 'date', required: true },
        {
          key: 'heures',
          label: 'Heures',
          type: 'number',
          required: true,
          min: 0.01,
          max: 10,
          step: 0.25,
        },
      ],
      values: { dateSaisie: this.text(this.selectedSheet, 'semaineDebut') },
    });
    if (!values) return;
    this.error = '';
    this.hoursWarning = '';
    try {
      const saved = await this.api.post<ApiRow>(
        `/api/timesheets/${this.number(this.selectedSheet, 'id')}/lignes`,
        {
          tacheId: Number(values['tacheId']),
          dateSaisie: values['dateSaisie'],
          heures: Number(values['heures']),
        },
      );
      await this.openDetails(this.selectedSheet);
      this.invalidateViewCaches('timesheets', 'projects', 'dashboard', 'my-missions');
      this.hoursWarning = this.text(saved, 'avertissement');
      if (!this.hoursWarning) this.notify('Heures ajoutées.');
    } catch (error: unknown) {
      this.setError(error);
    }
  }

  async detailAction(row: ApiRow): Promise<void> {
    try {
      if (this.currentView === 'projects') {
        const values = await this.showActionDialog({
          title: 'Mettre à jour la tâche',
          description: `Avancement de « ${this.text(row, 'titre')} ».`,
          submitLabel: 'Enregistrer',
          fields: [
            {
              key: 'avancement',
              label: 'Avancement (%)',
              type: 'number',
              required: true,
              min: 0,
              max: 100,
              step: 1,
            },
          ],
          values: { avancement: this.number(row, 'avancement') },
        });
        if (!values) return;
        const progress = Number(values['avancement']);
        if (!Number.isInteger(progress) || progress < 0 || progress > 100) {
          throw new Error("L'avancement doit être un nombre entier de 0 à 100.");
        }
        await this.api.patch(`/api/taches/${this.number(row, 'id')}/avancement`, {
          avancement: progress,
        });
        row['avancement'] = progress;
        row['statut'] = progress === 100 ? 'TERMINEE' : progress > 0 ? 'EN_COURS' : 'A_FAIRE';
        this.invalidateViewCaches('projects', 'dashboard', 'my-missions');
        if (this.selectedProject) await this.openDetails(this.selectedProject);
      } else if (this.selectedSheet) {
        const confirmed = await this.confirmAction(
          'Supprimer cette saisie ?',
          'Cette ligne d’heures sera supprimée définitivement.',
          'Supprimer',
          true,
        );
        if (!confirmed) return;
        await this.api.delete(
          `/api/timesheets/${this.number(this.selectedSheet, 'id')}/lignes/${this.number(row, 'id')}`,
        );
        await this.openDetails(this.selectedSheet);
        this.invalidateViewCaches('timesheets', 'projects', 'dashboard');
      }
    } catch (error: unknown) {
      this.setError(error);
    }
  }

  async assignFreelancer(): Promise<void> {
    if (!this.selectedProject) return;
    try {
      const freelancers = await this.api.get<ApiRow[]>('/api/freelances');
      if (!freelancers.length) throw new Error('Aucun freelance disponible.');
      const values = await this.showActionDialog({
        title: 'Affecter un freelance',
        description: `Choisissez un freelance pour le projet « ${this.text(this.selectedProject, 'nom')} ».`,
        submitLabel: 'Créer l’affectation',
        fields: [
          {
            key: 'freelanceId',
            label: 'Freelance',
            type: 'select',
            required: true,
            options: freelancers.map((person) => ({
              value: this.number(person, 'id'),
              label: `${this.text(person, 'prenom')} ${this.text(person, 'nom')}`,
            })),
          },
          {
            key: 'tjmApplique',
            label: 'TJM appliqué (MAD)',
            type: 'number',
            required: true,
            min: 0.01,
            step: 0.01,
          },
          { key: 'dateDebut', label: 'Date de début', type: 'date', required: true },
          { key: 'dateFin', label: 'Date de fin', type: 'date', hint: 'Facultative' },
        ],
        values: {
          freelanceId: this.number(freelancers[0], 'id'),
          tjmApplique: this.number(freelancers[0], 'tjm'),
          dateDebut: this.dateInputValue(new Date()),
        },
      });
      if (!values) return;
      await this.api.post(`/api/projets/${this.number(this.selectedProject, 'id')}/affectations`, {
        freelanceId: Number(values['freelanceId']),
        tjmApplique: Number(values['tjmApplique']),
        dateDebut: values['dateDebut'],
        dateFin: values['dateFin'] || null,
      });
      await this.openDetails(this.selectedProject);
      this.invalidateViewCaches('projects', 'dashboard', 'my-missions');
      this.notify('Freelance affecté au projet.');
    } catch (error: unknown) {
      this.setError(error);
    }
  }

  async editProject(): Promise<void> {
    if (!this.selectedProject || this.user?.role !== 'CHEF_PROJET') return;
    try {
      const project = await this.api.get<ApiRow>(
        `/api/projets/${this.number(this.selectedProject, 'id')}`,
      );
      this.projectTypes = await this.api.get<ApiRow[]>('/api/projets/types');
      const values = await this.showActionDialog({
        title: 'Modifier le projet',
        description: `Mettez à jour les informations de « ${this.text(project, 'nom')} ».`,
        submitLabel: 'Enregistrer',
        fields: [
          { key: 'nom', label: 'Nom du projet', type: 'text', required: true },
          { key: 'description', label: 'Description', type: 'textarea' },
          {
            key: 'budget',
            label: 'Budget (MAD)',
            type: 'number',
            required: true,
            min: 1,
            step: 0.01,
          },
          {
            key: 'heuresPrevues',
            label: 'Heures prévues du projet',
            type: 'number',
            required: true,
            min: 0.01,
            step: 0.01,
          },
          {
            key: 'nombreTachesPrevues',
            label: 'Nombre de tâches prévues',
            type: 'number',
            required: true,
            min: 1,
            step: 1,
          },
          { key: 'dateDebut', label: 'Date de début', type: 'date', required: true },
          { key: 'dateFinPrevue', label: 'Date de fin prévue', type: 'date', required: true },
          {
            key: 'typeProjetId',
            label: 'Type de projet',
            type: 'select',
            required: true,
            options: this.projectTypes.map((type) => ({
              value: this.number(type, 'id'),
              label: this.text(type, 'libelle'),
            })),
          },
          {
            key: 'statut',
            label: 'Statut',
            type: 'select',
            required: true,
            options: [
              { value: 'PLANIFIE', label: 'Planifié' },
              { value: 'EN_COURS', label: 'En cours' },
              { value: 'SUSPENDU', label: 'Suspendu' },
              { value: 'TERMINE', label: 'Terminé' },
            ],
          },
        ],
        values: {
          nom: this.text(project, 'nom'),
          description: this.text(project, 'description'),
          budget: this.number(project, 'budget'),
          heuresPrevues:
            project['heuresPrevues'] == null ? '' : this.number(project, 'heuresPrevues'),
          nombreTachesPrevues:
            project['nombreTachesPrevues'] == null
              ? ''
              : this.number(project, 'nombreTachesPrevues'),
          dateDebut: this.text(project, 'dateDebut'),
          dateFinPrevue: this.text(project, 'dateFinPrevue'),
          typeProjetId: this.number(project, 'typeProjetId'),
          statut: this.text(project, 'statut'),
        },
      });
      if (!values) return;
      const updated = await this.api.put<ApiRow>(`/api/projets/${this.number(project, 'id')}`, {
        nom: String(values['nom'] ?? '').trim(),
        description: values['description'] ?? '',
        budget: Number(values['budget']),
        heuresPrevues: Number(values['heuresPrevues']),
        nombreTachesPrevues: Number(values['nombreTachesPrevues']),
        dateDebut: values['dateDebut'],
        dateFinPrevue: values['dateFinPrevue'],
        typeProjetId: Number(values['typeProjetId']),
        statut: values['statut'],
      });
      this.selectedProject = updated;
      this.detailTitle = `${this.text(updated, 'nom')} — tâches`;
      this.notify('Projet modifié.');
      this.invalidateViewCaches('projects', 'dashboard');
      await this.loadView();
    } catch (error: unknown) {
      this.setError(error);
    }
  }

  async editTask(row: ApiRow): Promise<void> {
    if (!this.selectedProject || this.user?.role !== 'CHEF_PROJET') return;
    const values = await this.showActionDialog({
      title: 'Modifier la tâche',
      description: `Mettez à jour « ${this.text(row, 'titre')} ».`,
      submitLabel: 'Enregistrer',
      fields: [
        { key: 'titre', label: 'Intitulé de la tâche', type: 'text', required: true },
        {
          key: 'heuresPrevues',
          label: 'Heures prévues',
          type: 'number',
          required: true,
          min: 0.01,
          step: 0.01,
        },
        {
          key: 'avancement',
          label: 'Avancement (%)',
          type: 'number',
          required: true,
          min: 0,
          max: 100,
          step: 1,
        },
        {
          key: 'statut',
          label: 'Statut',
          type: 'select',
          required: true,
          options: [
            { value: 'A_FAIRE', label: 'À faire' },
            { value: 'EN_COURS', label: 'En cours' },
            { value: 'TERMINEE', label: 'Terminée' },
          ],
        },
        { key: 'dateDebut', label: 'Date de début', type: 'date' },
        { key: 'dateFin', label: 'Date de fin', type: 'date' },
      ],
      values: {
        titre: this.text(row, 'titre'),
        heuresPrevues: this.number(row, 'heuresPrevues'),
        avancement: this.number(row, 'avancement'),
        statut: this.text(row, 'statut'),
        dateDebut: this.text(row, 'dateDebut'),
        dateFin: this.text(row, 'dateFin'),
      },
    });
    if (!values) return;
    try {
      const progress = Number(values['avancement']);
      const status = String(values['statut'] ?? '');
      const matchesStatus =
        (status === 'A_FAIRE' && progress === 0) ||
        (status === 'EN_COURS' && progress > 0 && progress < 100) ||
        (status === 'TERMINEE' && progress === 100);
      if (!Number.isInteger(progress) || !matchesStatus) {
        throw new Error('Le statut de la tâche doit correspondre à son pourcentage d’avancement.');
      }
      await this.api.put(`/api/taches/${this.number(row, 'id')}`, {
        titre: String(values['titre'] ?? '').trim(),
        heuresPrevues: Number(values['heuresPrevues']),
        avancement: progress,
        statut: status,
        affectationId: this.number(row, 'affectationId') || null,
        dateDebut: values['dateDebut'] || null,
        dateFin: values['dateFin'] || null,
      });
      await this.openDetails(this.selectedProject);
      this.notify('Tâche modifiée.');
      this.invalidateViewCaches('projects', 'dashboard', 'my-missions');
    } catch (error: unknown) {
      this.setError(error);
    }
  }

  async createTask(): Promise<void> {
    if (!this.selectedProject) return;
    const fields = [
      { key: 'titre', label: 'Intitulé de la tâche', type: 'text' as const, required: true },
      {
        key: 'heuresPrevues',
        label: 'Heures prévues',
        type: 'number' as const,
        required: true,
        min: 0.01,
        step: 0.01,
        hint: 'Par défaut : heures du projet divisées par le nombre de tâches prévues.',
      },
      {
        key: 'affectationId',
        label: 'Freelance affecté',
        type: 'select' as const,
        options: [
          { value: '', label: 'Aucune affectation' },
          ...this.projectAssignments.map((assignment) => ({
            value: this.number(assignment, 'id'),
            label: this.text(assignment, 'freelanceNomComplet'),
          })),
        ],
      },
      { key: 'dateDebut', label: 'Date de début', type: 'date' as const },
      { key: 'dateFin', label: 'Date de fin', type: 'date' as const },
    ];
    const values = await this.showActionDialog({
      title: 'Créer une tâche',
      description: `Définissez l’intitulé, la charge et la période de la tâche pour « ${this.text(this.selectedProject, 'nom')} ».`,
      submitLabel: 'Créer la tâche',
      fields,
      values: {
        heuresPrevues:
          this.number(this.selectedProject, 'heuresPrevues') > 0 &&
          this.number(this.selectedProject, 'nombreTachesPrevues') > 0
            ? Number(
                (
                  this.number(this.selectedProject, 'heuresPrevues') /
                  this.number(this.selectedProject, 'nombreTachesPrevues')
                ).toFixed(2),
              )
            : '',
      },
    });
    if (!values) return;
    const titre = String(values['titre'] ?? '').trim();
    if (!titre) return;
    try {
      await this.api.post(`/api/projets/${this.number(this.selectedProject, 'id')}/taches`, {
        titre,
        heuresPrevues: Number(values['heuresPrevues']),
        affectationId: values['affectationId'] ? Number(values['affectationId']) : null,
        dateDebut: values['dateDebut'] || null,
        dateFin: values['dateFin'] || null,
      });
      await this.openDetails(this.selectedProject);
      this.notify('Tâche créée.');
      this.invalidateViewCaches('projects', 'dashboard', 'my-missions');
    } catch (error: unknown) {
      this.setError(error);
    }
  }

  async editUser(row: ApiRow): Promise<void> {
    const values = await this.showActionDialog({
      title: 'Modifier un compte',
      description: `Mettez à jour le profil de ${this.text(row, 'nomComplet')}.`,
      submitLabel: 'Enregistrer',
      fields: [
        { key: 'prenom', label: 'Prénom', type: 'text', required: true },
        { key: 'nom', label: 'Nom', type: 'text', required: true },
        { key: 'email', label: 'Adresse e-mail', type: 'email', required: true },
        {
          key: 'role',
          label: 'Rôle',
          type: 'select',
          required: true,
          options: [
            { value: 'ADMIN', label: 'Administrateur' },
            { value: 'CHEF_PROJET', label: 'Chef de projet' },
            { value: 'FREELANCE', label: 'Freelance' },
          ],
        },
        {
          key: 'motDePasse',
          label: 'Nouveau mot de passe',
          type: 'password',
          hint: 'Laissez vide pour le conserver',
        },
      ],
      values: {
        prenom: this.text(row, 'prenom'),
        nom: this.text(row, 'nom'),
        email: this.text(row, 'email'),
        role: this.text(row, 'role'),
      },
    });
    if (!values) return;
    try {
      await this.api.put(`/api/admin/utilisateurs/${this.number(row, 'id')}`, {
        prenom: String(values['prenom'] ?? ''),
        nom: String(values['nom'] ?? ''),
        email: String(values['email'] ?? ''),
        role: values['role'],
        motDePasse: values['motDePasse'] || null,
      });
      this.notify('Compte modifié.');
      this.invalidateViewCaches('users');
      await this.loadView();
    } catch (error: unknown) {
      this.setError(error);
    }
  }

  async createType(): Promise<void> {
    const values = await this.showActionDialog({
      title: 'Ajouter un type de projet',
      fields: [{ key: 'libelle', label: 'Nom du type de projet', type: 'text', required: true }],
      submitLabel: 'Ajouter',
    });
    const libelle = String(values?.['libelle'] ?? '').trim();
    if (!libelle) return;
    try {
      await this.api.post('/api/admin/types-projet', { libelle });
      this.invalidateViewCaches('settings', 'projects');
      await this.loadView();
    } catch (error: unknown) {
      this.setError(error);
    }
  }

  async runAnalysis(): Promise<void> {
    try {
      const result = await this.api.post<ApiRow>('/api/analyses/executer');
      this.notify(
        `Analyse terminée : ${this.value(result['alertesCreeesOuActualisees'])} alerte(s).`,
      );
      await this.loadView();
    } catch (error: unknown) {
      this.setError(error);
    }
  }

  async downloadMonthlyReport(): Promise<void> {
    const values = await this.showActionDialog({
      title: 'Télécharger le rapport mensuel',
      description: 'Choisissez le mois à inclure dans le rapport.',
      submitLabel: 'Télécharger le PDF',
      fields: [{ key: 'month', label: 'Mois du rapport', type: 'month', required: true }],
      values: { month: this.dateInputValue(new Date()).slice(0, 7) },
    });
    const month = String(values?.['month'] ?? '');
    if (!month) return;
    try {
      await this.api.download(
        `/api/analyses/rapport-mensuel?mois=${encodeURIComponent(month)}`,
        `Rapport-Mensuel-CDG-${month}.pdf`,
      );
    } catch (error: unknown) {
      this.setError(error);
    }
  }

  cell(row: ApiRow, key: string): string {
    const value = row[key];
    if (key === 'nomComplet') {
      return `${this.value(row['prenom'])} ${this.value(row['nom'])}`.trim() || '—';
    }
    if (value === null || value === undefined || value === '') return '—';
    if (key.toLowerCase().includes('date') || key === 'semaineDebut') {
      return this.formatDate(String(value));
    }
    if (key.toLowerCase().includes('budget') || key === 'montant' || key === 'tjm') {
      return this.money(value);
    }
    if (key.toLowerCase().includes('avancement')) return `${String(value)} %`;
    if (key.toLowerCase().includes('heures')) return `${String(value)} h`;
    if (typeof value === 'boolean') return value ? 'Actif' : 'Inactif';
    return String(value).replaceAll('_', ' ');
  }

  text(row: ApiRow, key: string): string {
    return this.value(row[key]);
  }

  number(row: ApiRow, key: string): number {
    const value = row[key];
    return typeof value === 'number' ? value : Number(value ?? 0);
  }

  bool(row: ApiRow, key: string): boolean {
    return row[key] === true || row[key] === 'true';
  }

  alerts(): ApiRow[] {
    const alerts = this.dashboard?.['dernieresAlertes'];
    return Array.isArray(alerts) ? (alerts as ApiRow[]) : [];
  }

  statusClass(row: ApiRow, key: string): string {
    const value = this.text(row, key).toUpperCase();
    if (key === 'traitee') return this.bool(row, key) ? 'success' : 'warning';
    if (/VALIDEE|PAYE|ACTIF|EN_COURS|TRAITEE/.test(value)) return 'success';
    if (/SOUMISE|PLANIFIE|ATTENTION|BROUILLON|EN_ATTENTE/.test(value)) return 'warning';
    if (/REJETEE|CRITIQUE|ANNULE|INACTIF/.test(value)) return 'critical';
    if (/INFO/.test(value)) return 'info';
    return 'neutral';
  }

  navIcon(view: ViewId): string {
    const icons: Record<ViewId, string> = {
      dashboard: '⌂',
      projects: '▦',
      freelancers: '♙',
      'my-missions': '✓',
      timesheets: '◷',
      billing: '▤',
      'my-pvs': '▤',
      analysis: '◉',
      users: '♙',
      settings: '⚙',
    };
    return icons[view];
  }

  roleLabel(role: AuthUser['role']): string {
    const labels: Record<AuthUser['role'], string> = {
      ADMIN: 'Administrateur',
      CHEF_PROJET: 'Chef de projet',
      FREELANCE: 'Freelance',
    };
    return labels[role];
  }

  async downloadPv(row: ApiRow): Promise<void> {
    const id = this.number(row, 'id');
    try {
      await this.api.download(`/api/pv/${id}/pdf`, `PV-CDG-${id}.pdf`);
    } catch (error: unknown) {
      this.setError(error);
    }
  }

  stat(key: string): string {
    return this.value(this.dashboard?.[key]);
  }

  money(value: unknown): string {
    return new Intl.NumberFormat('fr-MA', {
      style: 'currency',
      currency: 'MAD',
      maximumFractionDigits: 0,
    }).format(Number(value ?? 0));
  }

  private async showActionDialog(data: ActionDialogData): Promise<ActionDialogResult | null> {
    const ref = this.dialog.open(ActionDialogComponent, {
      header: data.title,
      width: '640px',
      style: { maxWidth: 'calc(100vw - 32px)' },
      modal: true,
      focusOnShow: true,
      closable: true,
      closeOnEscape: true,
      closeAriaLabel: 'Fermer',
      data,
    });
    if (!ref) throw new Error('Impossible d’ouvrir la fenêtre de saisie.');
    const result: ActionDialogResult | undefined = await firstValueFrom(ref.onClose);
    return result ?? null;
  }

  private async confirmAction(
    title: string,
    description: string,
    submitLabel: string,
    danger = false,
  ): Promise<boolean> {
    if (this.pendingConfirmation) return false;
    this.pendingConfirmation = true;
    try {
      const result = await this.showActionDialog({
        title,
        description,
        submitLabel,
        danger,
        fields: [],
        confirmation: true,
      });
      return result?.['__confirmed'] === 'true';
    } finally {
      this.pendingConfirmation = false;
      this.changeDetectorRef.markForCheck();
    }
  }

  private currentWeekMonday(): string {
    const monday = new Date();
    monday.setDate(monday.getDate() - ((monday.getDay() + 6) % 7));
    return this.dateInputValue(monday);
  }

  private dateInputValue(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private formatDate(value: string): string {
    if (!value || value === '—') return '—';
    const date = new Date(`${value.slice(0, 10)}T00:00:00`);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('fr-FR');
  }

  private value(value: unknown): string {
    return value === null || value === undefined ? '' : String(value);
  }

  private message(error: unknown): string {
    return error instanceof Error ? error.message : 'Une erreur inattendue est survenue.';
  }

  private setError(error: unknown): void {
    this.error = this.message(error);
    this.changeDetectorRef.markForCheck();
  }

  private notify(message: string): void {
    this.notice = message;
    this.changeDetectorRef.markForCheck();
    window.setTimeout(() => {
      this.notice = '';
      this.changeDetectorRef.markForCheck();
    }, 3500);
  }
}
