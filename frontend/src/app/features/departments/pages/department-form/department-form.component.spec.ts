import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { DepartmentResponse } from '../../../employees/models/employee.models';
import { DepartmentService } from '../../services/department.service';
import { DepartmentFormComponent } from './department-form.component';

describe('DepartmentFormComponent', () => {
  const department: DepartmentResponse = { id: 1, name: 'Informatique', code: 'IT', description: 'Support', createdAt: '', updatedAt: '' };
  let service: jasmine.SpyObj<DepartmentService>;
  let fixture: ComponentFixture<DepartmentFormComponent>;
  let router: Router;

  it('accepts 255 characters and empty description but blocks 256 characters before HTTP', async () => {
    const component = await create();
    component['form'].controls.name.setValue('Boundary');
    const control = component['form'].controls.description;
    control.setValue('é'.repeat(255));
    expect(control.valid).toBeTrue();
    expect(component['form'].valid).toBeTrue();
    control.setValue('é'.repeat(256));
    expect(control.hasError('maxlength')).toBeTrue();
    component['submit']();
    expect(service.createDepartment).not.toHaveBeenCalled();
    control.setValue('');
    expect(control.valid).toBeTrue();
  });

  beforeEach(() => {
    service = jasmine.createSpyObj<DepartmentService>('DepartmentService', ['getDepartmentById', 'createDepartment', 'updateDepartment']);
    service.getDepartmentById.and.returnValue(of(department));
    service.createDepartment.and.returnValue(of(department));
    service.updateDepartment.and.returnValue(of(department));
  });

  async function create(id?: string): Promise<DepartmentFormComponent> {
    await TestBed.configureTestingModule({ imports: [DepartmentFormComponent],
      providers: [provideRouter([]), provideZonelessChangeDetection(), { provide: DepartmentService, useValue: service },
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap(id === undefined ? {} : { id })) } }]
    }).compileComponents();
    router = TestBed.inject(Router); spyOn(router, 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(DepartmentFormComponent); fixture.detectChanges();
    return fixture.componentInstance;
  }

  it('requires a nonblank name and leaves optional fields optional', async () => {
    const component = await create();
    expect(component['form'].invalid).toBeTrue(); component['submit']();
    expect(service.createDepartment).not.toHaveBeenCalled();
    component['form'].controls.name.setValue('   '); expect(component['form'].invalid).toBeTrue();
    component['form'].controls.name.setValue('Informatique'); expect(component['form'].valid).toBeTrue();
    expect(service.getDepartmentById).not.toHaveBeenCalled();
  });

  it('enforces exactly the backend maximum lengths', async () => {
    const component = await create();
    for (const [field, max] of [['name', 255], ['code', 50], ['description', 255]] as const) {
      component['form'].controls[field].setValue('x'.repeat(max + 1));
      expect(component['form'].controls[field].hasError('maxlength')).withContext(field).toBeTrue();
      component['form'].controls[field].setValue('x'.repeat(max));
      expect(component['form'].controls[field].valid).withContext(field).toBeTrue();
    }
  });

  it('creates through the service and returns to the list', async () => {
    const component = await create(); component['form'].patchValue({ name: ' Informatique ', code: ' IT ', description: ' Support ' });
    component['submit']();
    expect(service.createDepartment).toHaveBeenCalledWith({ name: 'Informatique', code: 'IT', description: 'Support' });
    expect(service.updateDepartment).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/departments']);
  });

  it('sends null for blank optional fields instead of duplicate empty codes', async () => {
    const component = await create(); component['form'].controls.name.setValue('Informatique'); component['submit']();
    expect(service.createDepartment).toHaveBeenCalledWith({ name: 'Informatique', code: null, description: null });
  });

  it('prefills all edit fields and updates the existing ID', async () => {
    const component = await create('1');
    expect(service.getDepartmentById).toHaveBeenCalledWith(1);
    expect(component['form'].getRawValue()).toEqual({ name: 'Informatique', code: 'IT', description: 'Support' });
    component['form'].controls.description.setValue('Support informatique'); component['submit']();
    expect(service.updateDepartment).toHaveBeenCalledWith(1, { name: 'Informatique', code: 'IT', description: 'Support informatique' });
    expect(service.createDepartment).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/departments']);
  });

  it('blocks submit before the edit request completes', async () => {
    const pending = new Subject<DepartmentResponse>(); service.getDepartmentById.and.returnValue(pending);
    const component = await create('1'); component['submit']();
    expect(fixture.nativeElement.textContent).toContain('Chargement du département');
    expect(service.updateDepartment).not.toHaveBeenCalled();
    pending.next(department); pending.complete(); expect(component['ready']()).toBeTrue();
  });

  it('preserves entered data on duplicate or validation failure without showing backend text', async () => {
    service.createDepartment.and.returnValue(throwError(() => ({ status: 400, error: { message: 'private backend details' } })));
    const component = await create(); component['form'].controls.name.setValue('Informatique'); component['submit'](); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('unicité');
    expect(fixture.nativeElement.textContent).not.toContain('private backend details');
    expect(component['form'].controls.name.value).toBe('Informatique');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('shows a generic update error and restores saving state', async () => {
    service.updateDepartment.and.returnValue(throwError(() => ({ status: 500 })));
    const component = await create('1'); component['submit'](); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Impossible d’enregistrer');
    expect(component['saving']()).toBeFalse(); expect(router.navigate).not.toHaveBeenCalled();
  });

  it('blocks repeated saves and disables the form while saving', async () => {
    const pending = new Subject<DepartmentResponse>(); service.createDepartment.and.returnValue(pending);
    const component = await create(); component['form'].controls.name.setValue('Informatique');
    component['submit'](); component['submit'](); fixture.detectChanges();
    expect(service.createDepartment).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.querySelector('fieldset').disabled).toBeTrue();
    pending.error({ status: 500 }); expect(component['saving']()).toBeFalse();
  });

  for (const id of ['0', '-1', 'invalid', '1.5']) {
    it(`rejects invalid ID ${id} without a GET`, async () => {
      const component = await create(id); expect(component['notFound']()).toBeTrue();
      expect(service.getDepartmentById).not.toHaveBeenCalled(); component['submit']();
      expect(service.updateDepartment).not.toHaveBeenCalled();
    });
  }

  it('handles a missing department', async () => {
    service.getDepartmentById.and.returnValue(throwError(() => ({ status: 404 })));
    const component = await create('1'); expect(component['notFound']()).toBeTrue();
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
  });

  it('hides the form on a generic loading failure', async () => {
    service.getDepartmentById.and.returnValue(throwError(() => ({ status: 500 })));
    await create('1'); expect(fixture.nativeElement.textContent).toContain('Impossible de charger');
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
  });

  it('cancels pending loading on destruction', async () => {
    const pending = new Subject<DepartmentResponse>(); service.getDepartmentById.and.returnValue(pending);
    await create('1'); expect(pending.observed).toBeTrue(); fixture.destroy(); expect(pending.observed).toBeFalse();
  });
});
