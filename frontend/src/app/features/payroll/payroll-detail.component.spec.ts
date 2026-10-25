import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { PayrollDetailComponent } from './payroll-detail.component';
import { PayrollService } from './payroll.service';
describe('PayrollDetailComponent', () => {
  async function create(id: string, fails = false) {
    const service = jasmine.createSpyObj<PayrollService>('PayrollService', ['get']);
    service.get.and.returnValue(fails ? throwError(() => ({ status: 404 })) : of({ id: 4, ...{ employeeId: '7', baseSalary: 2000, bonuses: 100, deductions: 50, taxAmount: 200, payPeriod: '2026-10', paymentDate: '2026-10-31', currency: 'TND', status: 'À payer' }, netSalary: 123.45, createdAt: null, updatedAt: null }));
    await TestBed.configureTestingModule({ imports: [PayrollDetailComponent], providers: [provideRouter([]),
      provideZonelessChangeDetection(), { provide: PayrollService, useValue: service },
      { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id })) } }] }).compileComponents();
    const fixture = TestBed.createComponent(PayrollDetailComponent); fixture.detectChanges(); return { fixture, service };
  }
  it('shows server total, French amounts/dates and the edit link', async () => {
    const { fixture } = await create('4');
    expect(fixture.nativeElement.textContent).toContain('123,45');
    expect(fixture.nativeElement.textContent).not.toContain('1 850,00');
    expect(fixture.nativeElement.textContent).toContain('31/10/2026');
    expect(fixture.nativeElement.textContent).toContain('À payer');
    expect(fixture.nativeElement.querySelector('a[href="/payroll/4/edit"]')).toBeTruthy();
  });
  it('does not request an invalid ID', async () => {
    const { fixture, service } = await create('invalid');
    expect(service.get).not.toHaveBeenCalled(); expect(fixture.nativeElement.textContent).toContain('Fiche de paie introuvable');
  });
  it('shows a missing payroll without losing back navigation', async () => {
    const { fixture } = await create('4', true);
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Fiche de paie introuvable');
    expect(fixture.nativeElement.querySelector('a[href="/payroll"]')).toBeTruthy();
  });
});
