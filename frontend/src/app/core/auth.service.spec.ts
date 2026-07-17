import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TranslocoService } from '@jsverse/transloco';

import { AuthService } from './auth.service';
import { UserView } from './models';

function user(overrides: Partial<UserView> = {}): UserView {
  return { id: 1, username: 'player', email: 'p@example.com', role: 'PLAYER', preferredLanguage: 'en', ...overrides };
}

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let translocoStub: { setActiveLang: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    localStorage.clear();
    translocoStub = { setActiveLang: vi.fn() };
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: TranslocoService, useValue: translocoStub }]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('starts unauthenticated and uninitialized', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.initialized()).toBe(false);
    expect(service.currentUser()).toBeNull();
  });

  it('computes isAdmin/isModerator/canAccessAdmin from the current user role', () => {
    service.login('a', 'b').subscribe();
    httpMock.expectOne('/api/auth/login').flush(user({ role: 'ADMIN' }));
    expect(service.isAdmin()).toBe(true);
    expect(service.canAccessAdmin()).toBe(true);
    expect(service.isModerator()).toBe(false);
  });

  it('canAccessAdmin is true for moderators too', () => {
    service.login('a', 'b').subscribe();
    httpMock.expectOne('/api/auth/login').flush(user({ role: 'MODERATOR' }));
    expect(service.isModerator()).toBe(true);
    expect(service.canAccessAdmin()).toBe(true);
    expect(service.isAdmin()).toBe(false);
  });

  it('isAdmin/isModerator/canAccessAdmin are false (not throwing) when no user is logged in', () => {
    expect(service.isAdmin()).toBe(false);
    expect(service.isModerator()).toBe(false);
    expect(service.canAccessAdmin()).toBe(false);
  });

  it('register posts to /api/auth/register without mutating state', () => {
    let result: UserView | undefined;
    service.register({ username: 'a', email: 'a@b.com', password: 'pw' }).subscribe((u) => (result = u));

    const req = httpMock.expectOne('/api/auth/register');
    expect(req.request.method).toBe('POST');
    req.flush(user());

    expect(result).toEqual(user());
    expect(service.isAuthenticated()).toBe(false);
  });

  it('login applies the returned user and sets active language when it is a supported app lang', () => {
    service.login('player', 'secret').subscribe();

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush(user({ preferredLanguage: 'de' }));

    expect(service.isAuthenticated()).toBe(true);
    expect(service.currentUser()?.preferredLanguage).toBe('de');
    expect(translocoStub.setActiveLang).toHaveBeenCalledWith('de');
    expect(localStorage.getItem('lang')).toBe('de');
  });

  it('login does not touch transloco when the preferred language is not a supported app lang', () => {
    service.login('player', 'secret').subscribe();
    httpMock.expectOne('/api/auth/login').flush(user({ preferredLanguage: 'fr' }));

    expect(translocoStub.setActiveLang).not.toHaveBeenCalled();
  });

  it('logout clears the current user', () => {
    service.login('player', 'secret').subscribe();
    httpMock.expectOne('/api/auth/login').flush(user());
    expect(service.isAuthenticated()).toBe(true);

    service.logout().subscribe();
    httpMock.expectOne('/api/auth/logout').flush(null);

    expect(service.isAuthenticated()).toBe(false);
  });

  it('loadCurrentUser applies the user and marks initialized on success', () => {
    let result: UserView | null | undefined;
    service.loadCurrentUser().subscribe((u) => (result = u));

    httpMock.expectOne('/api/auth/me').flush(user());

    expect(result).toEqual(user());
    expect(service.isAuthenticated()).toBe(true);
    expect(service.initialized()).toBe(true);
  });

  it('loadCurrentUser clears state and still marks initialized on failure (no session)', () => {
    let result: UserView | null | undefined;
    service.loadCurrentUser().subscribe((u) => (result = u));

    httpMock.expectOne('/api/auth/me').flush('unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(result).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
    expect(service.initialized()).toBe(true);
  });

  it('updateLanguage patches the account and updates current user state', () => {
    let result: UserView | undefined;
    service.updateLanguage('de').subscribe((u) => (result = u));

    const req = httpMock.expectOne('/api/auth/me/language');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ language: 'de' });
    req.flush(user({ preferredLanguage: 'de' }));

    expect(result?.preferredLanguage).toBe('de');
    expect(service.currentUser()?.preferredLanguage).toBe('de');
  });
});
