export interface DashboardStat {
  title: string;
  value: string;
  description: string;
  icon: string;
  accent: 'blue' | 'teal' | 'amber' | 'coral' | 'violet' | 'green' | 'slate';
}

export type ActivityStatus =
  'Terminée' |
  'En attente' |
  'En cours d’examen';

export interface RecentActivity {
  type: string;
  description: string;
  user: string;
  date: string;
  status: ActivityStatus;
}

export interface DashboardSummary {
  label: string;
  value: string;
  detail: string;
  icon: string;
}