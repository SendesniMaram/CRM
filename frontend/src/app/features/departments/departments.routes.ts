import { Routes } from '@angular/router';
import { DepartmentsPageComponent } from './pages/departments-page/departments-page.component';

export const DEPARTMENTS_ROUTES: Routes = [
  { path: '', pathMatch: 'full', component: DepartmentsPageComponent, title: 'Départements' },
  {
    path: 'new', title: 'Nouveau département',
    loadComponent: () => import('./pages/department-form/department-form.component').then(
      ({ DepartmentFormComponent }) => DepartmentFormComponent)
  },
  {
    path: ':id/edit', title: 'Modifier le département',
    loadComponent: () => import('./pages/department-form/department-form.component').then(
      ({ DepartmentFormComponent }) => DepartmentFormComponent)
  }
];
