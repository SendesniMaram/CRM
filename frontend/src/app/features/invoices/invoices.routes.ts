import { Routes } from '@angular/router';
export const INVOICES_ROUTES: Routes = [
  { path: '', pathMatch: 'full', loadComponent: () => import('./invoices-page.component').then(m => m.InvoicesPageComponent) },
  { path: 'new', loadComponent: () => import('./invoice-form.component').then(m => m.InvoiceFormComponent) },
  { path: ':id/edit', loadComponent: () => import('./invoice-form.component').then(m => m.InvoiceFormComponent) },
  { path: ':id', loadComponent: () => import('./invoice-detail.component').then(m => m.InvoiceDetailComponent) }
];
