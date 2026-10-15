import { Routes } from '@angular/router';

export const CUSTOMERS_ROUTES: Routes = [
  { path: '', pathMatch: 'full', loadComponent: () => import('./customers-page.component').then(m => m.CustomersPageComponent) },
  { path: 'new', loadComponent: () => import('./customer-form.component').then(m => m.CustomerFormComponent) },
  { path: ':id/edit', loadComponent: () => import('./customer-form.component').then(m => m.CustomerFormComponent) },
  { path: ':id', loadComponent: () => import('./customer-detail.component').then(m => m.CustomerDetailComponent) }
];
