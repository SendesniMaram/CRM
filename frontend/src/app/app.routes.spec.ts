import { Component } from '@angular/core';
import { Route } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { LoginComponent } from './features/auth/login/login.component';
import { LayoutComponent } from './layout/components/layout/layout.component';
import { routes } from './app.routes';

describe('application routes', () => {
  const privateShell = routes.find((route) => route.component === LayoutComponent);
  const loginRoute = routes.find((route) => route.path === 'login');
  const dashboardRoute = privateShell?.children?.find((route) => route.path === 'dashboard');

  it('exposes login as a public route', () => {
    expect(loginRoute?.component).toBe(LoginComponent);
    expect(loginRoute?.canActivate).toBeUndefined();
  });

  it('protects dashboard with authGuard', () => {
    expect(dashboardRoute?.canActivate).toContain(authGuard);
  });

  it('keeps dashboard lazy-loaded', () => {
    expect(dashboardRoute?.loadComponent).toEqual(jasmine.any(Function));
  });

  it('keeps private routes inside LayoutComponent', () => {
    expect(privateShell?.component).toBe(LayoutComponent);
    expect(privateShell?.children).toContain(dashboardRoute as Route);
  });
});