import { provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, of, throwError } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { DashboardData, DashboardDataService, DashboardScope, emptyDashboard } from './dashboard-data.service';
import { DashboardComponent } from './dashboard.component';
import { ClientService } from '../client/client.service';

describe('DashboardComponent', () => {
  const role = signal(CrmRole.ADMIN);
  const username = signal('test-user');
  let fixture: ComponentFixture<DashboardComponent>;
  let service: jasmine.SpyObj<DashboardDataService>;
  let snapshot: DashboardData;
  function scope(): DashboardScope {
    return role() === CrmRole.ADMIN || role() === CrmRole.SUPER_ADMIN ? 'admin'
      : role() === CrmRole.EMPLOYEE ? 'employee' : 'client';
  }
  beforeEach(async () => {
    role.set(CrmRole.ADMIN);
    username.set('test-user');
    snapshot = { ...emptyDashboard('admin'), employees: { value: 125, error: '' },
      customers: { value: null, error: 'Service indisponible.' }, fees: { value: 0, error: '' },
      payroll: { value: 12, error: '' }, leaves: { value: [
        { id: 1, employeeId: '7', startDate: '2026-10-20', endDate: '2026-10-21', reason: null,
          status: 'PENDING', validatorUsername: null, createdAt: '2026-10-16T10:00:00', updatedAt: '2026-10-16T10:00:00' }
      ], error: '' }, invoices: { value: { count: 3, amounts: [{ currency: 'TND', value: '100,00' }], statuses: [
        { label: 'PAYÉE', count: 3, percent: 100 }], reason: '' }, error: '' } };
    service = jasmine.createSpyObj('DashboardDataService', ['scope', 'load']);
    service.scope.and.callFake(scope);
    service.load.and.callFake(() => of(scope() === 'admin' ? snapshot : {
      ...emptyDashboard(scope()), leaves: scope() === 'employee' ? snapshot.leaves : { value: null, error: '' }
    }));
    await TestBed.configureTestingModule({ imports: [DashboardComponent], providers: [
      provideZonelessChangeDetection(), provideRouter([]), { provide: DashboardDataService, useValue: service },
      { provide: AuthService, useValue: { sessionGeneration: 0,
        hasAnyRole: (roles: CrmRole[]) => roles.includes(role()),
        getCurrentUser: () => ({ username: username(), email: 'unit@example.test' }) } },
      { provide: ClientService, useValue: {
        profile: () => of({customerId:'101',name:'Customer backend',email:'client@example.test',phone:null,company:null}),
        invoices: () => of({content:[],totalElements:0,totalPages:0,number:0}) } },
      { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } }
    ] }).compileComponents();
    fixture = TestBed.createComponent(DashboardComponent);
  });
  it('renders real KPI values, zero and unavailable as distinct states without mock statistics', () => {
    fixture.detectChanges(); const text = fixture.nativeElement.textContent;
    expect(text).toContain('Tableau de bord'); expect(text).toContain('125');
    expect(text).toContain('100,00'); expect(text).toContain('TND'); expect(text).toContain('Indisponible');
    expect(fixture.componentInstance['cards']().find(card => card.title === 'Honoraires')?.value).toBe(0);
    expect(fixture.componentInstance['cards']().find(card => card.title === 'Clients')?.value).toBeNull();
    for (const value of ['€86.4K', 'Sophie Martin', 'Aperçu temporaire', 'Clients actifs']) expect(text).not.toContain(value);
  });
  it('shows loading and clears stale values on refresh', () => {
    fixture.detectChanges(); const pending = new Subject<DashboardData>(); service.load.and.returnValue(pending);
    fixture.componentInstance['refresh'](); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Chargement des données');
    expect(fixture.nativeElement.textContent).not.toContain('125'); pending.next(snapshot); pending.complete();
    fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('125');
  });
  it('shows French notifications for partial refresh and recovery', () => {
    fixture.detectChanges(); fixture.componentInstance['refresh']();
    expect(TestBed.inject(MatSnackBar).open).toHaveBeenCalledWith(
      'Actualisation partielle : certaines données sont indisponibles.', 'Fermer', { duration: 5000 });
    service.load.and.returnValue(of({ ...emptyDashboard('admin') })); fixture.componentInstance['refresh']();
    expect(TestBed.inject(MatSnackBar).open).toHaveBeenCalledWith('Tableau de bord actualisé.', 'Fermer', { duration: 5000 });
  });
  it('shows empty distributions and activities instead of invented data', () => {
    snapshot.leaves.value = []; snapshot.invoices.value = { count: 0, amounts: [], statuses: [], reason: '' };
    fixture.detectChanges(); const text = fixture.nativeElement.textContent;
    expect(text).toContain('Aucune demande de congé'); expect(text).toContain('Aucune facture');
    expect(text).toContain('Aucune demande datée disponible');
  });
  it('renders status bars and recent creation timestamps with valid navigation', () => {
    fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('En attente');
    expect(fixture.nativeElement.textContent).toContain('PAYÉE'); expect(fixture.nativeElement.textContent).toContain('16/10/2026');
    expect(fixture.nativeElement.querySelectorAll('mat-progress-bar[mode="determinate"]').length).toBe(2);
    expect(fixture.nativeElement.querySelector('a[href="/leaves"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('a[href="/payroll"]')).toBeTruthy();
  });
  it('shows only personal leave data and links for EMPLOYEE', () => {
    role.set(CrmRole.EMPLOYEE); fixture.detectChanges(); const text = fixture.nativeElement.textContent;
    expect(text).toContain('Mes demandes de congé'); expect(text).toContain('Mes congés en attente');
    for (const path of ['/employees', '/customers', '/invoices', '/fees', '/payroll']) {
      expect(fixture.nativeElement.querySelector('a[href="' + path + '"]')).toBeNull();
    }
    expect(text).not.toContain('Montant total'); expect(text).not.toContain('125');
  });
  it('shows account information and no administrative indicators for CLIENT', () => {
    role.set(CrmRole.CLIENT); fixture.detectChanges(); const text = fixture.nativeElement.textContent;
    expect(text).toContain('test-user'); expect(text).toContain('Customer backend'); expect(text).toContain('Mes factures');
    expect(fixture.nativeElement.querySelector('.kpis')).toBeNull();
    expect(fixture.nativeElement.querySelector('nav[aria-label="Accès rapides"]')).toBeNull();
  });
  it('clears administrative values immediately when roles change', () => {
    fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('125');
    role.set(CrmRole.CLIENT); fixture.detectChanges(); expect(fixture.nativeElement.textContent).not.toContain('125');
    expect(service.load).toHaveBeenCalledTimes(2);
  });
  it('never reuses an administrative snapshot for employee personal KPIs before effects run', () => {
    fixture.detectChanges(); role.set(CrmRole.EMPLOYEE);
    expect(fixture.componentInstance['visible']()).toBeNull();
    expect(fixture.componentInstance['cards']().every(card => card.value === null)).toBeTrue();
    expect(fixture.componentInstance['recent']()).toEqual([]);
  });
  it('clears personal data when switching accounts even with the same role', () => {
    role.set(CrmRole.EMPLOYEE); fixture.detectChanges();
    expect(fixture.componentInstance['visible']()?.leaves.value?.length).toBe(1);
    username.set('another-employee');
    expect(fixture.componentInstance['visible']()).toBeNull();
    fixture.detectChanges(); expect(service.load).toHaveBeenCalledTimes(2);
  });
  it('cancels obsolete requests on refresh and on destruction', () => {
    const old = new Subject<DashboardData>(); const latest = new Subject<DashboardData>();
    service.load.and.returnValues(old, latest); fixture.detectChanges(); fixture.componentInstance['refresh']();
    expect(old.observed).toBeFalse(); old.next(snapshot); expect(fixture.componentInstance['data']()).toBeNull();
    fixture.destroy(); expect(latest.observed).toBeFalse();
  });
  it('handles unexpected load failure without retaining previous figures', () => {
    fixture.detectChanges(); service.load.and.returnValue(throwError(() => new Error('test')));
    fixture.componentInstance['refresh'](); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Impossible');
    expect(fixture.nativeElement.textContent).not.toContain('125');
  });
  it('hides previous figures when a new session reuses the same username and role',()=>{
    const generation=signal(0);Object.defineProperty(TestBed.inject(AuthService),'sessionGeneration',{get:()=>generation()});
    fixture.detectChanges();expect(fixture.componentInstance['visible']()).not.toBeNull();
    const pending=new Subject<DashboardData>();service.load.and.returnValue(pending);generation.set(1);
    expect(fixture.componentInstance['visible']()).toBeNull();fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('125');expect(service.load).toHaveBeenCalledTimes(2);
  });
});
