import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { AuthService } from '../../core/services/auth.service';
import { CrmNavigation } from '../../core/navigation/crm-navigation';
import { WorkspaceClock, workspaceGreeting } from '../../core/navigation/workspace-clock';
import { AccountDialogComponent } from '../../layout/components/navbar/account-dialog.component';

@Component({
  selector: 'app-home', standalone: true, imports: [RouterLink, MatButtonModule, MatIconModule],
  templateUrl: './home.component.html', styleUrl: './home.component.scss', changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomeComponent {
  protected readonly auth = inject(AuthService);
  protected readonly navigation = inject(CrmNavigation);
  protected readonly clock = inject(WorkspaceClock);
  private readonly dialog = inject(MatDialog);
  protected readonly greeting = computed(() => workspaceGreeting(this.clock.now()));
  protected readonly dashboard = computed(() => this.navigation.items().find(item => item.route === '/dashboard'));
  protected readonly features = computed(() => this.navigation.items().filter(item => item.route !== '/home' && !item.fragment));
  protected readonly admin = computed(() => this.navigation.modules().some(item => item.route === '/employees'));
  protected readonly leaves = computed(() => this.navigation.modules().find(item => item.route === '/leaves'));
  protected readonly introduction = computed(() => this.admin()
    ? 'Pilotez vos équipes, les ressources humaines, les relations clients et la facturation depuis un espace de gestion centralisé.'
    : this.leaves() ? 'Retrouvez vos demandes de congé et leur suivi personnel dans votre espace collaborateur.'
    : 'Retrouvez les informations de votre compte et les accès disponibles selon vos autorisations.');
  protected description(route: string, fallback: string) {
    if (route === '/leaves' && !this.admin()) return 'Consultez uniquement vos demandes de congé et suivez leur état.';
    if (route === '/dashboard' && !this.admin()) return this.leaves()
      ? 'Retrouvez les indicateurs de vos propres demandes de congé.'
      : 'Consultez votre espace de compte, sans indicateurs administratifs.';
    return fallback;
  }
  protected openAccount() {
    this.dialog.open(AccountDialogComponent, { width: '420px', maxWidth: 'calc(100vw - 2rem)', ariaLabel: 'Informations du compte' });
  }
}
