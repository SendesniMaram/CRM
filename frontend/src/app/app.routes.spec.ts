import { Route } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { AccessDeniedComponent } from './features/access-denied/access-denied.component';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { LayoutComponent } from './layout/components/layout/layout.component';
import { routes } from './app.routes';

describe('application routes', () => {
  const privateShell = routes.find((route) => route.component === LayoutComponent);
  const loginRoute = routes.find((route) => route.path === 'login');
  const accessDeniedRoute = routes.find((route) => route.path === 'access-denied');
  const registerRoute = routes.find((route) => route.path === 'register');
  const dashboardRoute = privateShell?.children?.find((route) => route.path === 'dashboard');
  const employeesRoute = privateShell?.children?.find((route) => route.path === 'employees');

  it('exposes login as a public route', () => {
    expect(loginRoute?.component).toBe(LoginComponent);
    expect(loginRoute?.canActivate).toBeUndefined();
  });

  it('protects dashboard with authGuard', () => {
    expect(dashboardRoute?.canActivate).toContain(authGuard);
    expect(dashboardRoute?.canActivate).toContain(roleGuard);
  });

  it('protects Employees with admin roles and lazy loads its feature routes', () => {
    expect(employeesRoute?.canActivate).toContain(authGuard);
    expect(employeesRoute?.canActivate).toContain(roleGuard);
    expect(employeesRoute?.data?.['roles']).toEqual(['SUPER_ADMIN', 'ADMIN']);
    expect(employeesRoute?.loadChildren).toEqual(jasmine.any(Function));
  });

  it('exposes access denied as a public route', () => {
    expect(accessDeniedRoute?.component).toBe(AccessDeniedComponent);
    expect(accessDeniedRoute?.canActivate).toBeUndefined();
  });

  it('exposes register as a public route', () => {
    expect(registerRoute?.component).toBe(RegisterComponent);
    expect(registerRoute?.canActivate).toBeUndefined();
  });

  it('keeps dashboard lazy-loaded', () => {
    expect(dashboardRoute?.loadComponent).toEqual(jasmine.any(Function));
  });

  it('keeps private routes inside LayoutComponent', () => {
    expect(privateShell?.component).toBe(LayoutComponent);
    expect(privateShell?.children).toContain(dashboardRoute as Route);
  });
});