import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnDestroy, Output, inject } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { ApiRow, ApiService } from '../api.service';

interface WeekHours {
  start: string;
  label: string;
  hours: number;
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, ButtonModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) dashboard!: ApiRow;
  @Input() refreshVersion = 0;
  @Output() navigate = new EventEmitter<'projects' | 'timesheets'>();

  private readonly api = inject(ApiService);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private requestVersion = 0;
  weeks: WeekHours[] = [];
  hoursLoading = false;
  hoursError = '';

  ngOnChanges(): void {
    if (this.refreshVersion !== this.loadedVersion) {
      this.loadedVersion = this.refreshVersion;
      void this.loadHours();
    }
  }

  private loadedVersion = -1;

  ngOnDestroy(): void {
    this.requestVersion++;
  }

  async loadHours(): Promise<void> {
    const version = ++this.requestVersion;
    this.hoursLoading = true;
    this.hoursError = '';
    try {
      const sheets = await this.api.get<ApiRow[]>('/api/timesheets?statut=VALIDEE');
      const weeks = this.buildWeeks(sheets);
      if (version === this.requestVersion) this.weeks = weeks;
    } catch (error: unknown) {
      if (version === this.requestVersion) {
        this.weeks = [];
        this.hoursError = error instanceof Error ? error.message : 'Impossible de charger les heures validées.';
      }
    } finally {
      if (version === this.requestVersion) {
        this.hoursLoading = false;
        this.changeDetectorRef.markForCheck();
      }
    }
  }

  get progress(): ApiRow[] {
    return this.rows('avancementProjets');
  }

  get budgets(): ApiRow[] {
    return this.rows('consommationBudgetProjets');
  }

  get belowPlanCount(): number {
    return this.progress.filter((item) => {
      const actual = this.numeric(item['avancementReel']);
      const expected = this.numeric(item['avancementAttendu']);
      return actual !== null && expected !== null && actual < expected;
    }).length;
  }

  get comparableCount(): number {
    return this.progress.filter((item) =>
      this.numeric(item['avancementReel']) !== null && this.numeric(item['avancementAttendu']) !== null,
    ).length;
  }

  get validatedHours(): number {
    return this.weeks.reduce((sum, week) => sum + week.hours, 0);
  }

  get maximumHours(): number {
    return Math.max(0, ...this.weeks.map((week) => week.hours));
  }

  text(value: unknown): string {
    return value === null || value === undefined || value === '' ? '—' : String(value);
  }

  numeric(value: unknown): number | null {
    return typeof value === 'number' && Number.isFinite(value) ? value : null;
  }

  amount(value: unknown): string {
    const number = this.numeric(value);
    return number === null ? '—' : new Intl.NumberFormat('fr-MA', {
      style: 'currency', currency: 'MAD', maximumFractionDigits: 0,
    }).format(number);
  }

  quantity(value: number): string {
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 }).format(value);
  }

  percent(value: unknown): string {
    const number = this.numeric(value);
    return number === null ? '—' : `${this.quantity(number)} %`;
  }

  width(value: unknown): number {
    const number = this.numeric(value);
    return number === null ? 0 : Math.max(0, Math.min(100, number));
  }

  weekHeight(hours: number): number {
    return this.maximumHours ? (hours / this.maximumHours) * 100 : 0;
  }

  private rows(key: string): ApiRow[] {
    const value = this.dashboard?.[key];
    return Array.isArray(value)
      ? value.filter((item): item is ApiRow => typeof item === 'object' && item !== null && !Array.isArray(item))
      : [];
  }

  private buildWeeks(sheets: ApiRow[]): WeekHours[] {
    if (!Array.isArray(sheets)) throw new Error('Données des feuilles de temps invalides.');
    const currentMonday = new Date();
    currentMonday.setHours(0, 0, 0, 0);
    currentMonday.setDate(currentMonday.getDate() - ((currentMonday.getDay() + 6) % 7));
    const weeks = Array.from({ length: 8 }, (_, index) => {
      const date = new Date(currentMonday);
      date.setDate(date.getDate() - (7 - index) * 7);
      const start = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
      return { start, label: date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' }), hours: 0 };
    });
    const byWeek = new Map(weeks.map((week) => [week.start, week]));
    for (const sheet of sheets) {
      if (!sheet || typeof sheet !== 'object' || Array.isArray(sheet)) {
        throw new Error('Données des feuilles de temps validées invalides.');
      }
      if (sheet['statut'] !== 'VALIDEE') {
        throw new Error('Statut inattendu dans les feuilles de temps validées.');
      }
      const date = sheet['semaineDebut'];
      const hours = this.numeric(sheet['totalHeuresSemaine']);
      if (typeof date !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(date) || hours === null || hours < 0) {
        throw new Error('Données des feuilles de temps validées invalides.');
      }
      const week = byWeek.get(date);
      if (week) week.hours += hours;
    }
    return weeks;
  }
}
