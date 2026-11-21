import { provideZonelessChangeDetection } from '@angular/core';import { TestBed } from '@angular/core/testing';import { provideRouter,Router } from '@angular/router';import { provideHttpClient } from '@angular/common/http';import { provideHttpClientTesting,HttpTestingController } from '@angular/common/http/testing';import { signal } from '@angular/core';import { PasswordComponent } from './password.component';import { AuthService } from '../../../core/services/auth.service';
describe('Password lifecycle screen',()=>{let auth:any,http:HttpTestingController;beforeEach(()=>{auth={sessionGeneration:0,clearSession:jasmine.createSpy('clear'),sessionNotice:signal('')};TestBed.configureTestingModule({imports:[PasswordComponent],providers:[provideZonelessChangeDetection(),provideRouter([]),provideHttpClient(),provideHttpClientTesting(),{provide:AuthService,useValue:auth}]});http=TestBed.inject(HttpTestingController);spyOn(TestBed.inject(Router),'navigate').and.resolveTo(true);});afterEach(()=>http.verify());function setup(){const f=TestBed.createComponent(PasswordComponent);f.detectChanges();const c=f.componentInstance;c['login']='worker';c['current']='temporary-password';c['next']='new-password';c['repeat']='new-password';return{f,c};}
it('changes using current credentials and clears secrets before reconnecting',()=>{const{c}=setup();c['submit']();const r=http.expectOne('http://localhost:8080/identity/api/auth/password');expect(r.request.body).toEqual({login:'worker',currentPassword:'temporary-password',newPassword:'new-password'});r.flush(null,{status:204,statusText:'No Content'});expect(auth.clearSession).toHaveBeenCalled();expect(c['current']).toBe('');expect(c['next']).toBe('');expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/login']);});
it('rejects mismatched confirmation and UTF-8 values exceeding BCrypt limits',()=>{const{c}=setup();c['repeat']='different';c['submit']();expect(c['error']()).not.toBe('');c['next']='é'.repeat(40);c['repeat']=c['next'];c['submit']();http.expectNone('http://localhost:8080/identity/api/auth/password');});
it('does not announce success after a rejected change',()=>{const{c}=setup();c['submit']();http.expectOne('http://localhost:8080/identity/api/auth/password').flush({}, {status:401,statusText:'Unauthorized'});expect(c['error']()).toContain('refusés');expect(auth.clearSession).not.toHaveBeenCalled();expect(c['next']).toBe('');});
it('ignores a late successful response belonging to an old session',()=>{const{c}=setup();c['submit']();auth.sessionGeneration++;http.expectOne('http://localhost:8080/identity/api/auth/password').flush(null,{status:204,statusText:'No Content'});expect(auth.clearSession).not.toHaveBeenCalled();expect(TestBed.inject(Router).navigate).not.toHaveBeenCalled();});

it('renders the shared auth shell, card, associated labels and password guidance',async()=>{
 const {f}=setup();await f.whenStable();f.detectChanges();const el=f.nativeElement as HTMLElement;
 expect(el.querySelector('app-auth-shell mat-card')).not.toBeNull();
 for(const id of ['password-current','password-next','password-repeat']){
  expect(el.querySelector('label[for="'+id+'"]')).not.toBeNull();
  expect(el.querySelector<HTMLInputElement>('#'+id)!.type).toBe('password');
 }
 expect(el.querySelector('#password-rules')!.textContent).toContain('72 octets UTF-8');
 expect(el.querySelector('#password-rules')!.textContent).toContain('6 caract');
});
it('toggles each password independently with accessible non-submit buttons',async()=>{
 const {f}=setup();await f.whenStable();f.detectChanges();const el=f.nativeElement as HTMLElement;
 for(const id of ['password-current','password-next','password-repeat']){
  const button=el.querySelector<HTMLButtonElement>('button[aria-controls="'+id+'"]')!;
  expect(button.type).toBe('button');expect(button.getAttribute('aria-pressed')).toBe('false');
  button.click();f.detectChanges();expect(el.querySelector<HTMLInputElement>('#'+id)!.type).toBe('text');
  expect(button.getAttribute('aria-label')).toContain('Masquer');
  button.click();f.detectChanges();expect(el.querySelector<HTMLInputElement>('#'+id)!.type).toBe('password');
 }
 http.expectNone('http://localhost:8080/identity/api/auth/password');
});
it('submits the rendered form once, shows loading, then retains the existing success notice',async()=>{
 const f=TestBed.createComponent(PasswordComponent);f.detectChanges();await f.whenStable();
 const el=f.nativeElement as HTMLElement;
 for(const [name,value] of Object.entries({login:'worker',current:'temporary-password',next:'new-password',repeat:'new-password'})){
  const input=el.querySelector<HTMLInputElement>('input[name="'+name+'"]')!;input.value=value;input.dispatchEvent(new Event('input'));
 }
 await f.whenStable();f.detectChanges();el.querySelector('form')!.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));f.detectChanges();
 const req=http.expectOne('http://localhost:8080/identity/api/auth/password');
 expect(req.request.body).toEqual({login:'worker',currentPassword:'temporary-password',newPassword:'new-password'});
 expect(el.querySelector<HTMLFieldSetElement>('fieldset')!.disabled).toBeTrue();
 expect(el.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBeTrue();
 expect(el.querySelector('form')!.getAttribute('aria-busy')).toBe('true');
 expect(el.querySelector('[role="status"]')!.textContent).toContain('en cours');
 f.componentInstance['submit']();http.expectNone('http://localhost:8080/identity/api/auth/password');
 req.flush(null,{status:204,statusText:'No Content'});f.detectChanges();await f.whenStable();f.detectChanges();
 expect(auth.sessionNotice()).toContain('Reconnectez-vous');expect(auth.clearSession).toHaveBeenCalledTimes(1);
 expect(el.querySelector<HTMLInputElement>('#password-next')!.value).toBe('');
});
it('shows confirmation feedback and makes no request for invalid submitted input',async()=>{
 const {f,c}=setup();c['repeat']='mismatch';await f.whenStable();f.detectChanges();
 const el=f.nativeElement as HTMLElement;el.querySelector('form')!.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));f.detectChanges();
 expect(el.querySelector('#confirmation-feedback')!.textContent).toContain('ne correspondent pas');
 expect(el.querySelector('[role="alert"]')).not.toBeNull();http.expectNone('http://localhost:8080/identity/api/auth/password');
});
it('explains UTF-8 overflow and unchanged passwords without weakening submission guards',async()=>{
 const {f,c}=setup();c['next']='\u00e9'.repeat(37);c['repeat']=c['next'];await f.whenStable();f.detectChanges();
 const form=f.nativeElement.querySelector('form') as HTMLFormElement;form.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));f.detectChanges();
 expect(f.nativeElement.querySelector('#new-password-feedback').textContent).toContain('72 octets');
 c['next']=c['current'];c['repeat']=c['next'];f.detectChanges();form.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));f.detectChanges();
 expect(f.nativeElement.querySelector('#new-password-feedback').textContent).toContain('différent');
 http.expectNone('http://localhost:8080/identity/api/auth/password');
});
it('remasks all secrets and announces a rejected request without a success notice',async()=>{
 const {f,c}=setup();c['currentVisible'].set(true);c['nextVisible'].set(true);c['repeatVisible'].set(true);c['submit']();
 http.expectOne('http://localhost:8080/identity/api/auth/password').flush({}, {status:400,statusText:'Bad Request'});
 await f.whenStable();f.detectChanges();
 expect(f.nativeElement.querySelector('[role="alert"]').textContent).toContain('invalide');
 expect(c['currentVisible']()).toBeFalse();expect(c['nextVisible']()).toBeFalse();expect(c['repeatVisible']()).toBeFalse();
 expect(auth.sessionNotice()).toBe('');expect(auth.clearSession).not.toHaveBeenCalled();
});
});
