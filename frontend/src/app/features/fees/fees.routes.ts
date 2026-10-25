import { Routes } from '@angular/router';
export const FEES_ROUTES: Routes = [
  { path: '', pathMatch: 'full', loadComponent: () => import('./fees-page.component').then(m => m.FeesPageComponent) },
  { path: 'new', loadComponent: () => import('./fee-form.component').then(m => m.FeeFormComponent) },
  { path: ':id/edit', loadComponent: () => import('./fee-form.component').then(m => m.FeeFormComponent) },
  { path: ':id', loadComponent: () => import('./fee-detail.component').then(m => m.FeeDetailComponent) }
];
