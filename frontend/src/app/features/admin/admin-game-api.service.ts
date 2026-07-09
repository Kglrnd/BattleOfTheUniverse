import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AdminGameApiService {
  private readonly http = inject(HttpClient);

  reset(): Observable<void> {
    return this.http.post<void>('/api/admin/game/reset', null);
  }
}
