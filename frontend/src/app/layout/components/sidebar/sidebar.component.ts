import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { IsActiveMatchOptions, RouterLink, RouterLinkActive } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../../core/services/auth.service';
import { CrmNavigation } from '../../../core/navigation/crm-navigation';

@Component({
  selector: 'app-sidebar', standalone: true,
  imports: [MatIconModule, MatButtonModule, MatTooltipModule, RouterLink, RouterLinkActive],
  template: `
    <div class="sidebar" [class.compact]="collapsed()">
      <div class="brand"><span class="brand-mark" aria-hidden="true"><mat-icon>hub</mat-icon></span>
        @if (!collapsed()) { <div><strong>CRM</strong><span>Espace de travail</span></div> }
      </div>
      @if (!mobile()) { <button mat-icon-button class="collapse-toggle" (click)="toggleCollapse.emit()"
        [attr.aria-label]="collapsed() ? 'Développer la navigation' : 'Réduire la navigation'" [attr.aria-expanded]="!collapsed()" aria-controls="crm-navigation">
        <mat-icon>{{ collapsed() ? 'chevron_right' : 'chevron_left' }}</mat-icon></button> }
      <nav id="crm-navigation" aria-label="Navigation principale">
        @for (group of navigation.groups(); track group.label) {
          <section [attr.aria-label]="group.label"><h3 [attr.id]="group.label === 'Ressources humaines' ? 'hr-navigation-title' : null" [class.visually-hidden]="collapsed()">{{ group.label }}</h3>
            @for (item of group.items; track item.label) {
              <a [routerLink]="item.route" [fragment]="item.fragment" routerLinkActive="active" ariaCurrentWhenActive="page"
                [routerLinkActiveOptions]="item.fragment || item.route === '/leaves' ? fragmentMatch : moduleMatch" (click)="navigate.emit()"
                [attr.aria-label]="item.label" [matTooltip]="item.label" [matTooltipDisabled]="!collapsed()" matTooltipPosition="right">
                <mat-icon aria-hidden="true">{{ item.icon }}</mat-icon><span [class.visually-hidden]="collapsed()">{{ item.label }}</span>
              </a>
            }
          </section>
        }
      </nav>
      <div class="account-actions">
        <button mat-button (click)="account.emit()" aria-label="Mon compte" matTooltip="Mon compte" [matTooltipDisabled]="!collapsed()" matTooltipPosition="right">
          <mat-icon>account_circle</mat-icon><span [class.visually-hidden]="collapsed()">Mon compte</span></button>
        <button mat-button class="logout" (click)="logout()" aria-label="Se déconnecter" matTooltip="Se déconnecter" [matTooltipDisabled]="!collapsed()" matTooltipPosition="right">
          <mat-icon>logout</mat-icon><span [class.visually-hidden]="collapsed()">Se déconnecter</span></button>
      </div>
    </div>
  `,
  styles: `
    @use '../../../../styles/workspace' as ui;
    :host { display: block; height: 100%; }
    .sidebar { min-height: 100%; display: flex; flex-direction: column; padding: 1.25rem .85rem; box-sizing: border-box; background: ui.$ink; font-family: ui.$font; color: #e7eff7; }
    .brand { display: flex; align-items: center; gap: .8rem; padding: .25rem .5rem 1.5rem; }
    .brand-mark { display: grid; place-items: center; background: #2a666b; color: #bdfff1; width: 38px; height: 38px; border-radius: 12px; flex-shrink: 0; }
    .brand strong { display: block; font-size: 1.25rem; letter-spacing: .08em; }.brand span:not(.brand-mark) { font-size: .72rem; color: #b3c9db; }
    .collapse-toggle { align-self: flex-end; color: #d2e7f0; margin-bottom: .5rem; }
    nav { flex: 1; } section { margin-bottom: 1.2rem; } h3 { color: #a5c2d2; text-transform: uppercase; letter-spacing: .08em; font-size: .65rem; line-height: 1.5; padding: 0 .7rem; }
    a { display: flex; gap: .8rem; align-items: center; padding: .8rem .7rem; margin: .2rem 0; border-radius: 10px; text-decoration: none; color: #d9e5f0; font-size: .87rem; border-left: 3px solid transparent; transition: background 140ms ease; }
    a:hover { background: #ffffff12; } a.active { background: #24666d; color: #fff; border-left-color: #80ead6; font-weight: 600; }
    a mat-icon { flex-shrink: 0; font-size: 21px; width: 21px; height: 21px; }
    .account-actions { display: flex; flex-direction: column; align-items: stretch; gap: .25rem; border-top: 1px solid #ffffff24; padding-top: 1rem; }
    .account-actions button { color: #d9e5f0; justify-content: flex-start; min-height: 44px; }.account-actions .logout { color: #ffc9c4; }
    .compact { padding: 1rem .5rem; }.compact .brand { padding: .25rem 0 1rem; justify-content: center; }.compact a { justify-content: center; padding: .8rem 0; }.compact .account-actions button { min-width: 0; padding: 0; justify-content: center; }.compact .account-actions mat-icon { margin: 0; }
    .visually-hidden { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip-path: inset(50%); white-space: nowrap; }
    a:focus-visible, button:focus-visible { outline: 2px solid #a6f4e7; outline-offset: 2px; }
    @media (prefers-reduced-motion: reduce) { a { transition: none; } }
  `, changeDetection: ChangeDetectionStrategy.OnPush
})
export class SidebarComponent {
  protected readonly navigation = inject(CrmNavigation);
  private readonly auth = inject(AuthService);
  readonly collapsed = input(false);
  readonly mobile = input(false);
  readonly toggleCollapse = output<void>();
  readonly navigate = output<void>();
  readonly account = output<void>();
  protected readonly moduleMatch: IsActiveMatchOptions = { paths: 'subset', queryParams: 'ignored', matrixParams: 'ignored', fragment: 'ignored' };
  protected readonly fragmentMatch: IsActiveMatchOptions = { ...this.moduleMatch, fragment: 'exact' };
  protected logout() { this.navigate.emit(); this.auth.logout(); }
}
