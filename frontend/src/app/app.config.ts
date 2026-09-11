import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { MatPaginatorIntl } from '@angular/material/paginator';

import { authInterceptor } from './core/interceptors/auth.interceptor';
import { routes } from './app.routes';
import { getFrenchPaginatorIntl } from './features/employees/pages/employees-page/employees-page.component';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    { provide: MatPaginatorIntl, useFactory: getFrenchPaginatorIntl }
  ]
};
