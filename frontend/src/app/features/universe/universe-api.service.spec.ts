import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { UniverseApiService } from './universe-api.service';

describe('UniverseApiService', () => {
  let service: UniverseApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(UniverseApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listPlanets fetches all owned planets', () => {
    service.listPlanets().subscribe();
    const req = httpMock.expectOne('/api/planets');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('getHomePlanet fetches the homeworld', () => {
    service.getHomePlanet().subscribe();
    const req = httpMock.expectOne('/api/planets/home');
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('getPlanet fetches a single planet by id', () => {
    service.getPlanet(3).subscribe();
    const req = httpMock.expectOne('/api/planets/3');
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('byOwner fetches planets by owner id', () => {
    service.byOwner(9).subscribe();
    const req = httpMock.expectOne('/api/planets/by-owner/9');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('renamePlanet patches the planet name', () => {
    service.renamePlanet(3, 'New Name').subscribe();
    const req = httpMock.expectOne('/api/planets/3/name');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ name: 'New Name' });
    req.flush({});
  });

  it('getSystem fetches a galaxy/system view', () => {
    service.getSystem(2, 5).subscribe();
    const req = httpMock.expectOne('/api/systems/2/5');
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('getResources fetches planet resources', () => {
    service.getResources(3).subscribe();
    const req = httpMock.expectOne('/api/planets/3/resources');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('getBuildings fetches planet buildings', () => {
    service.getBuildings(3).subscribe();
    const req = httpMock.expectOne('/api/planets/3/buildings');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('upgrade posts an upgrade order for a building', () => {
    service.upgrade(3, 'metal_mine').subscribe();
    const req = httpMock.expectOne('/api/planets/3/buildings/metal_mine/upgrade');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush({});
  });

  it('getShips fetches the shipyard view for a planet', () => {
    service.getShips(3).subscribe();
    const req = httpMock.expectOne('/api/planets/3/ships');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('buildShips posts a ship build order with quantity', () => {
    service.buildShips(3, 'light_fighter', 5).subscribe();
    const req = httpMock.expectOne('/api/planets/3/ships/light_fighter/build');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ quantity: 5 });
    req.flush({});
  });
});
