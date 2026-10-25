import { provideZonelessChangeDetection, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Sort } from '@angular/material/sort';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CustomerPage, CustomerResponse } from './customer.models';
import { CustomerService } from './customer.service';
import { CustomersPageComponent } from './customers-page.component';

describe('CustomersPageComponent', () => {
  const row: CustomerResponse = { id: 1, name: 'Client test', company: 'Entreprise test', email: null,
    phone: null, address: null, createdAt: null, updatedAt: null };
  const result: CustomerPage = { content: [row], totalElements: 31, totalPages: 4, number: 0, size: 10 };
  let fixture: ComponentFixture<CustomersPageComponent>;
  let service: jasmine.SpyObj<CustomerService>;
  let auth: jasmine.SpyObj<AuthService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let confirmation: Subject<boolean>;
  let view: { search: FormControl<string>; page: WritableSignal<number>; find(): void;
    paginate(event: PageEvent): void; sort(event: Sort): void; remove(row: CustomerResponse): void };
  beforeEach(async () => {
    service = jasmine.createSpyObj('CustomerService', ['list', 'delete']);
    service.list.and.returnValue(of(result)); service.delete.and.returnValue(of(undefined));
    auth = jasmine.createSpyObj('AuthService', ['hasAnyRole']); auth.hasAnyRole.and.returnValue(true);
    confirmation = new Subject<boolean>();
    dialog = jasmine.createSpyObj('MatDialog', ['open']);
    dialog.open.and.returnValue({ afterClosed: () => confirmation } as unknown as ReturnType<MatDialog['open']>);
    snack = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({ imports: [CustomersPageComponent], providers: [
      provideZonelessChangeDetection(), provideRouter([]),
      { provide: CustomerService, useValue: service }, { provide: AuthService, useValue: auth },
      { provide: MatDialog, useValue: dialog }, { provide: MatSnackBar, useValue: snack }
    ] }).compileComponents();
    fixture = TestBed.createComponent(CustomersPageComponent);
    view = fixture.componentInstance as unknown as typeof view;
  });
  it('loads real service results and links to creation/detail/edit', () => {
    fixture.detectChanges();
    expect(service.list).toHaveBeenCalledWith({ page: 0, size: 10, sortBy: 'name', direction: 'asc' });
    expect(fixture.nativeElement.textContent).toContain('Client test');
    for (const path of ['/customers/new', '/customers/1', '/customers/1/edit'])
      expect(fixture.nativeElement.querySelector('a[href="' + path + '"]')).toBeTruthy();
  });
  it('shows loading then an empty state', () => {
    const pending = new Subject<CustomerPage>(); service.list.and.returnValue(pending);
    fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Chargement des clients');
    pending.next({ ...result, content: [], totalElements: 0 }); pending.complete(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Aucun client à afficher');
  });
  it('shows an API error without stale rows', () => {
    service.list.and.returnValue(throwError(() => ({ status: 503 }))); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Impossible de charger');
    expect(fixture.nativeElement.querySelector('table')).toBeNull();
  });
  it('resets the page when searching and passes the supported keyword', () => {
    fixture.detectChanges(); view.page.set(2); view.search.setValue(' test '); view.find();
    expect(service.list.calls.mostRecent().args[0]).toEqual({ page: 0, size: 10, sortBy: 'name', direction: 'asc', keyword: 'test' });
  });
  it('requests server pagination and sorting', () => {
    fixture.detectChanges(); view.paginate({ pageIndex: 2, pageSize: 25, length: 80 });
    expect(service.list.calls.mostRecent().args[0].page).toBe(2);
    expect(service.list.calls.mostRecent().args[0].size).toBe(25);
    view.sort({ active: 'email', direction: 'desc' });
    expect(service.list.calls.mostRecent().args[0]).toEqual({ page: 0, size: 25, sortBy: 'email', direction: 'desc' });
  });
  it('cancels an older search response', () => {
    const stale = new Subject<CustomerPage>(); service.list.and.returnValues(stale, of({ ...result, content: [] }));
    fixture.detectChanges(); view.search.setValue('new'); view.find(); stale.next(result); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('table')).toBeNull();
  });
  it('does not delete on cancellation or a duplicate click', () => {
    fixture.detectChanges(); view.remove(row); view.remove(row);
    expect(dialog.open).toHaveBeenCalledTimes(1);
    expect(service.delete).not.toHaveBeenCalled(); confirmation.next(false); confirmation.complete();
    expect(service.delete).not.toHaveBeenCalled();
  });
  it('deletes only after confirmation and returns to the previous page when needed', () => {
    fixture.detectChanges(); view.page.set(1); view.remove(row);
    confirmation.next(true); confirmation.complete();
    expect(service.delete).toHaveBeenCalledOnceWith(1);
    expect(service.list.calls.mostRecent().args[0].page).toBe(0);
    expect(snack.open).toHaveBeenCalledWith('Client supprimé.', 'Fermer', { duration: 5000 });
  });
  it('explains a backend refusal without exposing raw database text', () => {
    service.delete.and.returnValue(throwError(() => ({ status: 409, error: { message: 'private SQL' } })));
    fixture.detectChanges(); view.remove(row); confirmation.next(true); confirmation.complete();
    expect(snack.open.calls.mostRecent().args[0]).toContain('Suppression refusée');
    expect(snack.open.calls.mostRecent().args[0]).not.toContain('SQL');
  });
  it('does not allow deletion without administrative permission', () => {
    auth.hasAnyRole.and.returnValue(false); fixture.detectChanges(); view.remove(row);
    expect(dialog.open).not.toHaveBeenCalled(); expect(service.delete).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('a[href="/customers/new"]')).toBeNull();
  });
});
