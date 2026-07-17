import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { SendMessageRequest } from '../../core/models';
import { MessagesApiService } from './messages-api.service';

describe('MessagesApiService', () => {
  let service: MessagesApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(MessagesApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('inbox fetches received messages', () => {
    service.inbox().subscribe();
    const req = httpMock.expectOne('/api/messages/inbox');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('sent fetches sent messages', () => {
    service.sent().subscribe();
    const req = httpMock.expectOne('/api/messages/sent');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('unreadCount fetches the unread message count', () => {
    service.unreadCount().subscribe();
    const req = httpMock.expectOne('/api/messages/unread-count');
    expect(req.request.method).toBe('GET');
    req.flush({ count: 0 });
  });

  it('send posts a new message', () => {
    const request: SendMessageRequest = { recipientUsername: 'foo', subject: 'hi', body: 'hello' };
    service.send(request).subscribe();
    const req = httpMock.expectOne('/api/messages');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({});
  });

  it('markRead posts to mark a message as read', () => {
    service.markRead(9).subscribe();
    const req = httpMock.expectOne('/api/messages/9/read');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush({});
  });
});
