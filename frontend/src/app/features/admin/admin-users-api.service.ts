import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AdminUserView, Role } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class AdminUsersApiService {
  private readonly http = inject(HttpClient);

  list(): Observable<AdminUserView[]> {
    return this.http.get<AdminUserView[]>('/api/admin/users');
  }

  changeRole(id: number, role: Role): Observable<AdminUserView> {
    return this.http.patch<AdminUserView>(`/api/admin/users/${id}/role`, { role });
  }
}
