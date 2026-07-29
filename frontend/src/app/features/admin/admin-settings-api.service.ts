import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AppSettingsView } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class AdminSettingsApiService {
  private readonly http = inject(HttpClient);

  get(): Observable<AppSettingsView> {
    return this.http.get<AppSettingsView>('/api/admin/settings');
  }

  updateRegistrationEnabled(registrationEnabled: boolean): Observable<AppSettingsView> {
    return this.http.patch<AppSettingsView>('/api/admin/settings/registration', { registrationEnabled });
  }
}
