import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { MessageView, SendMessageRequest, UnreadCountView } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class MessagesApiService {
  private readonly http = inject(HttpClient);

  inbox(): Observable<MessageView[]> {
    return this.http.get<MessageView[]>('/api/messages/inbox');
  }

  sent(): Observable<MessageView[]> {
    return this.http.get<MessageView[]>('/api/messages/sent');
  }

  unreadCount(): Observable<UnreadCountView> {
    return this.http.get<UnreadCountView>('/api/messages/unread-count');
  }

  send(request: SendMessageRequest): Observable<MessageView> {
    return this.http.post<MessageView>('/api/messages', request);
  }

  markRead(id: number): Observable<MessageView> {
    return this.http.post<MessageView>(`/api/messages/${id}/read`, null);
  }
}
