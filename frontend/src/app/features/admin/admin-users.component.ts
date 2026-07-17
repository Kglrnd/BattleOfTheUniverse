import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';

import { AuthService } from '../../core/auth.service';
import { AdminUserView, Role } from '../../core/models';
import { AdminUsersApiService } from './admin-users-api.service';

@Component({
  selector: 'app-admin-users',
  imports: [FormsModule, TranslocoDirective],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.css'
})
export class AdminUsersComponent {
  private readonly api = inject(AdminUsersApiService);
  private readonly auth = inject(AuthService);
  private readonly transloco = inject(TranslocoService);

  protected readonly roles: Role[] = ['PLAYER', 'MODERATOR', 'ADMIN'];

  private readonly usersResource = rxResource({ stream: () => this.api.list() });
  // Reading .value() on an errored resource with no prior successful value throws, so this
  // must check for an error first rather than let the template call .value() unconditionally.
  protected readonly users = computed(() => (this.usersResource.error() ? [] : (this.usersResource.value() ?? [])));
  protected readonly loading = this.usersResource.isLoading;
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly savingUserId = signal<number | null>(null);

  protected readonly currentUserId = this.auth.currentUser()?.id ?? null;
  protected readonly isAdmin = this.auth.isAdmin;

  constructor() {
    effect(() => {
      const error = this.usersResource.error() as HttpErrorResponse | undefined;
      if (error) {
        this.errorMessage.set(error.error?.message ?? this.transloco.translate('admin.users.loadError'));
      }
    });
  }

  changeRole(user: AdminUserView, role: Role): void {
    if (role === user.role || this.savingUserId()) {
      return;
    }
    this.errorMessage.set(null);
    this.savingUserId.set(user.id);
    this.api.changeRole(user.id, role).subscribe({
      next: (updated) => {
        this.savingUserId.set(null);
        this.usersResource.update((current) => (current ?? []).map((u) => (u.id === updated.id ? updated : u)));
      },
      error: (err) => {
        this.savingUserId.set(null);
        this.errorMessage.set(err.error?.message ?? this.transloco.translate('admin.users.changeRoleError'));
      }
    });
  }

  formatCreatedAt(createdAt: string): string {
    return new Date(createdAt).toLocaleString();
  }
}
