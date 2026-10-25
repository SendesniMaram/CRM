import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { AuthShellComponent } from '../auth-shell/auth-shell.component';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute, Router } from '@angular/router';
import { safeReturnUrl } from '../../../core/services/auth-navigation';
import { finalize } from 'rxjs';
import { LoginRequest } from '../../../core/models/auth.models';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    AuthShellComponent,
    MatIconModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoginComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly loading = signal(false);
  protected readonly passwordVisible = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly loginForm = this.formBuilder.nonNullable.group({
    identifier: ['', Validators.required],
    password: ['', Validators.required]
  });

  protected submit(): void {
    if (this.loading()) return;
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
      next: () => void this.router.navigateByUrl(safeReturnUrl(this.route.snapshot.queryParamMap.get('returnUrl'))),
      error: () => this.errorMessage.set('Échec de la connexion. Vérifiez vos identifiants et réessayez.')
    });
  }
}
