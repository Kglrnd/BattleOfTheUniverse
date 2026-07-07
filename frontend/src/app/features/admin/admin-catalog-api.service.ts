import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AdminCatalogApiService {
  private readonly http = inject(HttpClient);

  getData(type: string): Observable<unknown> {
    return this.http.get(`/api/admin/catalog/${type}`);
  }

  getSchema(type: string): Observable<unknown> {
    return this.http.get(`/api/admin/catalog/${type}/schema`);
  }

  save(type: string, data: unknown): Observable<unknown> {
    return this.http.put(`/api/admin/catalog/${type}`, data);
  }
}
