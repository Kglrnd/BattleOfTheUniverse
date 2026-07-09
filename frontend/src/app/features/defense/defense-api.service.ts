import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { TowerBuildResponse, TowerView } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class DefenseApiService {
  private readonly http = inject(HttpClient);

  list(planetId: number): Observable<TowerView[]> {
    return this.http.get<TowerView[]>(`/api/planets/${planetId}/defenses`);
  }

  build(planetId: number, towerKey: string, quantity: number): Observable<TowerBuildResponse> {
    return this.http.post<TowerBuildResponse>(`/api/planets/${planetId}/defenses/${towerKey}/build`, { quantity });
  }
}
