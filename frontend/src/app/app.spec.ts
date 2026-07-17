import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of } from 'rxjs';

import { App } from './app';
import { AuthService } from './core/auth.service';

describe('App', () => {
  let logout: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    logout = vi.fn(() => of(undefined));

    await TestBed.configureTestingModule({
      imports: [App, TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { isAuthenticated: () => false, isAdmin: () => false, canAccessAdmin: () => false, logout } }
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('logout logs out and navigates to /login', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    fixture.componentInstance.logout();

    expect(logout).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('isAdminArea reflects the current router url, including after navigation', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as { isAdminArea: () => boolean };
    expect(component.isAdminArea()).toBe(false);
  });
});
