import { DEPARTMENTS_ROUTES } from './departments.routes';
import { DepartmentsPageComponent } from './pages/departments-page/departments-page.component';

describe('department routes', () => {
  it('exposes the collection and lazily loads creation and editing', async () => {
    expect(DEPARTMENTS_ROUTES[0].component).toBe(DepartmentsPageComponent);
    expect(DEPARTMENTS_ROUTES.map((route) => route.path)).toEqual(['', 'new', ':id/edit']);
    const creation = await DEPARTMENTS_ROUTES[1].loadComponent!();
    const edit = await DEPARTMENTS_ROUTES[2].loadComponent!();
    expect(creation).toBe(edit);
  });
});
