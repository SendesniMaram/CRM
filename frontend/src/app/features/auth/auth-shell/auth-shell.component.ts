import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-auth-shell', standalone: true, imports: [MatIconModule],
  template: `
    <main class="auth-shell">
      <aside class="story" aria-label="Votre espace CRM">
        <a class="brand" href="/welcome"><span><mat-icon aria-hidden="true">hub</mat-icon></span>CRM Workspace</a>
        <div class="story-copy"><p class="eyebrow">Un espace. Une vision commune.</p>
          <h1>{{ registration() ? 'Chaque relation commence par une connexion.' : 'Retrouvez le fil de votre activité.' }}</h1>
          <p>{{ registration() ? 'Créez votre compte client et retrouvez un espace adapté à vos autorisations.' : 'Vos équipes, vos clients et vos outils de gestion dans un espace de travail pensé pour vous.' }}</p>
          <div class="illustration" aria-hidden="true"><div class="mini-top"><span></span><span></span><span></span></div><div class="mini-body"><div class="mini-rail"></div><div class="mini-content"><div class="mini-line"></div><div class="mini-cards"><i></i><i></i><i></i></div><div class="mini-chart"><i></i><i></i><i></i><i></i><i></i><i></i><i></i></div></div></div></div>
          <p class="illustration-label">Une interface commune pour vos outils</p>
        </div>
        <p class="story-footer"><mat-icon aria-hidden="true">verified_user</mat-icon>Des fonctionnalités selon votre rôle</p>
      </aside>
      <div class="form-side"><a class="back" href="/welcome"><mat-icon aria-hidden="true">arrow_back</mat-icon>Retour à la présentation</a><div class="form-content"><ng-content /></div><p class="form-footer">CRM Workspace · Votre espace de travail</p></div>
    </main>`,
  styleUrl: './auth-shell.component.scss', changeDetection: ChangeDetectionStrategy.OnPush
})
export class AuthShellComponent {
  readonly registration = input(false);
}
