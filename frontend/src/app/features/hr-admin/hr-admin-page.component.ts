import { ChangeDetectionStrategy,Component,DestroyRef,effect,inject,signal,untracked } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormControl,FormGroup,ReactiveFormsModule,Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule,PageEvent } from '@angular/material/paginator';
import { Subscription } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import { HR_MODULES,HrModule,HrRow,hrError } from './hr-admin.models';
import { HrAdminService } from './hr-admin.service';

@Component({
  selector:'app-hr-admin-page',standalone:true,
  imports:[DatePipe,ReactiveFormsModule,MatButtonModule,MatCardModule,MatCheckboxModule,MatFormFieldModule,MatInputModule,MatSelectModule,MatTableModule,MatPaginatorModule],
  templateUrl:'./hr-admin-page.component.html',styleUrl:'./hr-admin-page.component.scss',
  changeDetection:ChangeDetectionStrategy.OnPush
})
export class HrAdminPageComponent {
  private readonly service=inject(HrAdminService);
  private readonly auth=inject(AuthService);
  private readonly route=inject(ActivatedRoute);
  private readonly dialog=inject(MatDialog);
  private readonly destroy=inject(DestroyRef);
  protected readonly module=this.route.snapshot.data['hrModule'] as HrModule;
  protected readonly definition=HR_MODULES[this.module];
  protected readonly columns=[...this.definition.columns,'actions'];
  protected readonly rows=signal<HrRow[]>([]);
  protected readonly selected=signal<HrRow|null>(null);
  protected readonly editing=signal(false);
  protected readonly viewing=signal(false);
  protected readonly loading=signal(false);
  protected readonly busy=signal(false);
  protected readonly error=signal('');
  protected readonly formError=signal('');
  protected readonly success=signal('');
  protected readonly total=signal(0);
  protected readonly page=signal(0);
  protected readonly search=new FormControl('',{nonNullable:true});
  protected readonly form:FormGroup<Record<string,FormControl>>;
  private requests=new Subscription();
  private listRequest?:Subscription;
  private generation=-1;
  private revision=0;

