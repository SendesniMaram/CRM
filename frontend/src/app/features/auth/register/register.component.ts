import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { RegisterRequest } from '../../../core/models/register.models';
import { RegistrationService } from '../../../core/services/registration.service';

function matchingPasswords(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;
  return password === confirmPassword ? null : { passwordsMismatch: true };
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    ReactiveFormsModule,
    RouterLink
  ],
  template: `
    <main class="register-page">
      <mat-card class="register-card">
        <mat-card-header>
          <mat-card-title>Créer un compte</mat-card-title>
          <mat-card-subtitle>Rejoignez votre espace CRM</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="registerForm" (ngSubmit)="submit()" novalidate>
            <div class="form-grid">
              <mat-form-field appearance="outline">
                <mat-label>Prénom</mat-label>
                <input matInput formControlName="firstName" autocomplete="given-name" />
                @if (registerForm.controls.firstName.hasError('required')) {
                  <mat-error>Le prénom est obligatoire.</mat-error>
                }
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Nom</mat-label>
                <input matInput formControlName="lastName" autocomplete="family-name" />
                @if (registerForm.controls.lastName.hasError('required')) {
                  <mat-error>Le nom est obligatoire.</mat-error>
                }
              </mat-form-field>
            </div>

            <mat-form-field appearance="outline">
              <mat-label>Nom d'utilisateur</mat-label>
              <input matInput formControlName="username" autocomplete="username" />
              @if (registerForm.controls.username.hasError('required')) {
                <mat-error>Le nom d'utilisateur est obligatoire.</mat-error>
              } @else if (registerForm.controls.username.hasError('minlength')) {
                <mat-error>Le nom d'utilisateur doit contenir au moins 3 caractères.</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Email</mat-label>
              <input matInput type="email" formControlName="email" autocomplete="email" />
              @if (registerForm.controls.email.hasError('required')) {
                <mat-error>L'email est obligatoire.</mat-error>
              } @else if (registerForm.controls.email.hasError('email')) {
                <mat-error>Saisissez une adresse email valide.</mat-error>
              }
            </mat-form-field>

            <div class="form-grid">
              <mat-form-field appearance="outline">
                <mat-label>Mot de passe</mat-label>
                <input matInput [type]="passwordVisible() ? 'text' : 'password'" formControlName="password" autocomplete="new-password" />
                <button mat-icon-button matSuffix type="button" [attr.aria-label]="passwordVisible() ? 'Masquer le mot de passe' : 'Afficher le mot de passe'" (click)="togglePassword()">
                  <mat-icon>{{ passwordVisible() ? 'visibility_off' : 'visibility' }}</mat-icon>
                </button>
                @if (registerForm.controls.password.hasError('required')) {
                  <mat-error>Le mot de passe est obligatoire.</mat-error>
                } @else if (registerForm.controls.password.hasError('minlength')) {
                  <mat-error>Le mot de passe doit contenir au moins 6 caractères.</mat-error>
                }
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Confirmer le mot de passe</mat-label>
                <input matInput [type]="passwordVisible() ? 'text' : 'password'" formControlName="confirmPassword" autocomplete="new-password" />
                @if (registerForm.controls.confirmPassword.hasError('required')) {
                  <mat-error>La confirmation est obligatoire.</mat-error>
                }
              </mat-form-field>
            </div>

            @if (registerForm.hasError('passwordsMismatch') && registerForm.controls.confirmPassword.touched) {
              <p class="field-error" role="alert">Les mots de passe ne correspondent pas.</p>
            }

            @if (errorMessage()) {
              <p class="error" role="alert">{{ errorMessage() }}</p>
            }
            @if (successMessage()) {
              <p class="success" role="status">{{ successMessage() }}</p>
            }

            <button mat-flat-button color="primary" type="submit" [disabled]="loading()">
              {{ loading() ? 'Création en cours...' : 'Créer mon compte' }}
            </button>
          </form>

          <p class="login-link">Déjà un compte ? <a routerLink="/login">Se connecter</a></p>
        </mat-card-content>
      </mat-card>
    </main>
  `,
  styleUrl: './register.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RegisterComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly registrationService = inject(RegistrationService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly passwordVisible = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly registerForm = this.formBuilder.nonNullable.group(
    {
      firstName: ['', [Validators.required, Validators.maxLength(100)]],
      lastName: ['', [Validators.required, Validators.maxLength(100)]],
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(100)]],
      confirmPassword: ['', Validators.required]
    },
    { validators: matchingPasswords }
  );

  protected togglePassword(): void {
    this.passwordVisible.update((visible) => !visible);
  }

  protected submit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    const { firstName, lastName, username, email, password } = this.registerForm.getRawValue();
    const request: RegisterRequest = { firstName, lastName, username, email, password };

    this.loading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.registrationService.register(request).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: () => {
        this.successMessage.set('Votre compte a été créé. Vous pouvez maintenant vous connecter.');
        void this.router.navigate(['/login']);
      },
      error: (error: { status?: number }) => this.errorMessage.set(this.getErrorMessage(error.status))
    });
  }

  private getErrorMessage(status?: number): string {
    if (status === 409) {
      return 'Ce nom d’utilisateur ou cette adresse email existe déjà.';
    }
    if (status === 400) {
      return 'Vérifiez les informations saisies puis réessayez.';
    }
    return 'Le service est momentanément indisponible. Réessayez plus tard.';
  }
}