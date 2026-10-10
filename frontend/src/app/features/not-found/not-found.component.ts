import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatCardModule],
  template: `
    <main>
      <mat-card>
        <mat-card-header><mat-card-title>Page introuvable</mat-card-title></mat-card-header>
        <mat-card-content><p>Cette adresse ne correspond à aucune page disponible.</p></mat-card-content>
        <mat-card-actions><a mat-flat-button routerLink="/dashboard">Retour au tableau de bord</a></mat-card-actions>
      </mat-card>
    </main>
  `,
  styles: `main { display: grid; min-height: 100vh; place-items: center; padding: 2rem; box-sizing: border-box; }`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NotFoundComponent {}
