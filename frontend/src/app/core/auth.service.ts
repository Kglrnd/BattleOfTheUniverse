import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import { Observable, catchError, of, tap } from 'rxjs';

import { AppLang, isAppLang, persistLang } from './language';
import { UserView } from './models';

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  preferredLanguage?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly transloco = inject(TranslocoService);

  private readonly currentUserState = signal<UserView | null>(null);
  private readonly initializedState = signal(false);

  readonly currentUser = this.currentUserState.asReadonly();
  readonly initialized = this.initializedState.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUserState() !== null);
  readonly isAdmin = computed(() => this.currentUserState()?.role === 'ADMIN');
  readonly isModerator = computed(() => this.currentUserState()?.role === 'MODERATOR');
  readonly canAccessAdmin = computed(() => this.isAdmin() || this.isModerator());

  register(request: RegisterRequest): Observable<UserView> {
    return this.http.post<UserView>('/api/auth/register', request);
  }

  login(username: string, password: string): Observable<UserView> {
    const body = new HttpParams().set('username', username).set('password', password);
    return this.http.post<UserView>('/api/auth/login', body).pipe(tap((user) => this.applyUser(user)));
  }

  logout(): Observable<void> {
    return this.http
      .post<void>('/api/auth/logout', null)
      .pipe(tap(() => this.currentUserState.set(null)));
  }

  /** Called once on app startup to restore session state from the auth cookie, if any. */
  loadCurrentUser(): Observable<UserView | null> {
    return this.http.get<UserView>('/api/auth/me').pipe(
      tap((user) => {
        this.applyUser(user);
        this.initializedState.set(true);
      }),
      catchError(() => {
        this.currentUserState.set(null);
        this.initializedState.set(true);
        return of(null);
      })
    );
  }

  /** Persists the chosen language on the account so it follows the user across devices/logins. */
  updateLanguage(lang: AppLang): Observable<UserView> {
    return this.http.patch<UserView>('/api/auth/me/language', { language: lang }).pipe(tap((user) => this.currentUserState.set(user)));
  }

  /**
   * The account's saved language preference wins as soon as it's known (login, session
   * restore on app boot) - overriding whatever the browser/localStorage guessed pre-login,
   * so the UI language follows the account rather than the device.
   */
  private applyUser(user: UserView): void {
    this.currentUserState.set(user);
    if (isAppLang(user.preferredLanguage)) {
      this.transloco.setActiveLang(user.preferredLanguage);
      persistLang(user.preferredLanguage);
    }
  }
}
