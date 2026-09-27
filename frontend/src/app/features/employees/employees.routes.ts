import { Routes } from '@angular/router';
import { EmployeesPageComponent } from './pages/employees-page/employees-page.component';

export const EMPLOYEES_ROUTES: Routes = [
  {
    path: '',
    component: EmployeesPageComponent,
    pathMatch: 'full',
    title: 'Employés'
  },
  {
    path: 'new',
    title: 'Nouvel employé',
    loadComponent: () => import('./pages/employee-form/employee-form.component').then(
      ({ EmployeeFormComponent }) => EmployeeFormComponent
    )
  },
  {
    path: ':id/edit',
    title: 'Modifier l’employé',
    loadComponent: () => import('./pages/employee-form/employee-form.component').then(
      ({ EmployeeFormComponent }) => EmployeeFormComponent
    )
  },
  {
    path: ':id',
    title: "Détail de l'employé",
    loadComponent: () =>
      import('./pages/employee-detail/employee-detail.component').then(
        ({ EmployeeDetailComponent }) => EmployeeDetailComponent
      )
  }
];
