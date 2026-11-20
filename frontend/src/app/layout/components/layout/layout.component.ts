import { ChangeDetectionStrategy, Component, ElementRef, computed, effect, inject, signal, viewChild } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { BreakpointObserver } from '@angular/cdk/layout';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatDialog } from '@angular/material/dialog';
import { NavbarComponent } from '../navbar/navbar.component';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { AccountDialogComponent } from '../navbar/account-dialog.component';

@Component({
  selector: 'app-layout', standalone: true,
  imports: [MatSidenavModule, NavbarComponent, RouterOutlet, SidebarComponent],
  template: `
    <a class="skip-link" href="#crm-content" (click)="skipToContent($event)">Aller au contenu</a>
    <app-navbar [mobile]="mobile()" [navigationOpen]="mobileOpen()" [collapsed]="collapsed()" (toggleNavigation)="toggleNavigation()" (account)="openAccount()" />
    <mat-sidenav-container class="shell" autosize>
      <mat-sidenav id="crm-drawer" [mode]="mobile() ? 'over' : 'side'" [opened]="mobile() ? mobileOpen() : true"
        [autoFocus]="mobile() ? 'first-tabbable' : false" [class.compact]="!mobile() && collapsed()"
        (closed)="drawerClosed()">
        <app-sidebar [collapsed]="!mobile() && collapsed()" [mobile]="mobile()" (toggleCollapse)="collapsed.set(!collapsed())" (navigate)="closeMobile()" (account)="openAccount()" />
      </mat-sidenav>
      <mat-sidenav-content><main #content id="crm-content" tabindex="-1" class="content"><router-outlet /></main></mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: `:host { display: flex; flex-direction: column; height: 100%; }.shell { flex: 1; min-height: 0; background: var(--mat-sys-surface); }mat-sidenav { width: 264px; border: 0; border-radius: 0; }mat-sidenav.compact { width: 80px; }.content { padding: clamp(1rem, 2.5vw, 2rem); min-height: calc(100% - 4rem); }.skip-link { position: fixed; top: -100px; left: 1rem; z-index: 2000; padding: .75rem; background: var(--mat-sys-primary); color: var(--mat-sys-on-primary); }.skip-link:focus { top: .5rem; }@media(max-width: 767px) { mat-sidenav { width: min(85vw, 290px); }.content { padding: 1rem; } }@media(prefers-reduced-motion: reduce) { mat-sidenav, mat-sidenav-content { transition: none !important; } }`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LayoutComponent {
  private readonly breakpoint = toSignal(inject(BreakpointObserver).observe('(max-width: 767px)'), { initialValue: { matches: false, breakpoints: {} } });
  protected readonly mobile = computed(() => this.breakpoint().matches);
  protected readonly collapsed = signal(false);
  protected readonly mobileOpen = signal(false);
  private readonly navbar = viewChild(NavbarComponent);
  private readonly content = viewChild<ElementRef<HTMLElement>>('content');
  private readonly dialog = inject(MatDialog);
  private accountPending = false;
  constructor() { effect(() => { this.mobile(); this.mobileOpen.set(false); }); }
  protected skipToContent(event: Event) {
    event.preventDefault();
    this.content()?.nativeElement.focus();
  }
  protected toggleNavigation() { if (this.mobile()) this.mobileOpen.set(!this.mobileOpen()); else this.collapsed.set(!this.collapsed()); }
  protected closeMobile() { if (this.mobile()) this.mobileOpen.set(false); }
  protected drawerClosed() {
    if (this.mobile()) {
      this.mobileOpen.set(false);
      if (this.accountPending) { this.accountPending = false; this.showAccount(); }
      else queueMicrotask(() => this.navbar()?.focusToggle());
    }
  }
  protected openAccount() {
    if (this.mobile() && this.mobileOpen()) { this.accountPending = true; this.closeMobile(); return; }
    this.showAccount();
  }
  private showAccount() {
    this.dialog.open(AccountDialogComponent, { width: '420px', maxWidth: 'calc(100vw - 2rem)', ariaLabel: 'Informations du compte' });
  }
}
