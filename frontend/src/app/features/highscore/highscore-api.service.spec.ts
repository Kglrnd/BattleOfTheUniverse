import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { HighscoreApiService } from './highscore-api.service';

describe('HighscoreApiService', () => {
  let service: HighscoreApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(HighscoreApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('get fetches the highscore', () => {
    service.get().subscribe();
    const req = httpMock.expectOne('/api/highscore');
    expect(req.request.method).toBe('GET');
    req.flush({ entries: [] });
  });
});
