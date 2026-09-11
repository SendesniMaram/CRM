import { Routes } from '@angular/router';
import { EmployeesPageComponent } from './pages/employees-page/employees-page.component';

export const EMPLOYEES_ROUTES: Routes = [
  {
    path: '',
    component: EmployeesPageComponent,
    title: 'Employés'
  }
];
