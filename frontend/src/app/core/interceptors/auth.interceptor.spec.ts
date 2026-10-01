import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';
import { storeTestSession, testJwt, testLogin } from '../testing/auth-fixtures';
import { CrmRole } from '../models/role.model';

describe('authInterceptor with real AuthService', () => {
  const api = 'http://localhost:8080/employee/api/employees';
  const base = 'http://localhost:8080/identity/api/auth';
  let client: HttpClient;
  let http: HttpTestingController;
  let auth: AuthService;
  let router: Router;
  let oldToken: string;
  const unauthorized = { status: 401, statusText: 'Unauthorized' };
  const rotated = () => ({ accessToken: testJwt({ exp: Math.floor(Date.now() / 1000) + 7200 }),
    refreshToken: 'fictitious-rotated', roles: [CrmRole.ADMIN], type: 'Bearer', expiration: 3600000 });
  function setup(refreshToken = 'fictitious-refresh') {
    sessionStorage.clear();
    oldToken = testJwt();
    storeTestSession(oldToken, refreshToken);
    TestBed.configureTestingModule({ providers: [provideHttpClient(withInterceptors([authInterceptor])),
      provideHttpClientTesting(), provideRouter([]), provideZonelessChangeDetection()] });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    spyOnProperty(router, 'url', 'get').and.returnValue('/employees?view=list');
  }
  beforeEach(() => setup());
  afterEach(() => { http.verify(); TestBed.resetTestingModule(); sessionStorage.clear(); });
  const get = (url = api) => client.get(url).subscribe({ error: () => {} });

  it('adds the bearer token and preserves other headers', () => {
    client.get(api, { headers: { 'X-Test': 'kept' } }).subscribe();
    const request = http.expectOne(api);
    expect(request.request.headers.get('Authorization')).toBe('Bearer ' + oldToken);
    expect(request.request.headers.get('X-Test')).toBe('kept');
    request.flush([]);
  });

  it('refreshes and replays the original PUT exactly once with the new JWT', () => {
    const body = { firstName: 'Fictitious' };
    client.put(api + '/12', body, { headers: { 'X-Test': 'kept' }, params: { view: 'full' } })
      .subscribe({ error: () => {} });
    http.expectOne(api + '/12?view=full').flush({}, unauthorized);
    const refresh = http.expectOne(base + '/refresh');
    expect(refresh.request.method).toBe('POST');
    expect(refresh.request.body).toEqual({ refreshToken: 'fictitious-refresh' });
    expect(refresh.request.headers.has('Authorization')).toBeFalse();
    const response = rotated();
    refresh.flush(response);
    const replay = http.expectOne(api + '/12?view=full');
    expect(replay.request.method).toBe('PUT');
    expect(replay.request.body).toEqual(body);
    expect(replay.request.headers.get('Authorization')).toBe('Bearer ' + response.accessToken);
    expect(replay.request.headers.get('X-Test')).toBe('kept');
    replay.flush({});
    expect(auth.getToken()).toBe(response.accessToken);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('shares one refresh and completes all three requests when B and C receive 401 during refresh', () => {
    const urls = [api + '/1', api + '/2', api + '/3'];
    const received: string[] = [];
    const completed: string[] = [];
    const subscriptions = urls.map((url) => client.get(url).subscribe({
      next: () => received.push(url),
      error: fail,
      complete: () => completed.push(url)
    }));
    http.expectOne(api + '/1').flush({}, unauthorized);
    const refresh = http.expectOne(base + '/refresh');
    http.expectOne(api + '/2').flush({}, unauthorized);
    http.expectOne(api + '/3').flush({}, unauthorized);
    http.expectNone(base + '/refresh');
    expect(received).toEqual([]);
    expect(subscriptions.every((subscription) => !subscription.closed)).toBeTrue();
    const response = rotated();
    refresh.flush(response);
    for (const url of urls) {
      const replay = http.expectOne(url);
      expect(replay.request.headers.get('Authorization')).toBe('Bearer ' + response.accessToken);
      replay.flush({});
    }
    expect(received).toEqual(urls);
    expect(completed).toEqual(urls);
    expect(subscriptions.every((subscription) => subscription.closed)).toBeTrue();
    http.expectNone(base + '/refresh');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('reuses a refreshed token for a delayed 401 without rotating again', () => {
    get(api + '/1'); get(api + '/2');
    http.expectOne(api + '/1').flush({}, unauthorized);
    const delayed = http.expectOne(api + '/2');
    http.expectOne(base + '/refresh').flush(rotated());
    http.expectOne(api + '/1').flush({});
    delayed.flush({}, unauthorized);
    http.expectOne(api + '/2').flush({});
    http.expectNone(base + '/refresh');
    expect(auth.getToken()).not.toBe(oldToken);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('does not refresh again if Identity returns the same JWT within the same second', () => {
    get(api + '/1'); get(api + '/2');
    http.expectOne(api + '/1').flush({}, unauthorized);
    const delayed = http.expectOne(api + '/2');
    http.expectOne(base + '/refresh').flush({ ...rotated(), accessToken: oldToken });
    http.expectOne(api + '/1').flush({});
    delayed.flush({}, unauthorized);
    const replay = http.expectOne(api + '/2');
    expect(replay.request.headers.get('Authorization')).toBe('Bearer ' + oldToken);
    replay.flush({});
    http.expectNone(base + '/refresh');
  });

  for (const status of [401, 403, 500, 0]) {
    it('cleans up and redirects when refresh fails with status ' + status, () => {
      get();
      http.expectOne(api).flush({}, unauthorized);
      const refresh = http.expectOne(base + '/refresh');
      if (status === 0) refresh.error(new ProgressEvent('error'));
      else refresh.flush({}, { status, statusText: 'Refresh failed' });
      expect(auth.getToken()).toBeNull();
      expect(sessionStorage.getItem('crm.auth.session')).toBeNull();
      expect(router.navigate).toHaveBeenCalledOnceWith(['/login'],
        { queryParams: { returnUrl: '/employees?view=list' } });
      http.expectNone(base + '/refresh');
      http.expectNone(api);
    });
  }

  it('cleans up and redirects without a refresh token', () => {
    TestBed.resetTestingModule();
    setup('');
    get();
    const request = http.expectOne(api);
    expect(request.request.headers.get('Authorization')).toBe('Bearer ' + oldToken);
    request.flush({}, unauthorized);
    http.expectNone(base + '/refresh');
    expect(router.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: '/employees?view=list' } });
    expect(auth.getToken()).toBeNull();
  });

  it('terminates on refresh 400 and ignores a later 401 from the cleared session', () => {
    const errors: HttpErrorResponse[] = [];
    const request = client.get(api).subscribe({ next: () => fail('Unexpected replay'), error: (error) => errors.push(error) });
    const lateRequest = client.get(api + '/late').subscribe({ error: (error) => errors.push(error) });
    const late = http.expectOne(api + '/late');
    http.expectOne(api).flush({}, unauthorized);
    http.expectOne(base + '/refresh').flush({}, { status: 400, statusText: 'Bad Request' });
    expect(request.closed).toBeTrue();
    expect(errors.map((error) => error.status)).toEqual([400]);
    late.flush({}, unauthorized);
    expect(lateRequest.closed).toBeTrue();
    expect(errors.map((error) => error.status)).toEqual([400, 401]);
    expect(auth.isAuthenticated()).toBeFalse();
    expect(auth.getToken()).toBeNull();
    expect(auth.getCurrentUser()).toBeNull();
    expect(auth.getRoles()).toEqual([]);
    expect(sessionStorage.getItem('crm.auth.session')).toBeNull();
    http.expectNone(base + '/refresh');
    http.expectNone((pending) => pending.url.startsWith(api));
    expect(router.navigate).toHaveBeenCalledOnceWith(['/login'],
      { queryParams: { returnUrl: '/employees?view=list' } });
  });

  it('omits authorization with no session and redirects on 401', () => {
    auth.clearSession();
    get();
    const request = http.expectOne(api);
    expect(request.request.headers.has('Authorization')).toBeFalse();
    request.flush({}, unauthorized);
    http.expectNone(base + '/refresh');
    expect(router.navigate).toHaveBeenCalledTimes(1);
  });

  it('uses the rotated refresh token on the next independent renewal', () => {
    get();
    http.expectOne(api).flush({}, unauthorized);
    http.expectOne(base + '/refresh').flush(rotated());
    http.expectOne(api).flush({});
    get();
    http.expectOne(api).flush({}, unauthorized);
    const secondRefresh = http.expectOne(base + '/refresh');
    expect(secondRefresh.request.body).toEqual({ refreshToken: 'fictitious-rotated' });
    secondRefresh.flush(rotated());
    http.expectOne(api).flush({});
  });

  it('does not loop if the replay also returns 401', () => {
    get();
    http.expectOne(api).flush({}, unauthorized);
    http.expectOne(base + '/refresh').flush(rotated());
    http.expectOne(api).flush({}, unauthorized);
    http.expectNone(base + '/refresh');
    expect(auth.isAuthenticated()).toBeFalse();
    expect(router.navigate).toHaveBeenCalledTimes(1);
  });

  it('keeps the session and does not refresh on 403', () => {
    get();
    http.expectOne(api).flush({}, { status: 403, statusText: 'Forbidden' });
    http.expectNone(base + '/refresh');
    expect(auth.getToken()).toBe(oldToken);
    expect(auth.isAuthenticated()).toBeTrue();
    expect(router.navigate).toHaveBeenCalledWith(['/access-denied']);
  });

  it('keeps the refreshed session when the replay returns 403', () => {
    get();
    http.expectOne(api).flush({}, unauthorized);
    http.expectOne(base + '/refresh').flush(rotated());
    http.expectOne(api).flush({}, { status: 403, statusText: 'Forbidden' });
    expect(auth.isAuthenticated()).toBeTrue();
    expect(router.navigate).toHaveBeenCalledWith(['/access-denied']);
    http.expectNone(base + '/refresh');
  });

  for (const endpoint of ['login', 'register', 'refresh']) {
    it('never refreshes or redirects recursively for ' + endpoint, () => {
      client.post(base + '/' + endpoint, {}).subscribe({ error: () => {} });
      const request = http.expectOne(base + '/' + endpoint);
      expect(request.request.headers.has('Authorization')).toBeFalse();
      request.flush({}, unauthorized);
      http.expectNone(base + '/refresh');
      expect(router.navigate).not.toHaveBeenCalled();
    });
  }

  it('does not send tokens or handle auth errors outside the CRM gateway', () => {
    get('https://example.test/api/data');
    const request = http.expectOne('https://example.test/api/data');
    expect(request.request.headers.has('Authorization')).toBeFalse();
    request.flush({}, unauthorized);
    http.expectNone(base + '/refresh');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('does not retry or clear the session for a server error', () => {
    get();
    http.expectOne(api).flush({}, { status: 500, statusText: 'Error' });
    expect(auth.getToken()).toBe(oldToken);
    http.expectNone(base + '/refresh');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('does not replay or restore a session after logout during refresh', () => {
    get();
    http.expectOne(api).flush({}, unauthorized);
    const refresh = http.expectOne(base + '/refresh');
    auth.logout();
    refresh.flush(rotated());
    http.expectNone(api);
    expect(auth.getToken()).toBeNull();
    expect(router.navigate).toHaveBeenCalledOnceWith(['/login']);
  });

  it('does not invalidate a new login due to an old request failure', () => {
    get();
    const pending = http.expectOne(api);
    auth.login({ username: 'unit', email: null, password: 'fictitious' }).subscribe();
    http.expectOne(base + '/login').flush(testLogin());
    pending.flush({}, unauthorized);
    expect(auth.isAuthenticated()).toBeTrue();
    http.expectNone(base + '/refresh');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('keeps logout final when the pending refresh subsequently fails', () => {
    const errors: HttpErrorResponse[] = [];
    const request = client.get(api).subscribe({ next: () => fail('Unexpected replay'), error: (error) => errors.push(error) });
    http.expectOne(api).flush({}, unauthorized);
    const refresh = http.expectOne(base + '/refresh');
    auth.logout();
    refresh.flush({}, unauthorized);
    expect(request.closed).toBeTrue();
    expect(errors.length).toBe(1);
    expect(errors[0].status).toBe(401);
    expect(auth.isAuthenticated()).toBeFalse();
    expect(auth.getToken()).toBeNull();
    expect(auth.getCurrentUser()).toBeNull();
    expect(auth.getRoles()).toEqual([]);
    expect(sessionStorage.getItem('crm.auth.session')).toBeNull();
    http.expectNone(api);
    http.expectNone(base + '/refresh');
    expect(router.navigate).toHaveBeenCalledOnceWith(['/login']);
  });

  it('clears a session on an invalid refresh response without replaying', () => {
    get();
    http.expectOne(api).flush({}, unauthorized);
    http.expectOne(base + '/refresh').flush({ ...rotated(), accessToken: 'malformed' });
    http.expectNone(api);
    expect(auth.isAuthenticated()).toBeFalse();
  });

  it('shares refresh failure and redirects concurrent requests only once', () => {
    const urls = [api + '/1', api + '/2', api + '/3'];
    const errors: HttpErrorResponse[] = [];
    const subscriptions = urls.map((url) => client.get(url).subscribe({
      next: () => fail('Unexpected response'),
      error: (error) => errors.push(error),
      complete: () => fail('Expected an error, not a successful completion')
    }));
    for (const url of urls) http.expectOne(url).flush({}, unauthorized);
    expect(errors).toEqual([]);
    expect(subscriptions.every((subscription) => !subscription.closed)).toBeTrue();
    http.expectOne(base + '/refresh').flush({}, unauthorized);
    expect(errors.length).toBe(3);
    expect(errors.every((error) => error.status === 401 && error === errors[0])).toBeTrue();
    expect(subscriptions.every((subscription) => subscription.closed)).toBeTrue();
    http.expectNone(base + '/refresh');
    http.expectNone((pending) => pending.url.startsWith(api));
    expect(router.navigate).toHaveBeenCalledOnceWith(['/login'],
      { queryParams: { returnUrl: '/employees?view=list' } });
    expect(auth.getToken()).toBeNull();
    expect(sessionStorage.getItem('crm.auth.session')).toBeNull();
  });
});
