import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { Router } from '@angular/router';

@Component({
  selector: 'app-access-denied',
  standalone: true,
  imports: [MatButtonModule, MatCardModule],
  template: `
    <main class="access-denied-page">
      <mat-card>
        <mat-card-header>
          <mat-card-title>Accès refusé</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <p>Vous n'avez pas les autorisations nécessaires pour accéder à cette page.</p>
        </mat-card-content>
        <mat-card-actions>
          <button mat-flat-button color="primary" type="button" (click)="goToDashboard()">
            Retour au tableau de bord
          </button>
        </mat-card-actions>
      </mat-card>
    </main>
  `,
  styles: `
    :host { display: block; min-height: 100%; }
    .access-denied-page { display: grid; min-height: 100%; place-items: center; padding: 2rem; box-sizing: border-box; }
    mat-card { width: min(100%, 520px); }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AccessDeniedComponent {
  private readonly router = inject(Router);

  protected goToDashboard(): void {
    void this.router.navigate(['/dashboard']);
  }
}