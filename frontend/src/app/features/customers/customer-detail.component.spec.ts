import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CustomerDetailComponent } from './customer-detail.component';
import { CustomerService } from './customer.service';

describe('CustomerDetailComponent', () => {
  async function create(id: string, fails = false) {
    const service = jasmine.createSpyObj<CustomerService>('CustomerService', ['get']);
    service.get.and.returnValue(fails ? throwError(() => ({ status: 404 })) : of({
      id: 4, name: 'Client test', company: 'Société test', email: null, phone: null, address: null,
      createdAt: '2026-10-14T10:00:00', updatedAt: null
    }));
    await TestBed.configureTestingModule({ imports: [CustomerDetailComponent], providers: [
      provideRouter([]), provideZonelessChangeDetection(), { provide: CustomerService, useValue: service },
      { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id })) } }
    ] }).compileComponents();
    const fixture = TestBed.createComponent(CustomerDetailComponent); fixture.detectChanges();
    return { fixture, service };
  }
  it('displays actual fields, optional fallbacks and an edit link', async () => {
    const { fixture, service } = await create('4');
    expect(service.get).toHaveBeenCalledOnceWith(4);
    expect(fixture.nativeElement.textContent).toContain('Société test');
    expect(fixture.nativeElement.textContent).toContain('Non renseigné');
    expect(fixture.nativeElement.textContent).toContain('14/10/2026');
    expect(fixture.nativeElement.querySelector('a[href="/customers/4/edit"]')).toBeTruthy();
  });
  it('rejects an invalid ID without calling the API', async () => {
    const { fixture, service } = await create('invalid');
    expect(service.get).not.toHaveBeenCalled(); expect(fixture.nativeElement.textContent).toContain('Client introuvable');
  });
  it('shows a missing customer and retains navigation back', async () => {
    const { fixture } = await create('4', true);
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Client introuvable');
    expect(fixture.nativeElement.querySelector('a[href="/customers"]')).toBeTruthy();
  });
});
