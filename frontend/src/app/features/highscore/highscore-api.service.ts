import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { HighscoreResponse } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class HighscoreApiService {
  private readonly http = inject(HttpClient);

  get(): Observable<HighscoreResponse> {
    return this.http.get<HighscoreResponse>('/api/highscore');
  }
}
