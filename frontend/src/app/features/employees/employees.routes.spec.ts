import { EMPLOYEES_ROUTES } from './employees.routes';
import { EmployeesPageComponent } from './pages/employees-page/employees-page.component';

describe('employee feature routes', () => {
  it('lazy loads creation before the id route', async () => {
    const index = EMPLOYEES_ROUTES.findIndex((route) => route.path === 'new');
    expect(index).toBeGreaterThanOrEqual(0);
    expect(index).toBeLessThan(EMPLOYEES_ROUTES.findIndex((route) => route.path === ':id'));
    const { EmployeeFormComponent } = await import('./pages/employee-form/employee-form.component');
    expect(await EMPLOYEES_ROUTES[index].loadComponent!()).toBe(EmployeeFormComponent);
  });

  it('lazy loads the same form for editing', async () => {
    const route = EMPLOYEES_ROUTES.find((route) => route.path === ':id/edit');
    const { EmployeeFormComponent } = await import('./pages/employee-form/employee-form.component');
    expect(await route!.loadComponent!()).toBe(EmployeeFormComponent);
  });
  it('keeps the list route exact and lazy loads the detail route', () => {
    const listRoute = EMPLOYEES_ROUTES.find((route) => route.path === '');
    const detailRoute = EMPLOYEES_ROUTES.find((route) => route.path === ':id');

    expect(listRoute?.component).toBe(EmployeesPageComponent);
    expect(listRoute?.pathMatch).toBe('full');
    expect(detailRoute?.loadComponent).toEqual(jasmine.any(Function));
  });
});
