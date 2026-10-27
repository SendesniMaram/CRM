import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { LeaveResponse } from './leave.models';

@Injectable({ providedIn: 'root' })
export class LeaveCalendarService {
  private readonly http = inject(HttpClient);
  list(from: string, to: string, employeeId = ''): Observable<LeaveResponse[]> {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(from) || !/^\d{4}-\d{2}-\d{2}$/.test(to) || to < from) throw new Error('Invalid calendar range');
    return this.http.get<LeaveResponse[]>('http://localhost:8080/hr/api/leaves/calendar', {
      params: { from, to, ...(employeeId.trim() ? { employeeId: employeeId.trim() } : {}) }
    });
  }
}
