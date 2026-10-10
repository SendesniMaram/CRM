import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { CrmRole } from '../../../core/models/role.model';
import { AuthService } from '../../../core/services/auth.service';

interface NavigationItem {
  label: string;
  route: string;
  icon: string;
  roles: CrmRole[];
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [MatIconModule, MatListModule, RouterLink, RouterLinkActive],
  template: `
    <mat-nav-list>
      @for (item of visibleItems(); track item.label) {
        @if (item.route) {
        <a mat-list-item [routerLink]="item.route" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">
          <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
          <span matListItemTitle>{{ item.label }}</span>
        </a>
        } @else {
          <p class="unavailable">{{ item.label }} — Non disponible</p>
        }
      }
    </mat-nav-list>
    @if (canAccessHr()) {
      <section aria-labelledby="hr-navigation-title">
        <h3 id="hr-navigation-title">Ressources humaines</h3>
        <mat-nav-list aria-labelledby="hr-navigation-title">
          <a mat-list-item class="hr-child" routerLink="/leaves" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">
            <mat-icon matListItemIcon>event</mat-icon>
            <span matListItemTitle>Congés</span>
          </a>
          @if (canManageAssociations()) {
            <a mat-list-item class="hr-child" routerLink="/leaves" fragment="associations" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">
              <mat-icon matListItemIcon>link</mat-icon>
              <span matListItemTitle>Associations employés</span>
            </a>
          }
        </mat-nav-list>
      </section>
    }
    <button type="button" (click)="logout()">Se déconnecter</button>
  `,
  styles: `
    :host { display: block; padding: 1rem 0.75rem; }
    a.active { background: color-mix(in srgb, var(--mat-sys-primary) 12%, transparent); }
    h3 { margin: 1rem 0.75rem 0; font-size: 0.95rem; }
    .hr-child { padding-left: 1.5rem; }
    .unavailable { margin: 0.75rem; font-size: 0.85rem; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SidebarComponent {
  private readonly authService = inject(AuthService);

  protected logout(): void { this.authService.logout(); }

  protected canAccessHr(): boolean {
    return this.authService.hasAnyRole([CrmRole.SUPER_ADMIN, CrmRole.ADMIN, CrmRole.EMPLOYEE]);
  }

  protected canManageAssociations(): boolean {
    return this.authService.hasAnyRole([CrmRole.SUPER_ADMIN, CrmRole.ADMIN]);
  }

  private readonly navigationItems: NavigationItem[] = [
    { label: 'Tableau de bord', route: '/dashboard', icon: 'dashboard', roles: [] },
    { label: 'Employés', route: '/employees', icon: 'badge', roles: [CrmRole.SUPER_ADMIN, CrmRole.ADMIN] },
    { label: 'Clients', route: '', icon: 'groups', roles: [CrmRole.SUPER_ADMIN, CrmRole.ADMIN] },
    { label: 'Départements', route: '/departments', icon: 'corporate_fare', roles: [CrmRole.SUPER_ADMIN, CrmRole.ADMIN] },
    { label: 'Paie', route: '', icon: 'payments', roles: [CrmRole.SUPER_ADMIN, CrmRole.ADMIN] },
    { label: 'Honoraires et dépenses', route: '', icon: 'receipt_long', roles: [CrmRole.SUPER_ADMIN, CrmRole.ADMIN] },
    { label: 'Factures', route: '', icon: 'description', roles: [CrmRole.SUPER_ADMIN, CrmRole.ADMIN] },
    { label: 'Espace employé', route: '', icon: 'work', roles: [CrmRole.EMPLOYEE] },
    { label: 'Espace client', route: '', icon: 'person', roles: [CrmRole.CLIENT] }
  ];

  protected visibleItems(): NavigationItem[] {
    return this.navigationItems.filter(
      (item) => item.roles.length === 0 || this.authService.hasAnyRole(item.roles)
    );
  }
}
