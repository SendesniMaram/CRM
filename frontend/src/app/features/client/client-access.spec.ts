import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { provideRouter,Router,UrlTree,ActivatedRouteSnapshot,RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { roleGuard } from '../../core/guards/role.guard';
import { routes } from '../../app.routes';
import { firstValueFrom,of,Observable } from 'rxjs';
describe('CLIENT route authorization',()=>{
 for(const role of Object.values(CrmRole)){
  it('keeps personal and administrative paths distinct for '+role,async ()=>{
   TestBed.configureTestingModule({providers:[provideZonelessChangeDetection(),provideRouter([]),
    {provide:AuthService,useValue:{ensureSession:()=>of(true),hasAnyRole:(roles:CrmRole[])=>roles.includes(role)}}]});
   for(const [path,allowed] of [['client',role===CrmRole.CLIENT],['customer-associations',role===CrmRole.ADMIN||role===CrmRole.SUPER_ADMIN]] as const){
    const route=routes.find(r=>r.path==='')!.children!.find(r=>r.path===path)!;
    const result=await firstValueFrom(TestBed.runInInjectionContext(()=>roleGuard({data:route.data} as ActivatedRouteSnapshot,{url:'/'+path} as RouterStateSnapshot)) as Observable<boolean|UrlTree>);
    if(allowed)expect(result).toBeTrue();else expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/access-denied');
   }
  });
 }
});
