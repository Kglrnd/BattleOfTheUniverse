import { TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../core/auth.service';
import { AdminUserView, UserView } from '../../core/models';
import { AdminUsersApiService } from './admin-users-api.service';
import { AdminUsersComponent } from './admin-users.component';

function user(id: number, role: AdminUserView['role'] = 'PLAYER'): AdminUserView {
  return { id, username: `user${id}`, email: `user${id}@example.com`, role, active: true, createdAt: '2026-01-01T00:00:00Z' };
}

describe('AdminUsersComponent', () => {
  async function setup(list: ReturnType<typeof vi.fn>, changeRole: ReturnType<typeof vi.fn> = vi.fn()) {
    await TestBed.configureTestingModule({
      imports: [
        AdminUsersComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        { provide: AdminUsersApiService, useValue: { list, changeRole } },
        { provide: AuthService, useValue: { currentUser: () => ({ id: 1 }) as UserView, isAdmin: () => true } }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(AdminUsersComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('loads and exposes the user list', async () => {
    const fixture = await setup(vi.fn(() => of([user(1), user(2)])));
    const component = fixture.componentInstance as unknown as { users: () => AdminUserView[] };
    expect(component.users().length).toBe(2);
  });

  it('surfaces a load error message', async () => {
    const fixture = await setup(vi.fn(() => throwError(() => Object.assign(new Error('boom'), { error: { message: 'load failed' } }))));
    const component = fixture.componentInstance as unknown as { errorMessage: () => string | null };
    expect(component.errorMessage()).toBe('load failed');
  });

  it('changeRole updates the role and refreshes the local list', async () => {
    const changeRole = vi.fn(() => of(user(1, 'ADMIN')));
    const fixture = await setup(vi.fn(() => of([user(1)])), changeRole);

    fixture.componentInstance.changeRole(user(1), 'ADMIN');

    expect(changeRole).toHaveBeenCalledWith(1, 'ADMIN');
    const component = fixture.componentInstance as unknown as { users: () => AdminUserView[]; savingUserId: () => number | null };
    expect(component.users()[0].role).toBe('ADMIN');
    expect(component.savingUserId()).toBeNull();
  });

  it('does nothing when the requested role matches the current role', async () => {
    const changeRole = vi.fn();
    const fixture = await setup(vi.fn(() => of([user(1)])), changeRole);

    fixture.componentInstance.changeRole(user(1, 'PLAYER'), 'PLAYER');

    expect(changeRole).not.toHaveBeenCalled();
  });

  it('surfaces an error when changing role fails', async () => {
    const changeRole = vi.fn(() => throwError(() => ({ error: { message: 'denied' } })));
    const fixture = await setup(vi.fn(() => of([user(1)])), changeRole);

    fixture.componentInstance.changeRole(user(1), 'ADMIN');

    const component = fixture.componentInstance as unknown as { errorMessage: () => string | null };
    expect(component.errorMessage()).toBe('denied');
  });

  it('formatCreatedAt formats the ISO timestamp for display', async () => {
    const fixture = await setup(vi.fn(() => of([])));
    expect(fixture.componentInstance.formatCreatedAt('2026-01-01T00:00:00Z')).toBe(new Date('2026-01-01T00:00:00Z').toLocaleString());
  });
});
