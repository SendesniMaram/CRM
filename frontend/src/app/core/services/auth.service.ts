import { DestroyRef, Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, defer, finalize, map, of, shareReplay, tap, throwError, timeout } from 'rxjs';
import { AuthSession, LoginRequest, LoginResponse, RefreshTokenResponse } from '../models/auth.models';
import { CrmRole } from '../models/role.model';
import { jwtExpiration } from './jwt-expiration';
import { safeReturnUrl } from './auth-navigation';
import { isSessionUnavailable } from './session-unavailable';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly authUrl = 'http://localhost:8080/identity/api/auth';
  private readonly sessionKey = 'crm.auth.session';
  private readonly session = signal<AuthSession | null>(this.readSession());
  private readonly expiryTick = signal(0);
  private expiryTimer?: ReturnType<typeof setTimeout>;
  private refreshInFlight?: Observable<string>;
  readonly sessionNotice = signal('');
  private generation = 0;
  private revision = 0;

  constructor() {
    this.scheduleExpiry();
    inject(DestroyRef).onDestroy(() => clearTimeout(this.expiryTimer));
  }

  get sessionGeneration(): number { return this.generation; }
  get tokenRevision(): number { return this.revision; }

  login(request: LoginRequest): Observable<LoginResponse> {
    return defer(() => {
      this.clearSession();
      this.sessionNotice.set('');
      const generation = this.generation;
      return this.http.post<LoginResponse>(this.authUrl + '/login', request).pipe(
        tap((response) => {
          if (generation !== this.generation) {
            this.revokeRemote(response.refreshToken, generation);
            throw new Error('Session changed');
          }
          this.validateTokens(response.token, response.refreshToken);
          const { token, refreshToken, ...user } = response;
          this.saveSession({ accessToken: token, refreshToken, user });
        })
      );
    });
  }

  logout(): void {
    const refreshToken = this.session()?.refreshToken;
    this.clearSession();
    this.sessionNotice.set('Déconnexion locale effectuée.');
    if (refreshToken) this.revokeRemote(refreshToken, this.generation);
    void this.router.navigate(['/login']);
  }

  private revokeRemote(refreshToken: string, generation: number): void {
    if (!refreshToken) return;
    this.http.post<void>(this.authUrl + '/logout', { refreshToken }).pipe(timeout(5000)).subscribe({
      next: () => { if (generation === this.generation) this.sessionNotice.set('Déconnexion confirmée.'); },
      error: () => { if (generation === this.generation) this.sessionNotice.set('Déconnexion locale effectuée ; révocation serveur non confirmée. Une nouvelle connexion révoquera vos anciennes sessions.'); }
    });
  }

  expireSession(returnUrl: string, generation = this.generation): void {
    if (generation !== this.generation) return;
    this.clearSession();
    this.sessionNotice.set('Votre session a expiré ou a été révoquée. Reconnectez-vous.');
    void this.router.navigate(['/login'], { queryParams: { returnUrl: safeReturnUrl(returnUrl) } });
  }

  clearSession(): void {
    this.generation++;
    this.refreshInFlight = undefined;
    clearTimeout(this.expiryTimer);
    sessionStorage.removeItem(this.sessionKey);
    this.session.set(null);
  }

  isAuthenticated(): boolean {
    this.expiryTick();
    const expiration = jwtExpiration(this.getToken());
    return expiration !== null && expiration > Date.now();
  }

  getToken(): string | null { return this.session()?.accessToken ?? null; }

  getCurrentUser(): AuthSession['user'] | null {
    return this.isAuthenticated() ? this.session()?.user ?? null : null;
  }

  getRoles(): CrmRole[] {
    const roles = this.getCurrentUser()?.roles;
    return Array.isArray(roles) ? roles.filter((role) => Object.values(CrmRole).includes(role)) : [];
  }

  hasRole(role: CrmRole): boolean { return this.getRoles().includes(role); }
  hasAnyRole(roles: CrmRole[]): boolean { return roles.some((role) => this.hasRole(role)); }

  ensureSession(): Observable<boolean> {
    if (this.isAuthenticated()) return of(true);
    const generation = this.generation;
    return this.refreshAccessToken().pipe(
      map(() => true),
      catchError((error) => {
        if (isSessionUnavailable(error)) return throwError(() => error);
        if (generation === this.generation) {
          this.clearSession();
          this.sessionNotice.set('Votre session a expiré ou a été révoquée. Reconnectez-vous.');
        }
        return of(false);
      })
    );
  }

  refreshAccessToken(): Observable<string> {
    if (this.refreshInFlight) return this.refreshInFlight;
    const session = this.session();
    if (!session?.refreshToken) return throwError(() => new Error('No refresh token'));
    const generation = this.generation;
    const refresh = this.http.post<RefreshTokenResponse>(this.authUrl + '/refresh', {
      refreshToken: session.refreshToken
    }).pipe(
      map((response) => {
        // Late responses must not restore a session after logout or a new login.
        if (generation !== this.generation) {
          this.revokeRemote(response.refreshToken, generation);
          throw new Error('Session changed');
        }
        this.validateTokens(response.accessToken, response.refreshToken);
        this.saveSession({
          accessToken: response.accessToken,
          refreshToken: response.refreshToken,
          user: { ...session.user, roles: response.roles, role: response.roles?.[0],
            type: response.type, expiration: response.expiration }
        });
        return response.accessToken;
      }),
      finalize(() => { if (this.refreshInFlight === refresh) this.refreshInFlight = undefined; }),
      shareReplay({ bufferSize: 1, refCount: false })
    );
    this.refreshInFlight = refresh;
    return refresh;
  }

  private validateTokens(accessToken: string, refreshToken: string): void {
    const expiration = jwtExpiration(accessToken);
    if (expiration === null || expiration <= Date.now() || typeof refreshToken !== 'string' || !refreshToken.trim()) {
      throw new Error('Invalid authentication response');
    }
  }

  private saveSession(session: AuthSession): void {
    sessionStorage.setItem(this.sessionKey, JSON.stringify(session));
    this.revision++;
    this.session.set(session);
    this.scheduleExpiry();
  }

  private scheduleExpiry(): void {
    clearTimeout(this.expiryTimer);
    const expiration = jwtExpiration(this.getToken());
    if (expiration !== null && expiration > Date.now()) {
      this.expiryTimer = setTimeout(() => {
        this.expiryTick.update((tick) => tick + 1);
        this.scheduleExpiry();
      }, Math.min(expiration - Date.now(), 2147483647));
    }
  }

  private readSession(): AuthSession | null {
    try {
      const value = JSON.parse(sessionStorage.getItem(this.sessionKey) ?? 'null');
      if (value && typeof value.accessToken === 'string' && value.user && typeof value.user === 'object') {
        return { ...value, refreshToken: typeof value.refreshToken === 'string' ? value.refreshToken : '' };
      }
    } catch { /* Corrupt storage is not an authenticated session. */ }
    sessionStorage.removeItem(this.sessionKey);
    return null;
  }
}
