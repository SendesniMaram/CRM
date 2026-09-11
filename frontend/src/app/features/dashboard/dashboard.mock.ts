import { DashboardStat, DashboardSummary, RecentActivity } from './dashboard.models';

// Temporary presentation data. These values will be replaced by microservice APIs.
export const DASHBOARD_STATS: DashboardStat[] = [
  {
    title: 'Employés',
    value: '248',
    description: '+12 ce mois-ci',
    icon: 'groups',
    accent: 'blue'
  },
  {
    title: 'Clients',
    value: '1,284',
    description: '+8,4 % par rapport au mois dernier',
    icon: 'business',
    accent: 'teal'
  },
  {
    title: 'Départements',
    value: '18',
    description: 'Répartis sur 4 sites',
    icon: 'account_tree',
    accent: 'amber'
  },
  {
    title: 'Factures',
    value: '€86.4K',
    description: 'Émises ce mois-ci',
    icon: 'receipt_long',
    accent: 'coral'
  },
  {
    title: 'Paie',
    value: '€214K',
    description: 'Prochaine exécution dans 6 jours',
    icon: 'payments',
    accent: 'violet'
  },
  {
    title: 'Ressources humaines',
    value: '14',
    description: 'Demandes à examiner',
    icon: 'volunteer_activism',
    accent: 'green'
  },
  {
    title: 'Honoraires et dépenses',
    value: '€12.8K',
    description: "En attente d'approbation",
    icon: 'account_balance_wallet',
    accent: 'slate'
  }
];

export const RECENT_ACTIVITIES: RecentActivity[] = [
  {
    type: 'Employé',
    description: "Nouvelle fiche d'employé ajoutée à l'équipe Ingénierie",
    user: 'Sophie Martin',
    date: "Aujourd'hui, 09:42",
    status: 'Terminée'
  },
  {
    type: 'Client',
    description: 'Nouveau compte client créé',
    user: 'Thomas Bernard',
    date: "Aujourd'hui, 08:17",
    status: 'Terminée'
  },
  {
    type: 'Facture',
    description: 'Facture n° INV-2026-0184 émise',
    user: 'Équipe Finance',
    date: 'Hier, 16:28',
    status: 'En attente'
  },
  {
    type: 'Demande RH',
    description: 'Demande de congé soumise pour approbation',
    user: 'Lucas Petit',
    date: 'Hier, 14:05',
    status: 'En cours d’examen'
  },
  {
    type: 'Paie',
    description: 'Préparation mensuelle de la paie terminée',
    user: 'Équipe Paie',
    date: '12 sept. 2026, 11:30',
    status: 'Terminée'
  }
];

export const DASHBOARD_SUMMARY: DashboardSummary[] = [
  { label: 'Factures en attente', value: '24', detail: 'À traiter', icon: 'pending_actions' },
  { label: 'Demandes RH en attente', value: '14', detail: 'Tous les services', icon: 'event_note' },
  { label: 'Employés actifs', value: '232', detail: '93,5 % des effectifs', icon: 'person_check' },
  { label: 'Clients actifs', value: '1 146', detail: '89,2 % du portefeuille', icon: 'verified_user' }
];