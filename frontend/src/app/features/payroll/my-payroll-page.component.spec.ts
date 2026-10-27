import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { MyPayrollService } from './my-payroll.service';
import { MyPayrollPageComponent } from './my-payroll-page.component';

describe('MyPayrollPageComponent', () => {
  let fixture: ComponentFixture<MyPayrollPageComponent>;
  let service: jasmine.SpyObj<MyPayrollService>;
  const result = { content: [{ id: 5, employeeId: '101', baseSalary: 2000, bonuses: 100, deductions: 50,
    taxAmount: 250, netSalary: 1800, payPeriod: '2026-10', paymentDate: '2026-10-31', currency: 'TND',
    status: 'PAID', createdAt: null, updatedAt: null }], number: 0, totalElements: 1, totalPages: 1, size: 10 };

  beforeEach(() => {
    service = jasmine.createSpyObj<MyPayrollService>('MyPayrollService', ['list', 'read', 'pdf']);
    service.list.and.returnValue(of(result));
    TestBed.configureTestingModule({ imports: [MyPayrollPageComponent], providers: [provideZonelessChangeDetection(), provideRouter([]),
      { provide: MyPayrollService, useValue: service }, { provide: AuthService, useValue: { sessionGeneration: 1, hasAnyRole: () => true } }] });
    fixture = TestBed.createComponent(MyPayrollPageComponent);
  });

  it('shows real personal rows and supports period filtering', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Mes bulletins de paie');
    expect(fixture.nativeElement.textContent).toContain('2026-10');
    const component = fixture.componentInstance as any;
    component.period.setValue('2026-10'); component.filter();
    expect(service.list).toHaveBeenCalledWith(0, '2026-10');
  });

  it('shows a useful empty state and backend errors', () => {
    service.list.and.returnValue(of({ ...result, content: [], totalElements: 0, totalPages: 0 }));
    fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Aucun bulletin');
    service.list.and.returnValue(throwError(() => ({ status: 403 })));
    (fixture.componentInstance as any).load(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Accès réservé au compte employé associé');
    expect(fixture.nativeElement.querySelector('[role="alert"]')).not.toBeNull();
  });
});
