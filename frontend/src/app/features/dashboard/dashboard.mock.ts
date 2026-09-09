import { DashboardStat, DashboardSummary, RecentActivity } from './dashboard.models';

// Temporary presentation data. These values will be replaced by microservice APIs.
export const DASHBOARD_STATS: DashboardStat[] = [
  {
    title: 'Employees',
    value: '248',
    description: '+12 this month',
    icon: 'groups',
    accent: 'blue'
  },
  {
    title: 'Customers',
    value: '1,284',
    description: '+8.4% from last month',
    icon: 'business',
    accent: 'teal'
  },
  {
    title: 'Departments',
    value: '18',
    description: 'Across 4 locations',
    icon: 'account_tree',
    accent: 'amber'
  },
  {
    title: 'Invoices',
    value: '€86.4K',
    description: 'Issued this month',
    icon: 'receipt_long',
    accent: 'coral'
  },
  {
    title: 'Payroll',
    value: '€214K',
    description: 'Next run in 6 days',
    icon: 'payments',
    accent: 'violet'
  },
  {
    title: 'Human resources',
    value: '14',
    description: 'Requests to review',
    icon: 'volunteer_activism',
    accent: 'green'
  },
  {
    title: 'Fees & expenses',
    value: '€12.8K',
    description: 'Awaiting approval',
    icon: 'account_balance_wallet',
    accent: 'slate'
  }
];

export const RECENT_ACTIVITIES: RecentActivity[] = [
  {
    type: 'Employee',
    description: 'New employee profile added to Engineering',
    user: 'Sophie Martin',
    date: 'Today, 09:42',
    status: 'Completed'
  },
  {
    type: 'Customer',
    description: 'New customer account created',
    user: 'Thomas Bernard',
    date: 'Today, 08:17',
    status: 'Completed'
  },
  {
    type: 'Invoice',
    description: 'Invoice #INV-2026-0184 issued',
    user: 'Finance team',
    date: 'Yesterday, 16:28',
    status: 'Pending'
  },
  {
    type: 'HR request',
    description: 'Leave request submitted for approval',
    user: 'Lucas Petit',
    date: 'Yesterday, 14:05',
    status: 'In review'
  },
  {
    type: 'Payroll',
    description: 'Monthly payroll preparation completed',
    user: 'Payroll team',
    date: '12 Sep 2026, 11:30',
    status: 'Completed'
  }
];

export const DASHBOARD_SUMMARY: DashboardSummary[] = [
  { label: 'Invoices pending', value: '24', detail: 'Needs attention', icon: 'pending_actions' },
  { label: 'HR requests pending', value: '14', detail: 'Across all teams', icon: 'event_note' },
  { label: 'Active employees', value: '232', detail: '93.5% of workforce', icon: 'person_check' },
  { label: 'Active customers', value: '1,146', detail: '89.2% of portfolio', icon: 'verified_user' }
];