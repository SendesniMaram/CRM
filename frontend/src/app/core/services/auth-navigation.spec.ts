import { safeReturnUrl } from './auth-navigation';

describe('safeReturnUrl', () => {
  for (const path of ['/employees', '/departments', '/departments/new', '/departments/12/edit', '/departments?view=all#list']) {
    it(`preserves the implemented internal destination ${path}`, () => {
      expect(safeReturnUrl(path)).toBe(path);
    });
  }

  for (const value of ['https://example.com', '//example.com', '/\\example.com', '/%2f%2fexample.com',
    '/departments/../login', '/departments/0/edit', '/departments/new/edit', '/departments/12',
    '/departments(aux:login)', '/departments/12/edit/extra', '/login', '/register', '/unknown', '/departments\n']) {
    it(`rejects an external, malformed or unimplemented destination ${JSON.stringify(value)}`, () => {
      expect(safeReturnUrl(value)).toBe('/dashboard');
    });
  }
});
