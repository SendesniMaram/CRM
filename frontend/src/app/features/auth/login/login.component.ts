import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { LoginRequest } from '../../../core/models/auth.models';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule
  ],
  template: `
    <main class="login-page">
      <mat-card class="login-card">
        <mat-card-header>
          <mat-card-title>Connexion CRM</mat-card-title>
          <mat-card-subtitle>Connectez-vous pour continuer</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="loginForm" (ngSubmit)="submit()">
            <mat-form-field appearance="outline">
              <mat-label>Nom d'utilisateur ou e-mail</mat-label>
              <input matInput formControlName="identifier" autocomplete="username" />
              @if (loginForm.controls.identifier.hasError('required')) {
                <mat-error>Le nom d'utilisateur ou l'e-mail est obligatoire.</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Mot de passe</mat-label>
              <input matInput type="password" formControlName="password" autocomplete="current-password" />
              @if (loginForm.controls.password.hasError('required')) {
                <mat-error>Le mot de passe est obligatoire.</mat-error>
              }
            </mat-form-field>

            @if (errorMessage()) {
              <p class="error" role="alert">{{ errorMessage() }}</p>
            }

            <button mat-flat-button color="primary" type="submit" [disabled]="loading()">
              {{ loading() ? 'Connexion en cours...' : 'Se connecter' }}
            </button>
          </form>
        </mat-card-content>
      </mat-card>
    </main>
  `,
  styles: `
    :host { display: block; min-height: 100%; }
    .login-page { display: grid; min-height: 100%; place-items: center; padding: 2rem; box-sizing: border-box; }
    .login-card { width: min(100%, 420px); }
    form { display: grid; gap: 1rem; margin-top: 1.5rem; }
    .error { color: var(--mat-sys-error); margin: 0; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoginComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly loginForm = this.formBuilder.nonNullable.group({
    identifier: ['', Validators.required],
    password: ['', Validators.required]
  });

  protected submit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    const { identifier, password } = this.loginForm.getRawValue();
    const request: LoginRequest = {
      username: identifier.includes('@') ? null : identifier,
      email: identifier.includes('@') ? identifier : null,
      password
    };

    this.loading.set(true);
    this.errorMessage.set('');

    this.authService.login(request).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: () => void this.router.navigate(['/dashboard']),
      error: () => this.errorMessage.set('Échec de la connexion. Vérifiez vos identifiants et réessayez.')
    });
  }
}
