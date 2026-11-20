import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { FeeDetailComponent } from './fee-detail.component';
import { FeeService } from './fee.service';
describe('FeeDetailComponent', () => {
  async function create(id: string, fails = false) {
    const service = jasmine.createSpyObj<FeeService>('FeeService', ['get']);
    service.get.and.returnValue(fails ? throwError(() => ({ status: 404 })) : of({ id: 4, ...{ employeeId: '1', customerId: '2', serviceName: 'Conseil test', description: null, hourlyRate: 50, workedHours: 2.5, invoiceStatus: 'À facturer', paymentStatus: null, workDate: '2026-10-14' }, amount: 123.45, createdAt: null, updatedAt: null }));
    await TestBed.configureTestingModule({ imports: [FeeDetailComponent], providers: [provideRouter([]),
      provideZonelessChangeDetection(), { provide: FeeService, useValue: service },
      { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id })) } }] }).compileComponents();
    const fixture = TestBed.createComponent(FeeDetailComponent); fixture.detectChanges(); return { fixture, service };
  }
  it('shows server total, French amounts/dates and the edit link', async () => {
    const { fixture } = await create('4');
    expect(fixture.nativeElement.textContent).toContain('123,45');
    expect(fixture.nativeElement.textContent).toContain('2,5');
    expect(fixture.nativeElement.textContent).not.toContain('125,00');
    expect(fixture.nativeElement.textContent).toContain('14/10/2026');
    expect(fixture.nativeElement.textContent).toContain('À facturer');
    expect(fixture.nativeElement.querySelector('a[href="/fees/4/edit"]')).toBeTruthy();
  });
  it('does not request an invalid ID', async () => {
    const { fixture, service } = await create('invalid');
    expect(service.get).not.toHaveBeenCalled(); expect(fixture.nativeElement.textContent).toContain('Honoraire introuvable');
  });
  it('shows a missing fee without losing back navigation', async () => {
    const { fixture } = await create('4', true);
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Honoraire introuvable');
    expect(fixture.nativeElement.querySelector('a[href="/fees"]')).toBeTruthy();
  });
});
