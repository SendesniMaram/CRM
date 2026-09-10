import { provideHttpClient } from '@angular/common/http';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Observable, of, Subject, throwError } from 'rxjs';
import { RegistrationService } from '../../../core/services/registration.service';
import { RegisterComponent } from './register.component';

describe('RegisterComponent', () => {
  let registrationService: jasmine.SpyObj<RegistrationService>;
  let router: Router;

  beforeEach(async () => {
    registrationService = jasmine.createSpyObj<RegistrationService>('RegistrationService', ['register']);

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        provideZonelessChangeDetection(),
        { provide: RegistrationService, useValue: registrationService }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
  });

  function createFixture() {
    const fixture = TestBed.createComponent(RegisterComponent);
    fixture.detectChanges();
    return fixture;
  }

  function fillValidForm(fixture: ReturnType<typeof createFixture>): void {
    fixture.componentInstance['registerForm'].setValue({
      firstName: 'Ada',
      lastName: 'Lovelace',
      username: 'ada-user',
      email: 'ada@example.test',
      password: 'synthetic-password',
      confirmPassword: 'synthetic-password'
    });
  }

  it('creates and displays the registration form', () => {
    const fixture = createFixture();

    expect(fixture.componentInstance).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('Créer un compte');
    expect(fixture.nativeElement.querySelector('form')).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('Déjà un compte ?');
  });

  it('validates required fields when the form is empty', () => {
    const fixture = createFixture();

    fixture.componentInstance['submit']();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Le prénom est obligatoire.');
    expect(fixture.nativeElement.textContent).toContain("L'email est obligatoire.");
    expect(fixture.nativeElement.textContent).toContain('Le mot de passe est obligatoire.');
    expect(registrationService.register).not.toHaveBeenCalled();
  });

  it('validates email format and minimum password length', () => {
    const fixture = createFixture();
    fixture.componentInstance['registerForm'].patchValue({
      firstName: 'Ada',
      lastName: 'Lovelace',
      username: 'ada-user',
      email: 'invalid-email',
      password: 'short',
      confirmPassword: 'short'
    });

    fixture.componentInstance['submit']();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Saisissez une adresse email valide.');
    expect(fixture.nativeElement.textContent).toContain('Le mot de passe doit contenir au moins 6 caractères.');
    expect(registrationService.register).not.toHaveBeenCalled();
  });

  it('detects different passwords', () => {
    const fixture = createFixture();
    fixture.componentInstance['registerForm'].patchValue({
      firstName: 'Ada',
      lastName: 'Lovelace',
      username: 'ada-user',
      email: 'ada@example.test',
      password: 'synthetic-password',
      confirmPassword: 'different-password'
    });

    fixture.componentInstance['submit']();
    fixture.componentInstance['registerForm'].controls.confirmPassword.markAsTouched();
    fixture.detectChanges();

    expect(fixture.componentInstance['registerForm'].hasError('passwordsMismatch')).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Les mots de passe ne correspondent pas.');
    expect(registrationService.register).not.toHaveBeenCalled();
  });

  it('sends the backend payload without confirmation or role fields', () => {
    registrationService.register.and.returnValue(of(void 0));
    const fixture = createFixture();
    fillValidForm(fixture);

    fixture.componentInstance['submit']();

    expect(registrationService.register).toHaveBeenCalledWith({
      firstName: 'Ada',
      lastName: 'Lovelace',
      username: 'ada-user',
      email: 'ada@example.test',
      password: 'synthetic-password'
    });
    const payload = registrationService.register.calls.mostRecent().args[0] as unknown as Record<string, unknown>;
    expect(payload['confirmPassword']).toBeUndefined();
    expect(payload['roleType']).toBeUndefined();
    expect(payload['role']).toBeUndefined();
    expect(payload['roles']).toBeUndefined();
  });

  it('shows success and navigates to login after registration', () => {
    registrationService.register.and.returnValue(of(void 0));
    const fixture = createFixture();
    fillValidForm(fixture);

    fixture.componentInstance['submit']();
    fixture.detectChanges();

    expect(fixture.componentInstance['successMessage']()).toContain('Votre compte a été créé');
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('shows a duplicate-account error', () => {
    registrationService.register.and.returnValue(throwError(() => ({ status: 409 })));
    const fixture = createFixture();
    fillValidForm(fixture);

    fixture.componentInstance['submit']();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('existe déjà');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('shows a generic error for an unavailable server', () => {
    registrationService.register.and.returnValue(throwError(() => ({ status: 503 })));
    const fixture = createFixture();
    fillValidForm(fixture);

    fixture.componentInstance['submit']();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('service est momentanément indisponible');
  });

  it('keeps the submit button disabled while registration is pending', () => {
    const pendingRegistration = new Subject<void>();
    registrationService.register.and.returnValue(pendingRegistration.asObservable());
    const fixture = createFixture();
    fillValidForm(fixture);

    fixture.componentInstance['submit']();
    fixture.detectChanges();

    expect(fixture.componentInstance['loading']()).toBeTrue();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBeTrue();

    pendingRegistration.complete();
    fixture.detectChanges();

    expect(fixture.componentInstance['loading']()).toBeFalse();
  });
});