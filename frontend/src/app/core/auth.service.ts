import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, of, tap } from 'rxjs';

import { UserView } from './models';

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly currentUserState = signal<UserView | null>(null);
  private readonly initializedState = signal(false);

  readonly currentUser = this.currentUserState.asReadonly();
  readonly initialized = this.initializedState.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUserState() !== null);
  readonly isAdmin = computed(() => this.currentUserState()?.role === 'ADMIN');

  register(request: RegisterRequest): Observable<UserView> {
    return this.http.post<UserView>('/api/auth/register', request);
  }

  login(username: string, password: string): Observable<UserView> {
    const body = new HttpParams().set('username', username).set('password', password);
    return this.http
      .post<UserView>('/api/auth/login', body)
      .pipe(tap((user) => this.currentUserState.set(user)));
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
        this.currentUserState.set(user);
        this.initializedState.set(true);
      }),
      catchError(() => {
        this.currentUserState.set(null);
        this.initializedState.set(true);
        return of(null);
      })
    );
  }
}
