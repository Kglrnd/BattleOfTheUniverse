import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AdminUsersApiService } from './admin-users-api.service';

describe('AdminUsersApiService', () => {
  let service: AdminUsersApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AdminUsersApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('list fetches all users', () => {
    service.list().subscribe();
    const req = httpMock.expectOne('/api/admin/users');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('changeRole patches a user role', () => {
    service.changeRole(7, 'MODERATOR').subscribe();
    const req = httpMock.expectOne('/api/admin/users/7/role');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ role: 'MODERATOR' });
    req.flush({});
  });
});
