import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { ActivatedRouteSnapshot,RouterStateSnapshot,Router,UrlTree,provideRouter } from '@angular/router';
import { Observable,of,firstValueFrom } from 'rxjs';
import { routes } from '../../app.routes';
import { roleGuard } from '../../core/guards/role.guard';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { CrmNavigation } from '../../core/navigation/crm-navigation';
import { safeReturnUrl } from '../../core/services/auth-navigation';
describe('HR administration navigation permissions',()=>{
  for(const role of Object.values(CrmRole)){
    it('enforces all HR administration routes for '+role,async()=>{
      TestBed.configureTestingModule({providers:[provideZonelessChangeDetection(),provideRouter([]),
        {provide:AuthService,useValue:{ensureSession:()=>of(true),hasAnyRole:(roles:CrmRole[])=>roles.includes(role)}}]});
      const allowed=role===CrmRole.ADMIN||role===CrmRole.SUPER_ADMIN;
      for(const path of ['trainings','attendances','hr-records','contracts','evaluations','job-offers','applications']){
        const route=routes.find(r=>r.path==='')!.children!.find(r=>r.path===path)!;
        const result=await firstValueFrom(TestBed.runInInjectionContext(()=>roleGuard({data:route.data} as ActivatedRouteSnapshot,{url:'/'+path} as RouterStateSnapshot)) as Observable<boolean|UrlTree>);
        if(allowed)expect(result).toBeTrue();else expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/access-denied');
        expect(TestBed.inject(CrmNavigation).items().some(item=>item.route==='/'+path)).toBe(allowed);
        expect(safeReturnUrl('/'+path)).toBe('/'+path);
      }
    });
  }
});