  constructor() {
    const controls:Record<string,FormControl>={};
    for(const field of this.definition.fields) {
      const validators=[];
      if(field.required)validators.push(Validators.required,Validators.pattern(/\S/));
      if(field.maxLength)validators.push(Validators.maxLength(field.maxLength));
      if(field.type==='number')validators.push(Validators.min(0));
      controls[field.key]=new FormControl('',validators);
    }
    this.form=new FormGroup(controls);
    effect(()=>{
      this.auth.getCurrentUser();const generation=this.auth.sessionGeneration;const allowed=this.admin();
      untracked(()=>{
        if(generation!==this.generation||!allowed){
          this.requests.unsubscribe();this.requests=new Subscription();this.generation=generation;this.revision++;
          this.rows.set([]);this.selected.set(null);this.editing.set(false);this.viewing.set(false);this.form.reset();
          this.error.set('');this.formError.set('');this.success.set('');this.loading.set(false);this.busy.set(false);
          this.total.set(0);this.page.set(0);this.search.reset('');
          if(allowed)this.load();
        }
      });
    });
    this.destroy.onDestroy(()=>this.requests.unsubscribe());
  }
  protected admin(){return this.auth.hasAnyRole([CrmRole.ADMIN,CrmRole.SUPER_ADMIN]);}
  private current(g:number){return g===this.auth.sessionGeneration&&this.admin()&&!this.destroy.destroyed;}
  protected label(key:string){return this.definition.fields.find(field=>field.key===key)?.label??key;}
  protected display(value:unknown){return value===null||value===undefined||value===''?'Non renseigné':String(value);}
  protected readonly statusLabels:Record<string,string>={PLANNED:'Planifiée',IN_PROGRESS:'En cours',COMPLETED:'Terminée',CANCELLED:'Annulée',PRESENT:'Présent',ABSENT:'Absent',LATE:'En retard',HALF_DAY:'Demi-journée',LEAVE:'En congé'};
  protected resetSearch(){this.search.reset('');this.find();}
  protected completed(row:HrRow){return this.module==='trainings'&&row['status']==='COMPLETED';}
  protected load(){
    if(!this.current(this.generation)||this.busy())return;
    this.listRequest?.unsubscribe();const g=this.generation;const revision=++this.revision;
    this.loading.set(true);this.error.set('');
    this.listRequest=this.service.list(this.module,this.page(),this.search.value.trim()).subscribe({
      next:result=>{if(this.current(g)&&revision===this.revision){this.rows.set(result.content);this.total.set(result.totalElements);this.loading.set(false);}},
      error:error=>{if(this.current(g)&&revision===this.revision){this.rows.set([]);this.total.set(0);this.loading.set(false);this.error.set(hrError(error));}}
    });
    this.requests.add(this.listRequest);
  }
  protected find(){if(this.busy())return;this.page.set(0);this.load();}
  protected paginate(e:PageEvent){if(this.busy())return;this.page.set(e.pageIndex);this.load();}
  protected create(){
    if(!this.current(this.generation)||this.busy())return;
    this.selected.set(null);this.viewing.set(false);this.form.reset();this.formError.set('');this.success.set('');
    if(this.module==='trainings')this.form.patchValue({status:'PLANNED',cost:0});
    if(this.module==='attendances')this.form.patchValue({status:'PRESENT'});
    this.editing.set(true);
  }
  protected open(row:HrRow,edit=false){
    if(!this.current(this.generation)||this.busy()||(edit&&this.completed(row)))return;
    const g=this.generation;this.busy.set(true);this.formError.set('');this.error.set('');this.success.set('');
    this.requests.add(this.service.read(this.module,row.id).subscribe({
      next:value=>{if(this.current(g)){
        this.selected.set(value);this.editing.set(edit&&!this.completed(value));this.viewing.set(!edit||this.completed(value));this.form.reset();
        const values:Record<string,unknown>={};for(const field of this.definition.fields)values[field.key]=value[field.key]??'';
        this.form.patchValue(values);this.busy.set(false);
      }},
      error:error=>{if(this.current(g)){this.busy.set(false);this.error.set(hrError(error));}}
    }));
  }
  protected cancel(){if(this.busy())return;this.editing.set(false);this.viewing.set(false);this.selected.set(null);this.form.reset();this.formError.set('');}
  protected save(){
    if(!this.current(this.generation)||this.busy()||!this.editing())return;
    if(this.form.invalid){this.form.markAllAsTouched();this.formError.set('Vérifiez les champs obligatoires et leurs limites.');return;}
    const request:Record<string,unknown>={};
    for(const field of this.definition.fields){
      const raw=this.form.controls[field.key].value;
      request[field.key]=field.type==='number'?Number(raw):field.type==='checkbox'?raw:raw===null||raw===''?null:String(raw).trim();
    }
    if(this.module==='trainings'){
      const start=String(request['startDate']),end=String(request['endDate']);
      const now=new Date();const today=[now.getFullYear(),String(now.getMonth()+1).padStart(2,'0'),String(now.getDate()).padStart(2,'0')].join('-');
      if(start<today||end<start){this.formError.set('Les dates doivent être actuelles ou futures, avec une fin après le début.');return;}
      if(!Number.isFinite(request['cost'] as number)){this.formError.set('Le coût doit être un nombre valide.');return;}
      if(!this.selected()&&request['status']==='COMPLETED'){this.formError.set('Créez la formation avant de la marquer terminée.');return;}
      if(request['certificateObtained']===true&&request['status']!=='COMPLETED'){this.formError.set('Un certificat ne peut être enregistré que pour une formation terminée.');return;}
    }
    const g=this.generation;this.busy.set(true);this.formError.set('');
    this.requests.add(this.service.save(this.module,this.selected()?.id??null,request).subscribe({
      next:()=>{if(this.current(g)){this.busy.set(false);this.editing.set(false);this.selected.set(null);this.form.reset();this.success.set('Enregistrement sauvegardé.');this.load();}},
      error:error=>{if(this.current(g)){this.busy.set(false);this.formError.set(hrError(error));}}
    }));
  }
  protected remove(row:HrRow){
    if(!this.current(this.generation)||this.busy()||this.loading())return;
    const g=this.generation;this.busy.set(true);this.error.set('');this.success.set('');
    this.requests.add(this.dialog.open(ConfirmDialogComponent,{width:'440px',maxWidth:'90vw',
      data:{title:'Confirmer la suppression',message:'Supprimer cet enregistrement ? Cette action est irréversible.',confirmLabel:'Supprimer'}
    }).afterClosed().subscribe(confirmed=>{
      if(!this.current(g))return;
      if(confirmed!==true){this.busy.set(false);return;}
      this.requests.add(this.service.delete(this.module,row.id).subscribe({
        next:()=>{if(this.current(g)){
          this.busy.set(false);this.cancel();this.success.set('Enregistrement supprimé.');
          if(this.rows().length===1&&this.page()>0)this.page.update(p=>p-1);
          this.load();
        }},
        error:error=>{if(this.current(g)){this.busy.set(false);this.error.set(hrError(error));}}
      }));
    }));
  }
}
