import { LoginResponse } from '../models/auth.models';
import { CrmRole } from '../models/role.model';

// Unsigned, fictitious tokens used only by HTTP mocks and browser unit tests.
export function testJwt(payload: object = { exp: Math.floor(Date.now() / 1000) + 3600 }): string {
  const encode = (value: object) => btoa(JSON.stringify(value)).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
  return encode({ alg: 'HS256' }) + '.' + encode(payload) + '.fictitious';
}
export function testLogin(): LoginResponse {
  return { username: 'unit-user', email: 'unit@example.test', enabled: true,
    token: testJwt(), refreshToken: 'fictitious-refresh', type: 'Bearer',
    role: CrmRole.ADMIN, roles: [CrmRole.ADMIN], expiration: 3600000 };
}
export function storeTestSession(token = testJwt(), refreshToken = 'fictitious-refresh'): void {
  const { token: ignored, refreshToken: ignoredRefresh, ...user } = testLogin();
  sessionStorage.setItem('crm.auth.session', JSON.stringify({ accessToken: token, refreshToken, user }));
}
