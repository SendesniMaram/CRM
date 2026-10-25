import { DestroyRef, Injectable, NgZone, inject, signal } from '@angular/core';

export function workspaceGreeting(date: Date): string {
  return date.getHours() >= 18 || date.getHours() < 5 ? 'Bonsoir' : 'Bonjour';
}

export function accountInitials(name: string): string {
  return name.trim().split(/[\s._@-]+/u).filter(Boolean).slice(0, 2)
    .map(part => Array.from(part)[0]).join('').toLocaleUpperCase('fr-FR') || '?';
}

@Injectable({ providedIn: 'root' })
export class WorkspaceClock {
  readonly now = signal(new Date());
  constructor() {
    const destroy = inject(DestroyRef);
    inject(NgZone).runOutsideAngular(() => {
      const timer = setInterval(() => this.now.set(new Date()), 60_000);
      destroy.onDestroy(() => clearInterval(timer));
    });
  }
}
