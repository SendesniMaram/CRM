import { Injectable, inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { CrmRole } from '../models/role.model';

export interface CrmNavigationItem {
  label: string; route: string; icon: string; description: string;
  category: 'Général' | 'Organisation' | 'Ressources humaines' | 'Relation client et facturation';
  roles: readonly CrmRole[]; fragment?: string; action?: { label: string; route: string; roles: readonly CrmRole[] };
}
const admins = [CrmRole.SUPER_ADMIN, CrmRole.ADMIN];
const everyone = [...admins, CrmRole.EMPLOYEE, CrmRole.CLIENT];
export const CRM_NAVIGATION: readonly CrmNavigationItem[] = [
  { label: 'Accueil', route: '/home', icon: 'home', description: 'Votre point de départ.', category: 'Général', roles: everyone },
  { label: 'Tableau de bord', route: '/dashboard', icon: 'dashboard', description: 'Consultez les indicateurs accessibles à votre rôle.', category: 'Général', roles: everyone },
  { label: 'Employés', route: '/employees', icon: 'badge', description: 'Centralisez les informations de vos collaborateurs et facilitez le suivi administratif.', category: 'Organisation', roles: admins,
    action: { label: 'Ajouter un employé', route: '/employees/new', roles: admins } },
  { label: 'Départements', route: '/departments', icon: 'corporate_fare', description: 'Organisez les équipes et structurez les départements de votre entreprise.', category: 'Organisation', roles: admins,
    action: { label: 'Créer un département', route: '/departments/new', roles: admins } },
  { label: 'Congés', route: '/leaves', icon: 'event_note', description: 'Consultez les demandes de congé, suivez leur état et gérez les validations selon vos autorisations.', category: 'Ressources humaines', roles: [...admins, CrmRole.EMPLOYEE],
    action: { label: 'Demander un congé', route: '/leaves/new', roles: [CrmRole.EMPLOYEE] } },
  { label: 'Associations employés', route: '/leaves', fragment: 'associations', icon: 'link', description: 'Associez les comptes aux collaborateurs.', category: 'Ressources humaines', roles: admins },
  { label: 'Paie', route: '/payroll', icon: 'payments', description: 'Consultez et gérez les fiches de paie selon les droits de votre compte.', category: 'Ressources humaines', roles: admins,
    action: { label: 'Créer une fiche de paie', route: '/payroll/new', roles: admins } },
  { label: 'Clients', route: '/customers', icon: 'groups', description: 'Centralisez les informations clients et simplifiez le suivi des relations commerciales.', category: 'Relation client et facturation', roles: admins,
    action: { label: 'Ajouter un client', route: '/customers/new', roles: admins } },
  { label: 'Factures', route: '/invoices', icon: 'description', description: 'Gérez les factures, consultez leurs montants et suivez leurs statuts.', category: 'Relation client et facturation', roles: admins,
    action: { label: 'Créer une facture', route: '/invoices/new', roles: admins } },
  { label: 'Honoraires', route: '/fees', icon: 'receipt_long', description: 'Suivez les prestations et les honoraires enregistrés dans le système.', category: 'Relation client et facturation', roles: admins,
    action: { label: 'Ajouter des honoraires', route: '/fees/new', roles: admins } }
];

@Injectable({ providedIn: 'root' })
export class CrmNavigation {
  private readonly auth = inject(AuthService);
  items() { return CRM_NAVIGATION.filter(item => this.auth.hasAnyRole([...item.roles])); }
  groups() {
    const items = this.items();
    return [...new Set(items.map(item => item.category))].map(label => ({ label, items: items.filter(item => item.category === label) }));
  }
  modules() { return this.items().filter(item => item.route !== '/home' && item.route !== '/dashboard' && !item.fragment); }
  actions() { return this.modules().flatMap(item => item.action && this.auth.hasAnyRole([...item.action.roles]) ? [item.action] : []); }
  roleLabel() {
    if (this.auth.hasAnyRole([CrmRole.SUPER_ADMIN])) return 'Super administrateur';
    if (this.auth.hasAnyRole([CrmRole.ADMIN])) return 'Administrateur';
    if (this.auth.hasAnyRole([CrmRole.EMPLOYEE])) return 'Employé';
    if (this.auth.hasAnyRole([CrmRole.CLIENT])) return 'Client';
    return 'Compte connecté';
  }
}
