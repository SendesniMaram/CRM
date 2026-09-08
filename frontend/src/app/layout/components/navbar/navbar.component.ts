import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [MatToolbarModule],
  template: `
    <mat-toolbar color="primary" class="navbar">
      <span>CRM</span>
      <span class="spacer"></span>
      <span class="context">Administration</span>
    </mat-toolbar>
  `,
  styles: `
    .navbar { min-height: 64px; }
    .spacer { flex: 1; }
    .context { font-size: 0.875rem; font-weight: 400; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NavbarComponent {}
