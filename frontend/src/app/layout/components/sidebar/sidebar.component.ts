import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [MatIconModule, MatListModule, RouterLink, RouterLinkActive],
  template: `
    <mat-nav-list>
      <a mat-list-item routerLink="/dashboard" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">
        <mat-icon matListItemIcon>dashboard</mat-icon>
        <span matListItemTitle>Dashboard</span>
      </a>
    </mat-nav-list>
  `,
  styles: `
    :host { display: block; padding: 1rem 0.75rem; }
    a.active { background: color-mix(in srgb, var(--mat-sys-primary) 12%, transparent); }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SidebarComponent {}
