import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AdminSettingsApiService } from './admin-settings-api.service';

describe('AdminSettingsApiService', () => {
  let service: AdminSettingsApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AdminSettingsApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('get fetches /api/admin/settings', () => {
    service.get().subscribe();
    const req = httpMock.expectOne('/api/admin/settings');
    expect(req.request.method).toBe('GET');
    req.flush({ registrationEnabled: true });
  });

  it('updateRegistrationEnabled patches /api/admin/settings/registration', () => {
    service.updateRegistrationEnabled(false).subscribe();
    const req = httpMock.expectOne('/api/admin/settings/registration');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ registrationEnabled: false });
    req.flush({ registrationEnabled: false });
  });
});
