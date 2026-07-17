import { ApplicationInitStatus, isDevMode } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TRANSLOCO_CONFIG } from '@jsverse/transloco';
import { of } from 'rxjs';

import { appConfig } from './app.config';
import { AuthService } from './core/auth.service';
import { AVAILABLE_LANGS, detectInitialLang } from './core/language';

describe('appConfig', () => {
  it('configures Transloco with the detected initial language and app-wide defaults', async () => {
    const loadCurrentUser = vi.fn(() => of(null));
    await TestBed.configureTestingModule({
      providers: [...appConfig.providers, provideHttpClientTesting(), { provide: AuthService, useValue: { loadCurrentUser } }]
    }).compileComponents();

    const translocoConfig = TestBed.inject(TRANSLOCO_CONFIG);
    expect(translocoConfig.availableLangs).toEqual(AVAILABLE_LANGS);
    expect(translocoConfig.defaultLang).toBe(detectInitialLang());
    expect(translocoConfig.fallbackLang).toBe('en');
    expect(translocoConfig.reRenderOnLangChange).toBe(true);
    expect(translocoConfig.prodMode).toBe(!isDevMode());
  });

  it('runs the app initializer, which loads the current user session', async () => {
    const loadCurrentUser = vi.fn(() => of(null));
    await TestBed.configureTestingModule({
      providers: [...appConfig.providers, provideHttpClientTesting(), { provide: AuthService, useValue: { loadCurrentUser } }]
    }).compileComponents();

    await TestBed.inject(ApplicationInitStatus).donePromise;

    expect(loadCurrentUser).toHaveBeenCalled();
  });

  it('attaches the configured XSRF header, populated from the configured cookie name', async () => {
    document.cookie = 'XSRF-TOKEN=test-token-value';
    const loadCurrentUser = vi.fn(() => of(null));
    await TestBed.configureTestingModule({
      providers: [...appConfig.providers, provideHttpClientTesting(), { provide: AuthService, useValue: { loadCurrentUser } }]
    }).compileComponents();

    const http = TestBed.inject(HttpClient);
    const httpMock = TestBed.inject(HttpTestingController);

    http.post('/api/some-action', {}).subscribe();
    const req = httpMock.expectOne('/api/some-action');
    expect(req.request.headers.get('X-XSRF-TOKEN')).toBe('test-token-value');
    req.flush({});
    httpMock.verify();

    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 UTC';
  });
});
