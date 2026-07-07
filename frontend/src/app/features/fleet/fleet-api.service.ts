import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { DispatchRequest, FleetMovementView } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class FleetApiService {
  private readonly http = inject(HttpClient);

  dispatch(request: DispatchRequest): Observable<FleetMovementView> {
    return this.http.post<FleetMovementView>('/api/fleet/dispatch', request);
  }

  movements(): Observable<FleetMovementView[]> {
    return this.http.get<FleetMovementView[]>('/api/fleet/movements');
  }
}
