import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { DispatchRequest, DriveOptionView, FleetMovementView, IncomingMovementView } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class FleetApiService {
  private readonly http = inject(HttpClient);

  dispatch(request: DispatchRequest): Observable<FleetMovementView> {
    return this.http.post<FleetMovementView>('/api/fleet/dispatch', request);
  }

  movements(): Observable<FleetMovementView[]> {
    return this.http.get<FleetMovementView[]>('/api/fleet/movements');
  }

  incomingMovements(): Observable<IncomingMovementView[]> {
    return this.http.get<IncomingMovementView[]>('/api/fleet/movements/incoming');
  }

  driveOptions(
    originPlanetId: number,
    shipKey: string,
    targetGalaxy: number,
    targetSystem: number,
    targetPosition: number
  ): Observable<DriveOptionView[]> {
    const params = new HttpParams()
      .set('originPlanetId', originPlanetId)
      .set('shipKey', shipKey)
      .set('targetGalaxy', targetGalaxy)
      .set('targetSystem', targetSystem)
      .set('targetPosition', targetPosition);
    return this.http.get<DriveOptionView[]>('/api/fleet/drive-options', { params });
  }
}
