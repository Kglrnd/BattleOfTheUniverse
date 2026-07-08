import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { DispatchRequest, FleetMovementView, TravelTimeView } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class FleetApiService {
  private readonly http = inject(HttpClient);

  dispatch(request: DispatchRequest): Observable<FleetMovementView> {
    return this.http.post<FleetMovementView>('/api/fleet/dispatch', request);
  }

  movements(): Observable<FleetMovementView[]> {
    return this.http.get<FleetMovementView[]>('/api/fleet/movements');
  }

  travelTime(
    originPlanetId: number,
    shipKey: string,
    targetGalaxy: number,
    targetSystem: number,
    targetPosition: number
  ): Observable<TravelTimeView> {
    const params = new HttpParams()
      .set('originPlanetId', originPlanetId)
      .set('shipKey', shipKey)
      .set('targetGalaxy', targetGalaxy)
      .set('targetSystem', targetSystem)
      .set('targetPosition', targetPosition);
    return this.http.get<TravelTimeView>('/api/fleet/travel-time', { params });
  }
}
