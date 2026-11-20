import { Routes } from '@angular/router';
export const PAYROLL_ROUTES: Routes = [
  { path: '', pathMatch: 'full', loadComponent: () => import('./payrolls-page.component').then(m => m.PayrollsPageComponent) },
  { path: 'new', loadComponent: () => import('./payroll-form.component').then(m => m.PayrollFormComponent) },
  { path: ':id/edit', loadComponent: () => import('./payroll-form.component').then(m => m.PayrollFormComponent) },
  { path: ':id', loadComponent: () => import('./payroll-detail.component').then(m => m.PayrollDetailComponent) }
];
