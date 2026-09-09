import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { LoginComponent } from './features/auth/login/login.component';
import { LayoutComponent } from './layout/components/layout/layout.component';

export const routes: Routes = [
	{ path: 'login', component: LoginComponent },
	{
		path: '',
		component: LayoutComponent,
		children: [
			{
				path: 'dashboard',
				canActivate: [authGuard],
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
