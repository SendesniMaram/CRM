import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../../core/services/auth.service';
import { CrmNavigation } from '../../../core/navigation/crm-navigation';

@Component({
  selector: 'app-account-dialog', standalone: true, imports: [MatDialogModule, MatButtonModule],
  template: `<h2 mat-dialog-title>Mon compte</h2><mat-dialog-content>
    <dl><dt>Nom d’utilisateur</dt><dd>{{ auth.getCurrentUser()?.username }}</dd><dt>E-mail</dt><dd>{{ auth.getCurrentUser()?.email }}</dd><dt>Rôle</dt><dd>{{ navigation.roleLabel() }}</dd></dl>
    <p>Informations de votre session connectée.</p></mat-dialog-content><mat-dialog-actions align="end"><button mat-button mat-dialog-close>Fermer</button></mat-dialog-actions>`,
  styles: `dd { margin: .25rem 0 1rem; overflow-wrap: anywhere; } dt { font-weight: 600; }`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AccountDialogComponent {
  protected readonly auth = inject(AuthService);
  protected readonly navigation = inject(CrmNavigation);
}
