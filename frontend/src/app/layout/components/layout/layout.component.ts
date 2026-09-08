import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { NavbarComponent } from '../navbar/navbar.component';
import { SidebarComponent } from '../sidebar/sidebar.component';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [MatSidenavModule, NavbarComponent, RouterOutlet, SidebarComponent],
  template: `
    <app-navbar />
    <mat-sidenav-container class="shell">
      <mat-sidenav mode="side" opened>
        <app-sidebar />
      </mat-sidenav>
      <mat-sidenav-content class="content">
        <router-outlet />
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: `
    :host { display: flex; flex-direction: column; height: 100%; }
    .shell { flex: 1; }
    mat-sidenav { width: 240px; }
    .content { padding: 2rem; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LayoutComponent {}
