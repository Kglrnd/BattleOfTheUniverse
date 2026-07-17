import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { adminGuard, authGuard, staffGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('auth guards', () => {
  let isAuthenticated: boolean;
  let isAdmin: boolean;
  let canAccessAdmin: boolean;
  let router: Router;
  let urlTree: unknown;

  beforeEach(() => {
    isAuthenticated = false;
    isAdmin = false;
    canAccessAdmin = false;

    TestBed.configureTestingModule({
      providers: [
        {
          provide: AuthService,
          useValue: {
            isAuthenticated: () => isAuthenticated,
            isAdmin: () => isAdmin,
            canAccessAdmin: () => canAccessAdmin
          }
        }
      ]
    });

    router = TestBed.inject(Router);
    urlTree = {};
    vi.spyOn(router, 'createUrlTree').mockReturnValue(urlTree as ReturnType<Router['createUrlTree']>);
  });

  describe('authGuard', () => {
    it('allows navigation when authenticated', () => {
      isAuthenticated = true;
      const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));
      expect(result).toBe(true);
    });

    it('redirects to /login when not authenticated', () => {
      const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));
      expect(router.createUrlTree).toHaveBeenCalledWith(['/login']);
      expect(result).toBe(urlTree);
    });
  });

  describe('adminGuard', () => {
    it('redirects to /login when not authenticated', () => {
      const result = TestBed.runInInjectionContext(() => adminGuard({} as never, {} as never));
      expect(router.createUrlTree).toHaveBeenCalledWith(['/login']);
      expect(result).toBe(urlTree);
    });

    it('allows navigation when authenticated and admin', () => {
      isAuthenticated = true;
      isAdmin = true;
      const result = TestBed.runInInjectionContext(() => adminGuard({} as never, {} as never));
      expect(result).toBe(true);
    });

    it('redirects to /universe when authenticated but not admin', () => {
      isAuthenticated = true;
      isAdmin = false;
      const result = TestBed.runInInjectionContext(() => adminGuard({} as never, {} as never));
      expect(router.createUrlTree).toHaveBeenCalledWith(['/universe']);
      expect(result).toBe(urlTree);
    });
  });

  describe('staffGuard', () => {
    it('redirects to /login when not authenticated', () => {
      const result = TestBed.runInInjectionContext(() => staffGuard({} as never, {} as never));
      expect(router.createUrlTree).toHaveBeenCalledWith(['/login']);
      expect(result).toBe(urlTree);
    });

    it('allows navigation when authenticated and can access admin', () => {
      isAuthenticated = true;
      canAccessAdmin = true;
      const result = TestBed.runInInjectionContext(() => staffGuard({} as never, {} as never));
      expect(result).toBe(true);
    });

    it('redirects to /universe when authenticated but cannot access admin', () => {
      isAuthenticated = true;
      canAccessAdmin = false;
      const result = TestBed.runInInjectionContext(() => staffGuard({} as never, {} as never));
      expect(router.createUrlTree).toHaveBeenCalledWith(['/universe']);
      expect(result).toBe(urlTree);
    });
  });
});
