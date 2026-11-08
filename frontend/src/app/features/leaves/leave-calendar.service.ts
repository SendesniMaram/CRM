import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { validHrPeriod } from '../hr-admin/hr-insights.service';
import { Observable } from 'rxjs';
import { LeaveResponse } from './leave.models';

@Injectable({ providedIn: 'root' })
export class LeaveCalendarService {
  private readonly http = inject(HttpClient);
  list(from: string, to: string, employeeId = ''): Observable<LeaveResponse[]> {
    if (!validHrPeriod(from,to)) throw new Error('Invalid calendar range');
    return this.http.get<LeaveResponse[]>('http://localhost:8080/hr/api/leaves/calendar', {
      params: { from, to, ...(employeeId.trim() ? { employeeId: employeeId.trim() } : {}) }
    });
  }
}
