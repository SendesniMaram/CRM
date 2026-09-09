import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AuthSession, LoginRequest, LoginResponse } from '../models/auth.models';
import { CrmRole } from '../models/role.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly loginUrl = 'http://localhost:8080/identity/api/auth/login';
  private readonly sessionKey = 'crm.auth.session';

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(this.loginUrl, request).pipe(
      tap((response) => this.saveSession(response))
    );
  }

  logout(): void {
    sessionStorage.removeItem(this.sessionKey);
  }

  isAuthenticated(): boolean {
    return Boolean(this.getToken());
  }

  getToken(): string | null {
    return this.getSession()?.accessToken ?? null;
  }

  getCurrentUser(): AuthSession['user'] | null {
    return this.getSession()?.user ?? null;
  }

  getRoles(): CrmRole[] {
    const roles = this.getCurrentUser()?.roles;
    return roles?.filter((role): role is CrmRole => Object.values(CrmRole).includes(role)) ?? [];
  }

  hasRole(role: CrmRole): boolean {
    return this.getRoles().includes(role);
  }

  hasAnyRole(roles: CrmRole[]): boolean {
    return roles.some((role) => this.hasRole(role));
  }

  private saveSession(response: LoginResponse): void {
    const session: AuthSession = {
      accessToken: response.token,
      refreshToken: response.refreshToken,
      user: {
        username: response.username,
        email: response.email,
        enabled: response.enabled,
        type: response.type,
        role: response.role,
        roles: response.roles,
        expiration: response.expiration
      }
    };

    sessionStorage.setItem(this.sessionKey, JSON.stringify(session));
  }

  private getSession(): AuthSession | null {
    const storedSession = sessionStorage.getItem(this.sessionKey);

    if (!storedSession) {
      return null;
    }

    try {
      return JSON.parse(storedSession) as AuthSession;
    } catch {
      this.logout();
      return null;
    }
  }
}
