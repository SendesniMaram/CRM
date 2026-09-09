import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { AccessDeniedComponent } from './features/access-denied/access-denied.component';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { LayoutComponent } from './layout/components/layout/layout.component';

export const routes: Routes = [
	{ path: 'login', component: LoginComponent },
	{ path: 'register', component: RegisterComponent },
	{ path: 'access-denied', component: AccessDeniedComponent },
	{
		path: '',
		component: LayoutComponent,
		children: [
			{
				path: 'dashboard',
				canActivate: [authGuard, roleGuard],
				loadComponent: () =>
					import('./features/dashboard/dashboard.component').then(
						({ DashboardComponent }) => DashboardComponent
					)
			},
			{ path: '', pathMatch: 'full', redirectTo: 'dashboard' }
		]
	},
	{ path: '**', redirectTo: 'login' }
];
