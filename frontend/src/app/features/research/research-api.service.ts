import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ResearchPlanetOption, ResearchStartResponse, TechnologyView } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class ResearchApiService {
  private readonly http = inject(HttpClient);

  list(): Observable<TechnologyView[]> {
    return this.http.get<TechnologyView[]>('/api/research');
  }

  start(technologyKey: string): Observable<ResearchStartResponse> {
    return this.http.post<ResearchStartResponse>(`/api/research/${technologyKey}/start`, {});
  }

  listPlanetOptions(): Observable<ResearchPlanetOption[]> {
    return this.http.get<ResearchPlanetOption[]>('/api/research/planets');
  }

  activate(planetId: number): Observable<ResearchPlanetOption> {
    return this.http.post<ResearchPlanetOption>(`/api/research/planets/${planetId}/activate`, {});
  }
}
