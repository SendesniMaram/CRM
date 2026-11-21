import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { timeout } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { AdministrationService, DisplaySettings } from '../users/administration.service';

@Component({
  selector: 'app-workspace-identity', standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './workspace-identity.component.html',
  styleUrl: './workspace-identity.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WorkspaceIdentityComponent {
  private readonly api = inject(AdministrationService);
  private readonly auth = inject(AuthService);
  private readonly destroy = inject(DestroyRef);
  protected readonly value = signal<DisplaySettings | null>(null);
  protected readonly error = signal('');
  protected readonly loading = signal(false);
  constructor() {
    effect(() => {
      this.auth.getCurrentUser();
      untracked(() => { this.value.set(null); this.error.set(''); this.loading.set(false); });
    });
  }
  protected load(): void {
    if (this.loading() || !this.auth.getCurrentUser()) return;
    const generation = this.auth.sessionGeneration;
    this.loading.set(true);
    this.error.set('');
    this.value.set(null);
    this.api.display().pipe(timeout(15000), takeUntilDestroyed(this.destroy)).subscribe({
      next: value => {
        if (generation === this.auth.sessionGeneration && this.auth.getCurrentUser()) {
          this.loading.set(false);
          this.value.set(value);
        }
      },
      error: () => {
        if (generation === this.auth.sessionGeneration && this.auth.getCurrentUser()) {
          this.loading.set(false);
          this.error.set('Les informations de l’entreprise sont indisponibles. Veuillez réessayer.');
        }
      }
    });
  }
}
