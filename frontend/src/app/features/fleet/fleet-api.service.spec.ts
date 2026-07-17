import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DispatchRequest, DriveOptionsRequest } from '../../core/models';
import { FleetApiService } from './fleet-api.service';

describe('FleetApiService', () => {
  let service: FleetApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(FleetApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('dispatch posts the dispatch request', () => {
    const request: DispatchRequest = {
      originPlanetId: 1,
      targetGalaxy: 1,
      targetSystem: 1,
      targetPosition: 2,
      missionType: 'ATTACK',
      ships: [],
      driveKey: 'combustion_drive',
      cargo: []
    };
    service.dispatch(request).subscribe();
    const req = httpMock.expectOne('/api/fleet/dispatch');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({});
  });

  it('movements fetches own fleet movements', () => {
    service.movements().subscribe();
    const req = httpMock.expectOne('/api/fleet/movements');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('incomingMovements fetches incoming fleet movements', () => {
    service.incomingMovements().subscribe();
    const req = httpMock.expectOne('/api/fleet/movements/incoming');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('driveOptions posts the drive options request', () => {
    const request: DriveOptionsRequest = { originPlanetId: 1, targetGalaxy: 1, targetSystem: 1, targetPosition: 2, ships: [] };
    service.driveOptions(request).subscribe();
    const req = httpMock.expectOne('/api/fleet/drive-options');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush([]);
  });
});
