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
      @for (item of visibleItems(); track item.route) {
        <a mat-list-item [routerLink]="item.route" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">
          <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
          <span matListItemTitle>{{ item.label }}</span>
        </a>
      }
    </mat-nav-list>
  `,
  styles: `
    :host { display: block; padding: 1rem 0.75rem; }
    a.active { background: color-mix(in srgb, var(--mat-sys-primary) 12%, transparent); }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SidebarComponent {
  private readonly authService = inject(AuthService);

  private readonly navigationItems: NavigationItem[] = [
    { label: 'Dashboard', route: '/dashboard', icon: 'dashboard', roles: [] },
    { label: 'Employees', route: '/employees', icon: 'badge', roles: [CrmRole.SUPER_ADMIN, CrmRole.ADMIN] },
    { label: 'Customers', route: '/customers', icon: 'groups', roles: [CrmRole.SUPER_ADMIN, CrmRole.ADMIN] },
    { label: 'Departments', route: '/departments', icon: 'corporate_fare', roles: [CrmRole.SUPER_ADMIN, CrmRole.ADMIN] },
    { label: 'HR', route: '/hr', icon: 'people', roles: [CrmRole.SUPER_ADMIN, CrmRole.ADMIN] },
    { label: 'Payroll', route: '/payroll', icon: 'payments', roles: [CrmRole.SUPER_ADMIN, CrmRole.ADMIN] },
    { label: 'Fees', route: '/fees', icon: 'receipt_long', roles: [CrmRole.SUPER_ADMIN, CrmRole.ADMIN] },
    { label: 'Invoices', route: '/invoices', icon: 'description', roles: [CrmRole.SUPER_ADMIN, CrmRole.ADMIN] },
    { label: 'Employee area', route: '/employee-area', icon: 'work', roles: [CrmRole.EMPLOYEE] },
    { label: 'Client area', route: '/client-area', icon: 'person', roles: [CrmRole.CLIENT] }
  ];

  protected visibleItems(): NavigationItem[] {
    return this.navigationItems.filter(
      (item) => item.roles.length === 0 || this.authService.hasAnyRole(item.roles)
    );
  }
}
