import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AdminCatalogApiService } from './admin-catalog-api.service';

describe('AdminCatalogApiService', () => {
  let service: AdminCatalogApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AdminCatalogApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getData fetches catalog data for a type', () => {
    service.getData('buildings').subscribe();
    const req = httpMock.expectOne('/api/admin/catalog/buildings');
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('getSchema fetches the JSON schema for a type', () => {
    service.getSchema('ships').subscribe();
    const req = httpMock.expectOne('/api/admin/catalog/ships/schema');
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('save puts updated catalog data for a type', () => {
    const data = { key: 'value' };
    service.save('technologies', data).subscribe();
    const req = httpMock.expectOne('/api/admin/catalog/technologies');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(data);
    req.flush({});
  });
});
