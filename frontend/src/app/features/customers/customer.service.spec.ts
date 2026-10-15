import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { CustomerService } from './customer.service';

describe('CustomerService', () => {
  let service: CustomerService;
  let http: HttpTestingController;
  const url = 'http://localhost:8080/customer/api/customers';
  const request = { name: 'Client test', email: null, phone: null, company: null, address: null };
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(CustomerService); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('uses server paging and supported sorting/search through Gateway', () => {
    service.list({ page: 2, size: 25, sortBy: 'company', direction: 'desc', keyword: ' test ' }).subscribe();
    const call = http.expectOne(r => r.url === url);
    expect(call.request.method).toBe('GET');
    expect(call.request.params.get('page')).toBe('2');
    expect(call.request.params.get('size')).toBe('25');
    expect(call.request.params.get('sortBy')).toBe('company');
    expect(call.request.params.get('direction')).toBe('desc');
    expect(call.request.params.get('keyword')).toBe('test');
    call.flush({ content: [], totalElements: 0 });
  });
  it('always sends sortBy and omits blank search to receive a Page instead of an array', () => {
    service.list({ page: 0, size: 10, sortBy: 'name', direction: 'asc', keyword: ' ' }).subscribe();
    const call = http.expectOne(r => r.url === url);
    expect(call.request.params.get('sortBy')).toBe('name');
    expect(call.request.params.has('keyword')).toBeFalse(); call.flush({ content: [] });
  });
  it('reads a detail through Gateway', () => {
    service.get(4).subscribe(); const call = http.expectOne(url + '/4');
    expect(call.request.method).toBe('GET'); call.flush({ id: 4, ...request });
  });
  it('creates using only actual request fields', () => {
    service.create(request).subscribe(); const call = http.expectOne(url);
    expect(call.request.method).toBe('POST'); expect(call.request.body).toEqual(request); call.flush({ id: 4, ...request });
  });
  it('updates the requested customer', () => {
    service.update(4, request).subscribe(); const call = http.expectOne(url + '/4');
    expect(call.request.method).toBe('PUT'); expect(call.request.body).toEqual(request); call.flush({ id: 4, ...request });
  });
  it('deletes the requested customer', () => {
    service.delete(4).subscribe(); const call = http.expectOne(url + '/4');
    expect(call.request.method).toBe('DELETE'); call.flush(null);
  });
});
