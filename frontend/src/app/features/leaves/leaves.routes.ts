import { Routes } from '@angular/router';
import { roleGuard } from '../../core/guards/role.guard';
export const LEAVES_ROUTES: Routes = [
  { path: '', pathMatch: 'full', loadComponent: () => import('./leaves-page.component').then(m => m.LeavesPageComponent) },
  { path: 'new', canActivate: [roleGuard], data: { roles: ['EMPLOYEE'] }, loadComponent: () => import('./leave-form.component').then(m => m.LeaveFormComponent) },
  { path: ':id/edit', canActivate: [roleGuard], data: { roles: ['EMPLOYEE'] }, loadComponent: () => import('./leave-form.component').then(m => m.LeaveFormComponent) }
];
