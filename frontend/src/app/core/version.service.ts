import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';

interface VersionResponse {
  version: string;
}

@Injectable({ providedIn: 'root' })
export class VersionService {
  private readonly http = inject(HttpClient);

  getVersion(): Observable<string | null> {
    return this.http.get<VersionResponse>('/api/version').pipe(
      map((res) => res.version),
      catchError(() => of(null))
    );
  }
}
