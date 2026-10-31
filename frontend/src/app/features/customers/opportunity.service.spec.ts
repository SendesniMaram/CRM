import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { OpportunityService } from './opportunity.service';

describe('OpportunityService', () => {
  const url = 'http://localhost:8080/customer/api/opportunities';
  let service: OpportunityService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(OpportunityService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends pipeline search, stage and paging filters', () => {
    service.list({ page: 1, size: 25, keyword: ' renew ', stage: 'NEGOTIATION' }).subscribe();
    const request = http.expectOne(req => req.url === url);
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('25');
    expect(request.request.params.get('keyword')).toBe('renew');
    expect(request.request.params.get('stage')).toBe('NEGOTIATION');
    request.flush({ content: [], totalElements: 0, totalPages: 0, number: 1, size: 25 });
  });

  it('creates a stage change and retrieves its history', () => {
    service.changeStage(7, 'WON').subscribe();
    const change = http.expectOne(`${url}/7/stage`);
    expect(change.request.method).toBe('PATCH');
    expect(change.request.body).toEqual({ stage: 'WON' });
    change.flush({ id: 7, stage: 'WON' });

    service.history(7).subscribe();
    const history = http.expectOne(`${url}/7/history`);
    expect(history.request.method).toBe('GET');
    history.flush([]);
  });
});
