import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ResearchApiService } from './research-api.service';

describe('ResearchApiService', () => {
  let service: ResearchApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ResearchApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('list fetches technologies', () => {
    service.list().subscribe();
    const req = httpMock.expectOne('/api/research');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('start posts to begin researching a technology', () => {
    service.start('laser_technology').subscribe();
    const req = httpMock.expectOne('/api/research/laser_technology/start');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush({});
  });

  it('listPlanetOptions fetches research planet options', () => {
    service.listPlanetOptions().subscribe();
    const req = httpMock.expectOne('/api/research/planets');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('activate posts to activate research on a planet', () => {
    service.activate(4).subscribe();
    const req = httpMock.expectOne('/api/research/planets/4/activate');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush({});
  });
});
