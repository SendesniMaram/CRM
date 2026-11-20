import { ChangeDetectionStrategy, Component, OnDestroy, inject } from '@angular/core';
import { DOCUMENT, ViewportScroller } from '@angular/common';
import { RouterLink } from '@angular/router';
import { WelcomeHeroComponent } from './welcome-hero.component';
import { PublicHeaderComponent } from './public-header.component';
import { PublicFooterComponent } from './public-footer.component';
import { LandingFaqComponent } from './landing-faq.component';
import { MatIconModule } from '@angular/material/icon';
import { CrmNavigation } from '../../core/navigation/crm-navigation';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-landing', standalone: true, imports: [RouterLink, MatIconModule, PublicHeaderComponent, WelcomeHeroComponent, LandingFaqComponent, PublicFooterComponent],
  templateUrl: './landing.component.html', styleUrl: './landing.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LandingComponent implements OnDestroy {
  private readonly viewportScroller = inject(ViewportScroller);
  private readonly document = inject(DOCUMENT);
  private readonly navigation = inject(CrmNavigation);
  private readonly auth = inject(AuthService);
  constructor() {
    this.viewportScroller.setOffset([0, 104]);
    this.document.documentElement.classList.add('crm-landing');
  }
  ngOnDestroy() {
    this.viewportScroller.setOffset([0, 0]);
    this.document.documentElement.classList.remove('crm-landing');
  }
  protected destination(route: string) {
    if (!this.auth.isAuthenticated()) return '/login';
    return this.navigation.items().some(item => item.route === route) ? route : '/home';
  }
  protected cardAction(route: string) {
    return this.destination(route) === '/login' ? 'Se connecter' : this.destination(route) === '/home' ? 'Voir mon espace' : 'Explorer';
  }
  protected readonly modules = [
    { route: '/employees', icon: 'badge', name: 'Employés', text: 'Les informations de vos collaborateurs, réunies au même endroit.' },
    { route: '/departments', icon: 'corporate_fare', name: 'Départements', text: 'Une organisation lisible pour structurer vos équipes.' },
    { route: '/leaves', icon: 'event_note', name: 'Congés', text: 'Des demandes aux validations, un suivi adapté à chaque rôle.' },
    { route: '/customers', icon: 'groups', name: 'Clients', text: 'Un répertoire partagé pour mieux suivre vos relations clients.' },
    { route: '/invoices', icon: 'description', name: 'Factures', text: 'Les montants, échéances et statuts de vos factures, en un regard.' },
    { route: '/fees', icon: 'receipt_long', name: 'Honoraires', text: 'Les prestations, heures et honoraires pour garder le fil de votre activité.' },
    { route: '/payroll', icon: 'payments', name: 'Paie', text: 'Les éléments de rémunération et fiches de paie dans un espace dédié.' }
  ];

}
