import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { firstValueFrom, Observable, of } from 'rxjs';
import { routes } from '../../app.routes';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { roleGuard } from '../../core/guards/role.guard';
import { authGuard } from '../../core/guards/auth.guard';
import { CRM_NAVIGATION } from '../../core/navigation/crm-navigation';
describe('personal document route',()=>{
 for(const role of Object.values(CrmRole))it('matches menu and guard for '+role,async()=>{
  TestBed.configureTestingModule({providers:[provideZonelessChangeDetection(),provideRouter([]),{provide:AuthService,useValue:{ensureSession:()=>of(true),hasAnyRole:(roles:CrmRole[])=>roles.includes(role)}}]});
  const route=routes.find(r=>r.path==='')!.children!.find(r=>r.path==='my-documents')!;
  expect(route.canActivate).toContain(authGuard);expect(route.canActivate).toContain(roleGuard);
  const result=await firstValueFrom(TestBed.runInInjectionContext(()=>roleGuard({data:route.data} as ActivatedRouteSnapshot,{url:'/my-documents'} as RouterStateSnapshot)) as Observable<boolean|UrlTree>);
  expect(result===true).toBe(role===CrmRole.EMPLOYEE);
  expect(CRM_NAVIGATION.find(item=>item.route==='/my-documents')!.roles.includes(role)).toBe(role===CrmRole.EMPLOYEE);
 });
});
