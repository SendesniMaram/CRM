import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { HeroImageComponent } from './hero-image.component';
import { DashboardPreviewComponent } from './dashboard-preview.component';

@Component({
  selector: 'app-welcome-hero', standalone: true,
  imports: [RouterLink, MatIconModule, HeroImageComponent, DashboardPreviewComponent],
  templateUrl: './welcome-hero.component.html',
  styleUrl: './welcome-hero.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WelcomeHeroComponent {}
