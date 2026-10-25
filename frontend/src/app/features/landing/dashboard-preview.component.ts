import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
@Component({ selector: 'app-dashboard-preview', standalone: true, imports: [MatIconModule], templateUrl: './dashboard-preview.component.html', styleUrl: './dashboard-preview.component.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class DashboardPreviewComponent {}
