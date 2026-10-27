import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { AccessDeniedComponent } from './features/access-denied/access-denied.component';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { LayoutComponent } from './layout/components/layout/layout.component';
import { NotFoundComponent } from './features/not-found/not-found.component';

export const routes: Routes = [
	{ path: 'welcome', title: 'CRM Workspace — Votre activité, en un regard',
		loadComponent: () => import('./features/landing/landing.component').then(m => m.LandingComponent) },
	{ path: 'login', component: LoginComponent },
	{ path: 'register', component: RegisterComponent },
	{ path: 'access-denied', component: AccessDeniedComponent },
	{
		path: '',
		component: LayoutComponent,
		children: [
            { path: 'trainings', canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN', 'SUPER_ADMIN'], hrModule: 'trainings' },
                loadComponent: () => import('./features/hr-admin/hr-admin-page.component').then(m => m.HrAdminPageComponent) },
            { path: 'attendances', canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN', 'SUPER_ADMIN'], hrModule: 'attendances' },
                loadComponent: () => import('./features/hr-admin/hr-admin-page.component').then(m => m.HrAdminPageComponent) },
            { path: 'hr-records', canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN', 'SUPER_ADMIN'], hrModule: 'hr-records' },
                loadComponent: () => import('./features/hr-admin/hr-admin-page.component').then(m => m.HrAdminPageComponent) },
            { path: 'contracts', canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN', 'SUPER_ADMIN'], hrModule: 'contracts' },
                loadComponent: () => import('./features/hr-admin/hr-admin-page.component').then(m => m.HrAdminPageComponent) },
            { path: 'evaluations', canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN', 'SUPER_ADMIN'], hrModule: 'evaluations' },
                loadComponent: () => import('./features/hr-admin/hr-admin-page.component').then(m => m.HrAdminPageComponent) },
            { path: 'job-offers', canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN', 'SUPER_ADMIN'], hrModule: 'job-offers' },
                loadComponent: () => import('./features/hr-admin/hr-admin-page.component').then(m => m.HrAdminPageComponent) },
            { path: 'applications', canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN', 'SUPER_ADMIN'], hrModule: 'applications' },
                loadComponent: () => import('./features/hr-admin/hr-admin-page.component').then(m => m.HrAdminPageComponent) },
            { path: 'leave-calendar', canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN', 'SUPER_ADMIN'] },
                loadComponent: () => import('./features/leaves/leave-calendar.component').then(m => m.LeaveCalendarComponent) },
            { path: 'my-evaluations', canActivate: [authGuard, roleGuard], data: { roles: ['EMPLOYEE'] },
                loadComponent: () => import('./features/hr-admin/my-evaluations.component').then(m => m.MyEvaluationsComponent) },
            { path: 'client', canActivate: [authGuard, roleGuard], data: { roles: ['CLIENT'] },
                loadComponent: () => import('./features/client/client-workspace.component').then(m => m.ClientWorkspaceComponent) },
            { path: 'customer-associations', canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN', 'SUPER_ADMIN'] },
                loadComponent: () => import('./features/client/customer-associations.component').then(m => m.CustomerAssociationsComponent) },
			{ path: 'profile', canActivate: [authGuard, roleGuard], data: { roles: ['EMPLOYEE'] },
				loadComponent: () => import('./features/profile/employee-profile.component').then(m => m.EmployeeProfileComponent) },
			{ path: 'my-payroll', canActivate: [authGuard, roleGuard], data: { roles: ['EMPLOYEE'] },
				loadComponent: () => import('./features/payroll/my-payroll-page.component').then(m => m.MyPayrollPageComponent) },
			{ path: 'my-payroll/:id', canActivate: [authGuard, roleGuard], data: { roles: ['EMPLOYEE'] },
				loadComponent: () => import('./features/payroll/my-payroll-detail.component').then(m => m.MyPayrollDetailComponent) },
			{ path: 'users', canActivate: [authGuard, roleGuard], data: { roles: ['SUPER_ADMIN', 'ADMIN'] },
				loadComponent: () => import('./features/users/users-page.component').then(m => m.UsersPageComponent) },
			{ path: 'home', canActivate: [authGuard, roleGuard],
				loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent) },
			{ path: 'payroll', canActivate: [authGuard, roleGuard], data: { roles: ['SUPER_ADMIN', 'ADMIN'] },
				loadChildren: () => import('./features/payroll/payroll.routes').then(m => m.PAYROLL_ROUTES) },
			{ path: 'fees', canActivate: [authGuard, roleGuard], data: { roles: ['SUPER_ADMIN', 'ADMIN'] },
				loadChildren: () => import('./features/fees/fees.routes').then(m => m.FEES_ROUTES) },
			{ path: 'invoices', canActivate: [authGuard, roleGuard], data: { roles: ['SUPER_ADMIN', 'ADMIN'] },
				loadChildren: () => import('./features/invoices/invoices.routes').then(m => m.INVOICES_ROUTES) },
			{ path: 'customers', canActivate: [authGuard, roleGuard], data: { roles: ['SUPER_ADMIN', 'ADMIN'] },
				loadChildren: () => import('./features/customers/customers.routes').then(m => m.CUSTOMERS_ROUTES) },
			{ path: 'hr', pathMatch: 'full', redirectTo: 'leaves' },
			{ path: 'leaves', canActivate: [authGuard, roleGuard], data: { roles: ['SUPER_ADMIN', 'ADMIN', 'EMPLOYEE'] },
				loadChildren: () => import('./features/leaves/leaves.routes').then(m => m.LEAVES_ROUTES) },
			{
				path: 'dashboard',
				canActivate: [authGuard, roleGuard],
				loadComponent: () =>
					import('./features/dashboard/dashboard.component').then(
						({ DashboardComponent }) => DashboardComponent
					)
			},
			{
				path: 'employees',
				canActivate: [authGuard, roleGuard],
				data: { roles: ['SUPER_ADMIN', 'ADMIN'] },
				loadChildren: () =>
					import('./features/employees/employees.routes').then(
						({ EMPLOYEES_ROUTES }) => EMPLOYEES_ROUTES
					)
			},
			{
				path: 'departments',
				canActivate: [authGuard, roleGuard],
				data: { roles: ['SUPER_ADMIN', 'ADMIN'] },
				loadChildren: () => import('./features/departments/departments.routes').then(
					({ DEPARTMENTS_ROUTES }) => DEPARTMENTS_ROUTES)
			},
			{ path: '', pathMatch: 'full', redirectTo: 'home' }
		]
	},
	{ path: '**', component: NotFoundComponent }
];
