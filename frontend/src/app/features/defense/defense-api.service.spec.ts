import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DefenseApiService } from './defense-api.service';

describe('DefenseApiService', () => {
  let service: DefenseApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(DefenseApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('list fetches defenses for a planet', () => {
    service.list(5).subscribe();
    const req = httpMock.expectOne('/api/planets/5/defenses');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('build posts a build order with quantity', () => {
    service.build(5, 'rocket_launcher', 3).subscribe();
    const req = httpMock.expectOne('/api/planets/5/defenses/rocket_launcher/build');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ quantity: 3 });
    req.flush({});
  });
});
