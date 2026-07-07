import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { BuildingView, PlanetView, ResourceView, UpgradeResponse } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class UniverseApiService {
  private readonly http = inject(HttpClient);

  listPlanets(): Observable<PlanetView[]> {
    return this.http.get<PlanetView[]>('/api/planets');
  }

  getPlanet(planetId: number): Observable<PlanetView> {
    return this.http.get<PlanetView>(`/api/planets/${planetId}`);
  }

  getResources(planetId: number): Observable<ResourceView[]> {
    return this.http.get<ResourceView[]>(`/api/planets/${planetId}/resources`);
  }

  getBuildings(planetId: number): Observable<BuildingView[]> {
    return this.http.get<BuildingView[]>(`/api/planets/${planetId}/buildings`);
  }

  upgrade(planetId: number, buildingKey: string): Observable<UpgradeResponse> {
    return this.http.post<UpgradeResponse>(`/api/planets/${planetId}/buildings/${buildingKey}/upgrade`, null);
  }
}
