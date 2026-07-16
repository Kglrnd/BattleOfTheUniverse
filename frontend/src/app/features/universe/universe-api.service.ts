import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  BuildingView,
  PlanetView,
  ResourceView,
  ShipyardBuildResponse,
  ShipyardView,
  SystemView,
  UpgradeResponse
} from '../../core/models';

@Injectable({ providedIn: 'root' })
export class UniverseApiService {
  private readonly http = inject(HttpClient);

  listPlanets(): Observable<PlanetView[]> {
    return this.http.get<PlanetView[]>('/api/planets');
  }

  getHomePlanet(): Observable<PlanetView> {
    return this.http.get<PlanetView>('/api/planets/home');
  }

  getPlanet(planetId: number): Observable<PlanetView> {
    return this.http.get<PlanetView>(`/api/planets/${planetId}`);
  }

  byOwner(ownerId: number): Observable<PlanetView[]> {
    return this.http.get<PlanetView[]>(`/api/planets/by-owner/${ownerId}`);
  }

  renamePlanet(planetId: number, name: string): Observable<PlanetView> {
    return this.http.patch<PlanetView>(`/api/planets/${planetId}/name`, { name });
  }

  getSystem(galaxy: number, system: number): Observable<SystemView> {
    return this.http.get<SystemView>(`/api/systems/${galaxy}/${system}`);
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

  getShips(planetId: number): Observable<ShipyardView[]> {
    return this.http.get<ShipyardView[]>(`/api/planets/${planetId}/ships`);
  }

  buildShips(planetId: number, shipKey: string, quantity: number): Observable<ShipyardBuildResponse> {
    return this.http.post<ShipyardBuildResponse>(`/api/planets/${planetId}/ships/${shipKey}/build`, { quantity });
  }
}
