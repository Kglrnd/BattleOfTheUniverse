import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ResearchStartResponse, TechnologyView } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class ResearchApiService {
  private readonly http = inject(HttpClient);

  list(): Observable<TechnologyView[]> {
    return this.http.get<TechnologyView[]>('/api/research');
  }

  start(technologyKey: string, planetId: number): Observable<ResearchStartResponse> {
    return this.http.post<ResearchStartResponse>(`/api/research/${technologyKey}/start`, { planetId });
  }
}
