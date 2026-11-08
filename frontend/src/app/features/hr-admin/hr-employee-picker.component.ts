import { ChangeDetectionStrategy,Component,DestroyRef,Injector,inject,input,output,signal,effect,untracked } from '@angular/core';import { FormsModule } from '@angular/forms';import { takeUntilDestroyed } from '@angular/core/rxjs-interop';import { EmployeeService } from '../employees/services/employee.service';import { AuthService } from '../../core/services/auth.service';import { CrmRole } from '../../core/models/role.model';import { hrError } from './hr-admin.models';
@Component({selector:'app-hr-employee-picker',standalone:true,imports:[FormsModule],templateUrl:'./hr-employee-picker.component.html',changeDetection:ChangeDetectionStrategy.OnPush})
export class HrEmployeePickerComponent {
 readonly selected=input('');readonly disabled=input(false);readonly employeeSelected=output<string>();
 private readonly injector=inject(Injector);private readonly auth=inject(AuthService);private readonly destroy=inject(DestroyRef);private revision=0;
 protected query='';protected readonly page=signal(0);protected readonly pages=signal(0);protected readonly choices=signal<{id:string;label:string}[]>([]);protected readonly loading=signal(false);protected readonly loaded=signal(false);protected readonly error=signal('');
 constructor(){effect(()=>{this.auth.getCurrentUser();const generation=this.auth.sessionGeneration;untracked(()=>{this.revision++;this.choices.set([]);this.error.set('');this.loading.set(false);this.loaded.set(false);this.query='';this.page.set(0);});});}
 private admin(){return this.auth.hasAnyRole([CrmRole.ADMIN,CrmRole.SUPER_ADMIN]);}
 protected find(){this.page.set(0);this.load();}
 protected move(offset:number){const page=this.page()+offset;if(page<0||page>=this.pages()||this.loading())return;this.page.set(page);this.load();}
 protected load(){if(this.disabled()||!this.admin())return;const revision=++this.revision,generation=this.auth.sessionGeneration;this.loading.set(true);this.error.set('');this.choices.set([]);
  this.injector.get(EmployeeService).getEmployeesPage({page:this.page(),size:20,sortBy:'employeeCode',direction:'asc',keyword:this.query.trim()}).pipe(takeUntilDestroyed(this.destroy)).subscribe({next:r=>{if(revision!==this.revision||generation!==this.auth.sessionGeneration||!this.admin())return;
   if(!r||!Array.isArray(r.content)||!Number.isSafeInteger(r.totalPages)||r.totalPages<0||r.content.some(e=>!Number.isSafeInteger(e.id)||e.id<=0)){this.error.set('Annuaire invalide.');this.loading.set(false);return;}
   this.choices.set(r.content.map(e=>({id:String(e.id),label:[e.firstName,e.lastName].filter(Boolean).join(' ')||e.employeeCode})));this.pages.set(r.totalPages);this.loaded.set(true);this.loading.set(false);},error:e=>{if(revision===this.revision&&generation===this.auth.sessionGeneration){this.loading.set(false);this.error.set(hrError(e));}}});
 }
 protected select(id:string){if(!this.disabled()&&this.admin()&&this.choices().some(c=>c.id===id))this.employeeSelected.emit(id);}
}