import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { TranslocoHttpLoader } from './transloco-loader';

describe('TranslocoHttpLoader', () => {
  it('requests translations from /i18n/<lang>.json', () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    const loader = TestBed.inject(TranslocoHttpLoader);
    const httpMock = TestBed.inject(HttpTestingController);

    let result: unknown;
    loader.getTranslation('de').subscribe((r) => (result = r));

    httpMock.expectOne('/i18n/de.json').flush({ hello: 'Hallo' });
    httpMock.verify();

    expect(result).toEqual({ hello: 'Hallo' });
  });
});
