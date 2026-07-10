import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../../core/auth.service';
import { AdminUserView, Role } from '../../core/models';
import { AdminUsersApiService } from './admin-users-api.service';

@Component({
  selector: 'app-admin-users',
  imports: [FormsModule],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.css'
})
export class AdminUsersComponent {
  private readonly api = inject(AdminUsersApiService);
  private readonly auth = inject(AuthService);

  protected readonly roles: Role[] = ['PLAYER', 'MODERATOR', 'ADMIN'];
  protected readonly users = signal<AdminUserView[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly savingUserId = signal<number | null>(null);

  protected readonly currentUserId = this.auth.currentUser()?.id ?? null;
  protected readonly isAdmin = this.auth.isAdmin;

  constructor() {
    this.api.list().subscribe({
      next: (users) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message ?? 'Failed to load users.');
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
        this.users.update((current) => current.map((u) => (u.id === updated.id ? updated : u)));
      },
      error: (err) => {
        this.savingUserId.set(null);
        this.errorMessage.set(err.error?.message ?? 'Failed to change role.');
      }
    });
  }

  formatCreatedAt(createdAt: string): string {
    return new Date(createdAt).toLocaleString();
  }
}
