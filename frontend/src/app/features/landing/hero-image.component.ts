import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-hero-image',
  standalone: true,
  template: `
    <div class="visual">
      <img src="images/crm-workspace-960.webp"
        srcset="images/crm-workspace-640.webp 640w, images/crm-workspace-960.webp 960w, images/crm-workspace-1440.webp 1440w"
        sizes="100vw"
        width="1440" height="810" loading="eager" fetchpriority="high" decoding="async"
        alt="" aria-hidden="true" />
    </div>
  `,
  styleUrl: './hero-image.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HeroImageComponent {}
