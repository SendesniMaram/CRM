import { EMPLOYEES_ROUTES } from './employees.routes';
import { EmployeesPageComponent } from './pages/employees-page/employees-page.component';

describe('employee feature routes', () => {
  it('keeps the list route exact and lazy loads the detail route', () => {
    const listRoute = EMPLOYEES_ROUTES.find((route) => route.path === '');
    const detailRoute = EMPLOYEES_ROUTES.find((route) => route.path === ':id');

    expect(listRoute?.component).toBe(EmployeesPageComponent);
    expect(listRoute?.pathMatch).toBe('full');
    expect(detailRoute?.loadComponent).toEqual(jasmine.any(Function));
  });
});
