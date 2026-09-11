import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [provideZonelessChangeDetection()]
    }).compileComponents();
  });

  it('creates the dashboard', () => {
    const fixture = TestBed.createComponent(DashboardComponent);

    expect(fixture.componentInstance).toBeTruthy();
  });

  it('displays the dashboard title and subtitle', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('h1').textContent).toContain('Tableau de bord');
    expect(fixture.nativeElement.textContent).toContain('principales informations');
  });

  it('displays the main statistics cards', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Employés');
    expect(fixture.nativeElement.textContent).toContain('Clients');
    expect(fixture.nativeElement.textContent).toContain('Départements');
    expect(fixture.nativeElement.textContent).toContain('Factures');
    expect(fixture.nativeElement.textContent).toContain('€86.4K');
  });

  it('displays recent activities and summary indicators', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Activités récentes');
    expect(fixture.nativeElement.textContent).toContain("Nouvelle fiche d'employé ajoutée");
    expect(fixture.nativeElement.textContent).toContain('Factures en attente');
    expect(fixture.nativeElement.textContent).toContain('Clients actifs');
  });
});