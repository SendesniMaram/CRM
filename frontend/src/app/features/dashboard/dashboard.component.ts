import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { DASHBOARD_STATS, DASHBOARD_SUMMARY, RECENT_ACTIVITIES } from './dashboard.mock';
import { DashboardSummary, DashboardStat, RecentActivity } from './dashboard.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [MatCardModule, MatChipsModule, MatDividerModule, MatIconModule, MatTableModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent {
  protected readonly stats: DashboardStat[] = DASHBOARD_STATS;
  protected readonly activities: RecentActivity[] = RECENT_ACTIVITIES;
  protected readonly summary: DashboardSummary[] = DASHBOARD_SUMMARY;
  protected readonly activityColumns = ['type', 'description', 'user', 'date', 'status'];
}
