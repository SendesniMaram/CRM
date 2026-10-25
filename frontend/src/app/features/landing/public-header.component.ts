import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, HostListener, afterNextRender, inject, signal, viewChild } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-public-header', standalone: true, imports: [RouterLink, MatIconModule],
  host: { '[class.scrolled]': 'scrolled()' },
  template: `
    <header class="public-header">
      <a class="brand" routerLink="/welcome" aria-label="CRM Workspace, présentation"><span class="brand-icon"><mat-icon aria-hidden="true">hub</mat-icon></span>CRM<span class="brand-detail">Workspace</span></a>
      <nav class="desktop-nav" aria-label="Navigation de présentation"><a href="/welcome#fonctionnalites" [attr.aria-current]="activeSection() === 'fonctionnalites' ? 'location' : null">Fonctionnalités</a><a href="/welcome#fonctionnement" [attr.aria-current]="activeSection() === 'fonctionnement' ? 'location' : null">Comment ça marche</a><a href="/welcome#questions" [attr.aria-current]="activeSection() === 'questions' ? 'location' : null">FAQ</a></nav>
      <details #menu class="mobile-menu" (keydown.escape)="closeMenu()">
        <summary aria-label="Menu de présentation"><mat-icon aria-hidden="true">menu</mat-icon></summary>
        <nav aria-label="Navigation mobile de présentation"><a href="/welcome#fonctionnalites" [attr.aria-current]="activeSection() === 'fonctionnalites' ? 'location' : null" (click)="closeMenu()">Fonctionnalités</a><a href="/welcome#fonctionnement" [attr.aria-current]="activeSection() === 'fonctionnement' ? 'location' : null" (click)="closeMenu()">Comment ça marche</a><a href="/welcome#questions" [attr.aria-current]="activeSection() === 'questions' ? 'location' : null" (click)="closeMenu()">FAQ</a></nav>
      </details>
      <a class="button" routerLink="/login">Se connecter <mat-icon aria-hidden="true">arrow_forward</mat-icon></a>
    </header>
  `,
  styleUrl: './public-header.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PublicHeaderComponent {
  private readonly menu = viewChild<ElementRef<HTMLDetailsElement>>('menu');
  private readonly document = inject(DOCUMENT);
  private frame: number | undefined;
  protected readonly scrolled = signal(false);
  protected readonly activeSection = signal('');
  constructor() {
    afterNextRender(() => this.updateSection());
    inject(DestroyRef).onDestroy(() => {
      if (this.frame !== undefined) this.document.defaultView?.cancelAnimationFrame(this.frame);
    });
  }
  @HostListener('window:scroll')
  protected updateSection() {
    const view = this.document.defaultView;
    if (!view || this.frame !== undefined) return;
    this.frame = view.requestAnimationFrame(() => {
      this.frame = undefined;
      this.scrolled.set(view.scrollY > 8);
      let active = '';
      for (const id of ['fonctionnalites', 'fonctionnement', 'questions']) {
        if ((this.document.getElementById(id)?.getBoundingClientRect().top ?? Infinity) <= 144) active = id;
      }
      if (view.scrollY > 0 && view.scrollY + view.innerHeight >= this.document.documentElement.scrollHeight - 2) active = 'questions';
      this.activeSection.set(active);
    });
  }
  protected closeMenu() {
    const menu = this.menu()?.nativeElement;
    if (menu) { menu.open = false; menu.querySelector('summary')?.focus(); }
  }
}
