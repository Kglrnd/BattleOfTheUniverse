import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AdminPlanetsApiService } from './admin-planets-api.service';

describe('AdminPlanetsApiService', () => {
  let service: AdminPlanetsApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AdminPlanetsApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('list fetches all planets', () => {
    service.list().subscribe();
    const req = httpMock.expectOne('/api/admin/planets');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
