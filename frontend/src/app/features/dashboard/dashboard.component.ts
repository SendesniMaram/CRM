import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [MatCardModule],
  template: `
    <section class="dashboard" aria-labelledby="dashboard-title">
      <h1 id="dashboard-title">Dashboard</h1>
      <mat-card>
        <mat-card-header>
          <mat-card-title>CRM dashboard</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <p>The dashboard workspace is ready for future CRM features.</p>
        </mat-card-content>
      </mat-card>
    </section>
  `,
  styles: `
    .dashboard { max-width: 960px; margin: 0 auto; }
    h1 { margin: 0 0 1.5rem; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent {}
