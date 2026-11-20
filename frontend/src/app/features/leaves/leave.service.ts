import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { LeaveAssociation, LeaveDirectoryOptions, LeaveRequest, LeaveResponse } from './leave.models';

@Injectable({ providedIn: 'root' })
export class LeaveService {
  private readonly http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/hr/api/leaves';
  list() { return this.http.get<LeaveResponse[]>(this.url); }
  get(id: number) { return this.http.get<LeaveResponse>(`${this.url}/${id}`); }
  create(request: LeaveRequest) { return this.http.post<LeaveResponse>(`${this.url}/mine`, request); }
  update(id: number, request: LeaveRequest) { return this.http.put<LeaveResponse>(`${this.url}/mine/${id}`, request); }
  delete(id: number) { return this.http.delete<void>(`${this.url}/${id}`); }
  approve(id: number) { return this.http.patch<LeaveResponse>(`${this.url}/${id}/approve`, {}); }
  reject(id: number) { return this.http.patch<LeaveResponse>(`${this.url}/${id}/reject`, {}); }
  associations() { return this.http.get<LeaveAssociation[]>(`${this.url}/associations`); }
  options() { return this.http.get<LeaveDirectoryOptions>(`${this.url}/associations/options`); }
  associate(userId: number, employeeId: number) { return this.http.post<LeaveAssociation>(`${this.url}/associations`, { userId, employeeId }); }
  reassign(userId: number, employeeId: number) { return this.http.put<LeaveAssociation>(`${this.url}/associations/${userId}`, { employeeId }); }
}
