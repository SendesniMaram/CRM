import { of } from 'rxjs';
import { DashboardDataService, emptyDashboard } from '../../features/dashboard/dashboard-data.service';

// Navigation tests exercise routing; Dashboard data has its own HTTP and role tests.
export const dashboardNavigationStub = {
  provide: DashboardDataService,
  useValue: { scope: () => 'client', load: () => of(emptyDashboard('client')) }
};
