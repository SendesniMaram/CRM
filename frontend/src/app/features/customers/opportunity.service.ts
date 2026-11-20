import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { OpportunityPage, OpportunityRequest, OpportunityStage, OpportunityStageHistory, SalesOpportunity } from './opportunity.models';

export interface OpportunityQuery {
  page: number;
  size: number;
  keyword?: string;
  stage?: OpportunityStage | '';
  employeeId?: number | null;
}

@Injectable({ providedIn: 'root' })
export class OpportunityService {
  private readonly http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/customer/api/opportunities';

  list(query: OpportunityQuery) {
    let params = new HttpParams().set('page', query.page).set('size', query.size)
      .set('sortBy', 'createdAt').set('direction', 'desc');
    if (query.keyword?.trim()) params = params.set('keyword', query.keyword.trim());
    if (query.stage) params = params.set('stage', query.stage);
    if (query.employeeId) params = params.set('employeeId', query.employeeId);
    return this.http.get<OpportunityPage>(this.url, { params });
  }
  get(id: number) { return this.http.get<SalesOpportunity>(`${this.url}/${id}`); }
  history(id: number) { return this.http.get<OpportunityStageHistory[]>(`${this.url}/${id}/history`); }
  create(request: OpportunityRequest) { return this.http.post<SalesOpportunity>(this.url, request); }
  update(id: number, request: OpportunityRequest) { return this.http.put<SalesOpportunity>(`${this.url}/${id}`, request); }
  changeStage(id: number, stage: OpportunityStage) {
    return this.http.patch<SalesOpportunity>(`${this.url}/${id}/stage`, { stage });
  }
  delete(id: number) { return this.http.delete<void>(`${this.url}/${id}`); }
}
