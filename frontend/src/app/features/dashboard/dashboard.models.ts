export interface DashboardStat {
  title: string;
  value: string;
  description: string;
  icon: string;
  accent: 'blue' | 'teal' | 'amber' | 'coral' | 'violet' | 'green' | 'slate';
}

export type ActivityStatus = 'Completed' | 'Pending' | 'In review';

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