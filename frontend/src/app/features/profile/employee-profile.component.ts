import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, effect, inject, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { Subscription, finalize } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CrmRole } from '../../core/models/role.model';
import { EmployeeProfile, EmployeeProfileService } from './employee-profile.service';

@Component({
  selector: 'app-employee-profile', standalone: true, imports: [MatButtonModule, MatCardModule],
  templateUrl: './employee-profile.component.html', changeDetection: ChangeDetectionStrategy.OnPush,
  styles: ':host{display:block}mat-card{max-width:48rem;margin:auto}dl{display:grid;grid-template-columns:minmax(8rem,1fr) 2fr;gap:1rem}dd{margin:0;overflow-wrap:anywhere}[role=alert]{color:var(--mat-sys-error)}'
})
export class EmployeeProfileComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly service = inject(EmployeeProfileService);
  private readonly destroy = inject(DestroyRef);
  private generation = this.auth.sessionGeneration;
  private request?: Subscription;
  protected readonly profile = signal<EmployeeProfile | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected readonly missing = signal(false);
  protected employee() { return this.auth.hasAnyRole([CrmRole.EMPLOYEE]); }
  constructor() {
    effect(() => {
      this.auth.getCurrentUser(); const allowed = this.employee(); const generation = this.auth.sessionGeneration;
      untracked(() => {
        if (generation !== this.generation || !allowed) {
          this.request?.unsubscribe(); this.generation = generation;
          this.profile.set(null); this.error.set(''); this.missing.set(false); this.loading.set(false);
          if (allowed) this.load();
        }
      });
    });
  }
  ngOnInit() { this.load(); }
  protected load() {
    if (!this.employee() || this.loading() || this.generation !== this.auth.sessionGeneration) return;
    const generation = this.generation;
    const current = () => generation === this.auth.sessionGeneration && this.employee() && !this.destroy.destroyed;
    this.profile.set(null); this.error.set(''); this.missing.set(false); this.loading.set(true);
    this.request = this.service.read().pipe(takeUntilDestroyed(this.destroy), finalize(() => {
      if (current()) this.loading.set(false);
    })).subscribe({
      next: profile => { if (current()) this.profile.set(profile); },
      error: error => {
        if (!current()) return;
        if (error.status === 404 && error.error?.code === 'PROFILE_NOT_ASSOCIATED') { this.missing.set(true); return; }
        const messages: Record<number, string> = {
          401: 'Votre session a expiré. Veuillez vous reconnecter.',
          403: 'Vous ne disposez pas des autorisations nécessaires.',
          404: 'Votre fiche professionnelle est indisponible. Contactez un administrateur.',
          502: 'La réponse du service est invalide. Réessayez plus tard.',
          503: 'Le service est temporairement indisponible. Réessayez plus tard.'
        };
        this.error.set(messages[error.status] ?? 'Impossible de charger votre profil. Vérifiez la connexion et réessayez.');
      }
    });
  }
}
