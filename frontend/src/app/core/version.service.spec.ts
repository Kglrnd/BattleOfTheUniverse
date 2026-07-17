import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { VersionService } from './version.service';

describe('VersionService', () => {
  let service: VersionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(VersionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('returns the version string from the API', () => {
    let result: string | null | undefined;
    service.getVersion().subscribe((v) => (result = v));

    httpMock.expectOne('/api/version').flush({ version: '1.2.3' });

    expect(result).toBe('1.2.3');
  });

  it('returns null when the request fails', () => {
    let result: string | null | undefined;
    service.getVersion().subscribe((v) => (result = v));

    httpMock.expectOne('/api/version').flush('error', { status: 500, statusText: 'Server Error' });

    expect(result).toBeNull();
  });
});
