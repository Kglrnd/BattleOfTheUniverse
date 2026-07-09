import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { MessageView } from '../../core/models';
import { MessagesApiService } from './messages-api.service';

type Tab = 'inbox' | 'sent';

@Component({
  selector: 'app-messages',
  imports: [ReactiveFormsModule],
  templateUrl: './messages.component.html',
  styleUrl: './messages.component.css'
})
export class MessagesComponent {
  private readonly api = inject(MessagesApiService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly activeTab = signal<Tab>('inbox');
  protected readonly inbox = signal<MessageView[]>([]);
  protected readonly sent = signal<MessageView[]>([]);
  protected readonly selected = signal<MessageView | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly sending = signal(false);
  protected readonly composing = signal(false);

  protected readonly composeForm = this.fb.nonNullable.group({
    recipientUsername: ['', Validators.required],
    subject: ['', Validators.required],
    body: ['', Validators.required]
  });

  constructor() {
    this.refresh();
    const pollHandle = setInterval(() => this.refresh(), 10000);
    this.destroyRef.onDestroy(() => clearInterval(pollHandle));
  }

  private refresh(): void {
    this.api.inbox().subscribe((messages) => this.inbox.set(messages));
    this.api.sent().subscribe((messages) => this.sent.set(messages));
  }

  setTab(tab: Tab): void {
    this.activeTab.set(tab);
    this.selected.set(null);
  }

  currentMessages(): MessageView[] {
    return this.activeTab() === 'inbox' ? this.inbox() : this.sent();
  }

  unreadCount(): number {
    return this.inbox().filter((m) => !m.readAt).length;
  }

  select(message: MessageView): void {
    this.selected.set(message);
    if (this.activeTab() === 'inbox' && !message.readAt) {
      this.api.markRead(message.id).subscribe(() => this.refresh());
    }
  }

  toggleCompose(): void {
    this.composing.update((v) => !v);
    this.errorMessage.set(null);
  }

  submitCompose(): void {
    if (this.composeForm.invalid || this.sending()) {
      return;
    }
    this.errorMessage.set(null);
    this.sending.set(true);
    this.api.send(this.composeForm.getRawValue()).subscribe({
      next: () => {
        this.sending.set(false);
        this.composeForm.reset();
        this.composing.set(false);
        this.refresh();
      },
      error: (err) => {
        this.sending.set(false);
        this.errorMessage.set(err.error?.message ?? 'Could not send message.');
      }
    });
  }

  formatTimestamp(value: string): string {
    return new Date(value).toLocaleString();
  }
}
