import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AdminPlanetView } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class AdminPlanetsApiService {
  private readonly http = inject(HttpClient);

  list(): Observable<AdminPlanetView[]> {
    return this.http.get<AdminPlanetView[]>('/api/admin/planets');
  }
}
