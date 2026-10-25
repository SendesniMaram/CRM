import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, input, output, viewChild } from '@angular/core';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter, map } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { CrmNavigation } from '../../../core/navigation/crm-navigation';
import { WorkspaceClock, accountInitials } from '../../../core/navigation/workspace-clock';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-navbar', standalone: true,
  imports: [RouterLink, MatToolbarModule, MatIconModule, MatButtonModule, MatMenuModule],
  templateUrl: './navbar.component.html', styleUrl: './navbar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NavbarComponent {
  protected readonly navigation = inject(CrmNavigation);
  protected readonly auth = inject(AuthService);
  protected readonly clock = inject(WorkspaceClock);
  protected readonly initials = computed(() => accountInitials(this.auth.getCurrentUser()?.username ?? ''));
  protected readonly date = computed(() => new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(this.clock.now()));
  private readonly router = inject(Router);
  private readonly url = toSignal(this.router.events.pipe(filter(event => event instanceof NavigationEnd), map(() => this.router.url)), { initialValue: this.router.url });
  protected readonly current = computed(() => {
    const path = this.url().split(/[?#]/)[0];
    return this.navigation.items().find(item => !item.fragment && (path === item.route || path.startsWith(item.route + '/')));
  });
  protected readonly title = computed(() => this.current()?.label ?? 'Votre espace');
  protected readonly detail = computed(() => {
    const base = this.current()?.route;
    if (!base) return null;
    const suffix = this.url().split(/[?#]/)[0].slice(base.length);
    if (suffix === '/new') return 'Création';
    if (/^\/\d+\/edit$/.test(suffix)) return 'Modification';
    if (/^\/\d+$/.test(suffix)) return 'Détails';
    if (base === '/leaves' && this.url().split('#')[1] === 'associations') return 'Associations employés';
    return null;
  });
  readonly mobile = input(false);
  readonly collapsed = input(false);
  readonly navigationOpen = input(false);
  readonly toggleNavigation = output<void>();
  readonly account = output<void>();
  private readonly toggle = viewChild('toggle', { read: ElementRef });
  focusToggle() { this.toggle()?.nativeElement.focus(); }
}
