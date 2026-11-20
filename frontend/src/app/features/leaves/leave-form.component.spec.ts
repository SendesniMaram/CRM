import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { LeaveFormComponent, localToday } from './leave-form.component';
import { LeaveService } from './leave.service';
describe('LeaveFormComponent', () => {
  let fixture: ComponentFixture<LeaveFormComponent>; let service: jasmine.SpyObj<LeaveService>; let params: Subject<ReturnType<typeof convertToParamMap>>;
  const row = { id: 7, employeeId: '101', startDate: '2099-01-01', endDate: '2099-01-02', reason: 'Repos', status: 'PENDING' as const, validatorUsername: null, createdAt: '', updatedAt: '' };
  beforeEach(async () => {
    params = new Subject(); service = jasmine.createSpyObj('LeaveService', ['get', 'create', 'update']);
    service.get.and.returnValue(of(row)); service.create.and.returnValue(of(row)); service.update.and.returnValue(of(row));
    await TestBed.configureTestingModule({ imports: [LeaveFormComponent], providers: [provideZonelessChangeDetection(), provideRouter([]),
      { provide: LeaveService, useValue: service }, { provide: ActivatedRoute, useValue: { paramMap: params } }]
    }).compileComponents(); fixture = TestBed.createComponent(LeaveFormComponent); spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true); fixture.detectChanges();
  });
  function component(): any { return fixture.componentInstance; }
  function create() { params.next(convertToParamMap({})); fixture.detectChanges(); }
  it('has no employeeId input and requires dates', () => { create(); expect(fixture.nativeElement.querySelector('[formControlName="employeeId"]')).toBeNull(); component().submit(); expect(service.create).not.toHaveBeenCalled(); });
  it('creates with dates/reason and returns to list', () => {
    create(); component().form.setValue({ startDate: row.startDate, endDate: row.endDate, reason: ' Repos ' }); component().submit();
    expect(service.create).toHaveBeenCalledWith({ startDate: row.startDate, endDate: row.endDate, reason: 'Repos' }); expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/leaves']);
  });
  it('accepts same-day dates', () => { create(); component().form.setValue({ startDate: localToday(), endDate: localToday(), reason: '' }); expect(component().form.valid).toBeTrue(); });
  it('rejects past, impossible and inverted dates', () => {
    create(); for (const [startDate, endDate] of [['2000-01-01', '2000-01-02'], ['2099-02-30', '2099-03-01'], ['2099-01-03', '2099-01-02']]) {
      component().form.setValue({ startDate, endDate, reason: '' }); component().submit(); expect(component().form.invalid).toBeTrue();
    } expect(service.create).not.toHaveBeenCalled();
  });
  it('rejects a reason longer than 500', () => { create(); component().form.setValue({ startDate: row.startDate, endDate: row.endDate, reason: 'x'.repeat(501) }); component().submit(); expect(service.create).not.toHaveBeenCalled(); });
  it('prefills and updates only dates/reason', () => {
    params.next(convertToParamMap({ id: '7' })); fixture.detectChanges(); expect(component().form.getRawValue().reason).toBe('Repos'); component().submit();
    expect(service.update).toHaveBeenCalledWith(7, { startDate: row.startDate, endDate: row.endDate, reason: 'Repos' });
  });
  it('does not edit a decided request', () => { service.get.and.returnValue(of({ ...row, status: 'REJECTED' })); params.next(convertToParamMap({ id: '7' })); fixture.detectChanges(); component().submit(); expect(service.update).not.toHaveBeenCalled(); expect(fixture.nativeElement.textContent).toContain('déjà décidée'); });
  it('does not edit an inaccessible request', () => { service.get.and.returnValue(throwError(() => ({ status: 403 }))); params.next(convertToParamMap({ id: '7' })); component().submit(); expect(component().ready()).toBeFalse(); expect(service.update).not.toHaveBeenCalled(); });
  it('shows loading and refuses invalid IDs', () => { const stream = new Subject<any>(); service.get.and.returnValue(stream); params.next(convertToParamMap({ id: '7' })); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Chargement'); stream.next(row); stream.complete(); params.next(convertToParamMap({ id: '0' })); expect(component().ready()).toBeFalse(); });
  it('blocks double save and shows safe failure', () => {
    create(); const stream = new Subject<any>(); service.create.and.returnValue(stream); component().form.setValue({ startDate: row.startDate, endDate: row.endDate, reason: '' }); component().submit(); component().submit(); expect(service.create).toHaveBeenCalledTimes(1);
    stream.error({ status: 409, message: 'internal details' }); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Enregistrement refusé'); expect(fixture.nativeElement.textContent).not.toContain('internal details');
  });
});
