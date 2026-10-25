import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { FeePage, FeePageParams, FeeRequest, FeeResponse } from './fee.models';
@Injectable({ providedIn: 'root' })
export class FeeService {
  private readonly http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/fees/api/fees';
  list(query: FeePageParams) {
    // Supplying sortBy ensures the existing controller returns a Spring Page.
    let params = new HttpParams().set('page', query.page).set('size', query.size)
      .set('sortBy', query.sortBy).set('direction', query.direction);
    if (query.keyword?.trim()) params = params.set('keyword', query.keyword.trim());
    return this.http.get<FeePage>(this.url, { params });
  }
  get(id: number) { return this.http.get<FeeResponse>(`${this.url}/${id}`); }
  create(value: FeeRequest) { return this.http.post<FeeResponse>(this.url, value); }
  update(id: number, value: FeeRequest) { return this.http.put<FeeResponse>(`${this.url}/${id}`, value); }
  delete(id: number) { return this.http.delete<void>(`${this.url}/${id}`); }
}
