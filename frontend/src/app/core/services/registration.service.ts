import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RegisterRequest } from '../models/register.models';

@Injectable({ providedIn: 'root' })
export class RegistrationService {
  private readonly http = inject(HttpClient);
  private readonly registerUrl = 'http://localhost:8080/identity/api/auth/register';

  register(request: RegisterRequest): Observable<void> {
    return this.http.post<void>(this.registerUrl, request);
  }
}