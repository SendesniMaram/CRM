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
import { CrmRole } from '../../core/models/role.model';

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
    category: FormControl<string>; segment: FormControl<string>; contactType: FormControl<string>;
    paginate(event: PageEvent): void; sort(event: Sort): void; remove(row: CustomerResponse): void };
  beforeEach(async () => {
    service = jasmine.createSpyObj('CustomerService', ['list', 'delete','restore','importExcel','importCsv','exportCsv','exportExcel']);
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
    expect(fixture.nativeElement.textContent).toContain('Aucun contact à afficher');
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
  it('filters commercial contacts by type, category and segment', () => {
    fixture.detectChanges();
    view.contactType.setValue('SUPPLIER');
    view.category.setValue('  Entreprise ');
    view.segment.setValue('Nord');
    view.find();
    expect(service.list.calls.mostRecent().args[0]).toEqual({
      page: 0, size: 10, sortBy: 'name', direction: 'asc',
      contactType: 'SUPPLIER', category: 'Entreprise', segment: 'Nord'
    });
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
  it('archives only after confirmation and returns to the previous page when needed', () => {
    fixture.detectChanges(); view.page.set(1); view.remove(row);
    confirmation.next(true); confirmation.complete();
    expect(service.delete).toHaveBeenCalledOnceWith(1);
    expect(service.list.calls.mostRecent().args[0].page).toBe(0);
    expect(snack.open).toHaveBeenCalledWith('Contact archivé.', 'Fermer', { duration: 5000 });
  });
  it('explains a backend refusal without exposing raw database text', () => {
    service.delete.and.returnValue(throwError(() => ({ status: 409, error: { message: 'private SQL' } })));
    fixture.detectChanges(); view.remove(row); confirmation.next(true); confirmation.complete();
    expect(snack.open.calls.mostRecent().args[0]).toContain('Archivage refusé');
    expect(snack.open.calls.mostRecent().args[0]).not.toContain('SQL');
  });
  it('does not allow deletion without administrative permission', () => {
    auth.hasAnyRole.and.returnValue(false); fixture.detectChanges(); view.remove(row);
    expect(dialog.open).not.toHaveBeenCalled(); expect(service.delete).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('a[href="/customers/new"]')).toBeNull();
  });
  it('sends combined archive and assignment filters to the paged service',()=>{fixture.detectChanges();const c=fixture.componentInstance;c['assignedEmployeeId'].setValue('101');c['status'].setValue('ARCHIVED');c['category'].setValue('Tech');c['search'].setValue('Acme');c['find']();expect(service.list.calls.mostRecent().args[0]).toEqual(jasmine.objectContaining({assignedEmployeeId:'101',status:'ARCHIVED',category:'Tech',keyword:'Acme',page:0}));});
  it('requires XLSX selection confirmation and shows per-row report',()=>{fixture.detectChanges();const c=fixture.componentInstance,file=new File(['synthetic'],'contacts.xlsx');const event={target:{files:[file],value:'selected'}} as unknown as Event;c['selectExcel'](event);expect(service.importExcel).not.toHaveBeenCalled();const report={totalRows:2,acceptedRows:1,rejectedRows:1,issues:[{row:3,message:'Duplicate'}]};service.importExcel.and.returnValue(of(report));c['importExcel']();fixture.detectChanges();expect(service.importExcel).toHaveBeenCalledWith(file);expect(c['importReport']()).toEqual(report);expect(fixture.nativeElement.textContent).toContain('Duplicate');});
  it('rejects invalid XLSX files before upload and explains a server refusal',()=>{const c=fixture.componentInstance;c['selectExcel']({target:{files:[new File(['x'],'macro.xlsm')],value:'x'}} as unknown as Event);expect(c['pendingExcel']()).toBeNull();expect(service.importExcel).not.toHaveBeenCalled();c['pendingExcel'].set(new File(['x'],'contacts.xlsx'));service.importExcel.and.returnValue(throwError(()=>({status:400})));c['importExcel']();expect(c['importError']()).toContain('sans formules');});
  it('renders archives read-only and exposes restoration',()=>{service.list.and.returnValue(of({...result,content:[{...row,archived:true}]}));service.restore.and.returnValue(of({...row,archived:false}));fixture.detectChanges();expect(fixture.nativeElement.textContent).toContain('Archivé');expect(fixture.nativeElement.querySelector('a[href="/customers/1/edit"]')).toBeNull();fixture.componentInstance['restore']({...row,archived:true});expect(service.restore).toHaveBeenCalledWith(1);});
  function action(label: string): HTMLButtonElement {
    return [...fixture.nativeElement.querySelectorAll('.page-actions button') as NodeListOf<HTMLButtonElement>]
      .find(button => button.textContent?.includes(label))!;
  }
  for (const role of Object.values(CrmRole)) {
    it('preserves toolbar permissions for '+role, () => {
      auth.hasAnyRole.and.callFake(required => required.includes(role)); fixture.detectChanges();
      const allowed = role === CrmRole.ADMIN || role === CrmRole.SUPER_ADMIN;
      expect(!!fixture.nativeElement.querySelector('.page-actions')).toBe(allowed);
      expect(!!fixture.nativeElement.querySelector('.primary-action')).toBe(allowed);
      if(allowed) {
        expect(action('Exporter CSV').disabled).toBeFalse();
        expect(action('Importer XLSX').disabled).toBeFalse();
        expect(fixture.nativeElement.querySelector('a[href="/customers/requests"]')).not.toBeNull();
      }
    });
  }
  it('keeps both exports disabled during export and passes the current filters', () => {
    const pending=new Subject<Blob>(); service.exportCsv.and.returnValue(pending);
    fixture.detectChanges(); const c=fixture.componentInstance;
    c['search'].setValue(' Test '); c['assignedEmployeeId'].setValue('101'); c['status'].setValue('ARCHIVED');
    action('Exporter CSV').click(); fixture.detectChanges();
    expect(service.exportCsv).toHaveBeenCalledWith(jasmine.objectContaining({keyword:'Test',assignedEmployeeId:'101',status:'ARCHIVED'}));
    expect(action('Export en cours').disabled).toBeTrue(); expect(action('Exporter Excel').disabled).toBeTrue();
    expect(action('Importer CSV').disabled).toBeFalse();
    action('Exporter Excel').click(); expect(service.exportExcel).not.toHaveBeenCalled();
    pending.error({status:503}); fixture.detectChanges(); expect(action('Exporter CSV').disabled).toBeFalse();
  });
  it('keeps both imports disabled during an upload and preserves CSV and XLSX pickers', () => {
    fixture.detectChanges();
    const csv=fixture.nativeElement.querySelector('input[accept=".csv,text/csv"]') as HTMLInputElement;
    const xlsx=fixture.nativeElement.querySelector('input[accept=".xlsx"]') as HTMLInputElement;
    const csvClick=spyOn(csv,'click'),xlsxClick=spyOn(xlsx,'click');
    action('Importer CSV').click(); action('Importer XLSX').click();
    expect(csvClick).toHaveBeenCalledTimes(1); expect(xlsxClick).toHaveBeenCalledTimes(1);
    const pending=new Subject<never>(); service.importCsv.and.returnValue(pending);
    fixture.componentInstance['importCsv']({target:{files:[new File(['test'],'contacts.csv')],value:'x'}} as unknown as Event);
    fixture.detectChanges(); expect(action('Import en cours').disabled).toBeTrue(); expect(action('Importer XLSX').disabled).toBeTrue();
    expect(action('Exporter CSV').disabled).toBeFalse();
    action('Importer XLSX').click(); expect(xlsxClick).toHaveBeenCalledTimes(1);
    pending.error({status:400});fixture.detectChanges();expect(action('Importer CSV').disabled).toBeFalse();
  });
  it('keeps the Excel export download functional without triggering a real download', () => {
    service.exportExcel.and.returnValue(of(new Blob(['test'])));
    const create=spyOn(URL,'createObjectURL').and.returnValue('blob:unit-test');
    const revoke=spyOn(URL,'revokeObjectURL');
    const click=spyOn(HTMLAnchorElement.prototype,'click');
    fixture.detectChanges();action('Exporter Excel').click();
    expect(service.exportExcel).toHaveBeenCalledWith({});expect(create).toHaveBeenCalled();expect(click).toHaveBeenCalled();expect(revoke).toHaveBeenCalledWith('blob:unit-test');
  });
  it('submits all filters together and keeps the assignment label readable', () => {
    fixture.detectChanges();const c=fixture.componentInstance;
    c['contactType'].setValue('LEAD');c['category'].setValue(' Tech ');c['segment'].setValue('Nord');c['assignedEmployeeId'].setValue('101');c['status'].setValue('ACTIVE');
    fixture.nativeElement.querySelector('form.toolbar').dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));
    expect(service.list.calls.mostRecent().args[0]).toEqual(jasmine.objectContaining({contactType:'LEAD',category:'Tech',segment:'Nord',assignedEmployeeId:'101',status:'ACTIVE'}));
    expect(fixture.nativeElement.querySelector('.filter-grid').textContent).toContain('Employé affecté');
    expect(fixture.nativeElement.querySelector('mat-hint').textContent).toContain('Identifiant');
  });
  for(const width of [375,768,1280,1440]) {
    it('keeps actions paired and filters contained at '+width+'px', () => {
      fixture.detectChanges();const frame=document.createElement('iframe');
      frame.style.cssText=`position:absolute;left:-10000px;width:${width}px;height:1000px;border:0;visibility:hidden`;
      document.body.appendChild(frame);
      try {
        const doc=frame.contentDocument!;const reset=doc.createElement('style');reset.textContent='html,body{margin:0;width:100%}';doc.head.appendChild(reset);
        document.querySelectorAll('style').forEach(style=>doc.head.appendChild(style.cloneNode(true)));
        doc.body.appendChild(fixture.nativeElement.cloneNode(true));
        expect(doc.documentElement.scrollWidth).toBeLessThanOrEqual(width+1);
        const imports=doc.querySelectorAll<HTMLButtonElement>('[aria-label="Importer des contacts"] button');
        if(width>550)expect(Math.abs(imports[0].getBoundingClientRect().top-imports[1].getBoundingClientRect().top)).toBeLessThan(1);
        doc.querySelectorAll('button,.headline-actions a').forEach(button=>{if(button.closest('.page-header'))expect(button.getBoundingClientRect().height).toBeGreaterThanOrEqual(44);});
        const label=[...doc.querySelectorAll('mat-label')].find(el=>el.textContent==='Employé affecté')!;
        expect(label.getBoundingClientRect().width).toBeLessThanOrEqual(label.closest('mat-form-field')!.getBoundingClientRect().width);
      }finally{frame.remove();}
    });
  }
  it('keeps secondary text and icons readable in normal and disabled states', () => {
    fixture.detectChanges();
    function luminance(rgb:string){const values=rgb.match(/[\d.]+/g)!.slice(0,3).map(n=>{const v=Number(n)/255;return v<=.04045?v/12.92:((v+.055)/1.055)**2.4;});return values[0]*.2126+values[1]*.7152+values[2]*.0722;}
    for(const disabled of [false,true]){
      fixture.componentInstance['exporting'].set(disabled);fixture.detectChanges();
      const button=action('Exporter Excel');const style=getComputedStyle(button);
      const fg=luminance(style.color),bg=luminance(style.backgroundColor);
      expect((Math.max(fg,bg)+.05)/(Math.min(fg,bg)+.05)).toBeGreaterThanOrEqual(4.5);
      expect(getComputedStyle(button.querySelector('mat-icon')!).color).toBe(style.color);
    }
  });
});
