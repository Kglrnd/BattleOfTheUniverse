import { TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';

import { MessageView } from '../../core/models';
import { MessagesApiService } from './messages-api.service';
import { MessagesComponent } from './messages.component';

function message(overrides: Partial<MessageView> = {}): MessageView {
  return {
    id: 1,
    senderUsername: 'alice',
    recipientUsername: 'bob',
    subject: 'Hi',
    body: 'Hello there',
    type: 'PLAYER',
    sentAt: '2026-01-01T00:00:00Z',
    readAt: null,
    ...overrides
  };
}

describe('MessagesComponent', () => {
  let inbox: ReturnType<typeof vi.fn>;
  let sent: ReturnType<typeof vi.fn>;
  let markRead: ReturnType<typeof vi.fn>;
  let send: ReturnType<typeof vi.fn>;

  async function setup(inboxMessages: MessageView[] = [], sentMessages: MessageView[] = []) {
    inbox = vi.fn(() => of(inboxMessages));
    sent = vi.fn(() => of(sentMessages));
    markRead = vi.fn(() => of(message()));
    send = vi.fn(() => of(message()));

    await TestBed.configureTestingModule({
      imports: [
        MessagesComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [{ provide: MessagesApiService, useValue: { inbox, sent, markRead, send, unreadCount: vi.fn() } }]
    }).compileComponents();

    const fixture = TestBed.createComponent(MessagesComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('currentMessages returns the inbox by default and switches with setTab', async () => {
    const fixture = await setup([message({ id: 1 })], [message({ id: 2 })]);
    expect(fixture.componentInstance.currentMessages().map((m) => m.id)).toEqual([1]);

    fixture.componentInstance.setTab('sent');
    expect(fixture.componentInstance.currentMessages().map((m) => m.id)).toEqual([2]);
  });

  it('setTab clears the selected message', async () => {
    const fixture = await setup([message({ id: 1 })]);
    fixture.componentInstance.select(message({ id: 1, readAt: '2026-01-01T00:00:00Z' }));
    fixture.componentInstance.setTab('sent');

    const component = fixture.componentInstance as unknown as { selected: () => MessageView | null };
    expect(component.selected()).toBeNull();
  });

  it('unreadCount counts unread inbox messages', async () => {
    const fixture = await setup([message({ id: 1, readAt: null }), message({ id: 2, readAt: '2026-01-01T00:00:00Z' })]);
    expect(fixture.componentInstance.unreadCount()).toBe(1);
  });

  it('select marks an unread inbox message as read', async () => {
    const fixture = await setup([message({ id: 1, readAt: null })]);
    fixture.componentInstance.select(message({ id: 1, readAt: null }));

    expect(markRead).toHaveBeenCalledWith(1);
  });

  it('select does not mark an already-read message as read', async () => {
    const fixture = await setup([message({ id: 1, readAt: '2026-01-01T00:00:00Z' })]);
    fixture.componentInstance.select(message({ id: 1, readAt: '2026-01-01T00:00:00Z' }));

    expect(markRead).not.toHaveBeenCalled();
  });

  it('select does not mark as read while viewing the sent tab', async () => {
    const fixture = await setup([], [message({ id: 2, readAt: null })]);
    fixture.componentInstance.setTab('sent');
    fixture.componentInstance.select(message({ id: 2, readAt: null }));

    expect(markRead).not.toHaveBeenCalled();
  });

  it('toggleCompose flips composing state and clears errors', async () => {
    const fixture = await setup();
    const component = fixture.componentInstance as unknown as { composing: () => boolean; errorMessage: { set: (v: string | null) => void } };
    component.errorMessage.set('boom');

    fixture.componentInstance.toggleCompose();

    expect(component.composing()).toBe(true);
    expect((fixture.componentInstance as unknown as { errorMessage: () => string | null }).errorMessage()).toBeNull();
  });

  it('does not submit an invalid compose form', async () => {
    const fixture = await setup();
    fixture.componentInstance.submitCompose();
    expect(send).not.toHaveBeenCalled();
  });

  it('submits a valid compose form and resets on success', async () => {
    const fixture = await setup();
    fixture.componentInstance['composeForm'].setValue({ recipientUsername: 'bob', subject: 'Hi', body: 'Hello' });

    fixture.componentInstance.submitCompose();

    expect(send).toHaveBeenCalledWith({ recipientUsername: 'bob', subject: 'Hi', body: 'Hello' });
    const component = fixture.componentInstance as unknown as { composing: () => boolean; sending: () => boolean };
    expect(component.composing()).toBe(false);
    expect(component.sending()).toBe(false);
  });

  it('surfaces an error message when sending fails', async () => {
    const failingSend = vi.fn(() => throwError(() => ({ error: { message: 'blocked' } })));
    await TestBed.configureTestingModule({
      imports: [
        MessagesComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        { provide: MessagesApiService, useValue: { inbox: vi.fn(() => of([])), sent: vi.fn(() => of([])), markRead: vi.fn(), send: failingSend } }
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(MessagesComponent);
    fixture.detectChanges();

    fixture.componentInstance['composeForm'].setValue({ recipientUsername: 'bob', subject: 'Hi', body: 'Hello' });
    fixture.componentInstance.submitCompose();

    const component = fixture.componentInstance as unknown as { errorMessage: () => string | null };
    expect(component.errorMessage()).toBe('blocked');
  });

  it('formatTimestamp formats an ISO string for display', async () => {
    const fixture = await setup();
    expect(fixture.componentInstance.formatTimestamp('2026-01-01T00:00:00Z')).toBe(new Date('2026-01-01T00:00:00Z').toLocaleString());
  });
});
