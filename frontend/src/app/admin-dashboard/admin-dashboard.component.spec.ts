import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AdminDashboardComponent } from './admin-dashboard.component';

function monday(offset: number): string {
  const date = new Date();
  date.setHours(0, 0, 0, 0);
  date.setDate(date.getDate() - ((date.getDay() + 6) % 7) + offset * 7);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

describe('AdminDashboardComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminDashboardComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('groups validated hours into the last eight weeks and compares project indicators', async () => {
    const fixture = TestBed.createComponent(AdminDashboardComponent);
    fixture.componentRef.setInput('dashboard', {
      avancementProjets: [
        { nom: 'Portail', avancementReel: 30, avancementAttendu: 55 },
        { nom: 'Data', avancementReel: 80, avancementAttendu: 60 },
      ],
      consommationBudgetProjets: [{ nom: 'Portail', taux: 125, budgetTotal: 100, budgetConsomme: 125 }],
    });
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/timesheets?statut=VALIDEE').flush([
      { statut: 'VALIDEE', semaineDebut: monday(0), totalHeuresSemaine: 5.5 },
      { statut: 'VALIDEE', semaineDebut: monday(0), totalHeuresSemaine: 2 },
      { statut: 'VALIDEE', semaineDebut: monday(-7), totalHeuresSemaine: 3 },
      { statut: 'VALIDEE', semaineDebut: monday(-8), totalHeuresSemaine: 100 },
    ]);
    await new Promise((resolve) => setTimeout(resolve, 0));
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.weeks).toHaveLength(8);
    expect(component.weeks[0].hours).toBe(3);
    expect(component.weeks[7].hours).toBe(7.5);
    expect(component.validatedHours).toBe(10.5);
    expect(component.belowPlanCount).toBe(1);
    expect(component.width(125)).toBe(100);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('125 %');
    http.verify();
  });

  it('shows an error instead of zero hours and permits retry', async () => {
    const fixture = TestBed.createComponent(AdminDashboardComponent);
    fixture.componentRef.setInput('dashboard', {});
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/timesheets?statut=VALIDEE').flush(
      { message: 'Service indisponible' }, { status: 503, statusText: 'Unavailable' },
    );
    await new Promise((resolve) => setTimeout(resolve, 0));
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Service indisponible');
    expect(fixture.componentInstance.weeks).toEqual([]);

    const retry = fixture.componentInstance.loadHours();
    http.expectOne('/api/timesheets?statut=VALIDEE').flush([]);
    await retry;
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Aucune heure validée');
    http.verify();
  });

  it('ignores a late response from an earlier refresh', async () => {
    const fixture = TestBed.createComponent(AdminDashboardComponent);
    fixture.componentRef.setInput('dashboard', {});
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    const first = http.expectOne('/api/timesheets?statut=VALIDEE');
    fixture.componentRef.setInput('refreshVersion', 1);
    fixture.detectChanges();
    http.expectOne('/api/timesheets?statut=VALIDEE').flush([
      { statut: 'VALIDEE', semaineDebut: monday(0), totalHeuresSemaine: 8 },
    ]);
    await new Promise((resolve) => setTimeout(resolve, 0));
    first.flush([{ statut: 'VALIDEE', semaineDebut: monday(0), totalHeuresSemaine: 2 }]);
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(fixture.componentInstance.validatedHours).toBe(8);
    http.verify();
  });
});
