import { Component, inject, signal } from '@angular/core';

import { AdminGameApiService } from './admin-game-api.service';

@Component({
  selector: 'app-admin-reset',
  imports: [],
  templateUrl: './admin-reset.component.html',
  styleUrl: './admin-reset.component.css'
})
export class AdminResetComponent {
  private readonly api = inject(AdminGameApiService);

  protected readonly sending = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly doneMessage = signal<string | null>(null);

  reset(): void {
    if (this.sending()) {
      return;
    }
    const confirmed = confirm(
      'This will wipe every player\'s buildings, resources, research, fleet, and messages, delete all colonies, ' +
        'and move every homeworld to a new random position. This cannot be undone. Continue?'
    );
    if (!confirmed) {
      return;
    }
    this.errorMessage.set(null);
    this.doneMessage.set(null);
    this.sending.set(true);
    this.api.reset().subscribe({
      next: () => {
        this.sending.set(false);
        this.doneMessage.set('The game has been reset. Every player now starts fresh.');
      },
      error: (err) => {
        this.sending.set(false);
        this.errorMessage.set(err.error?.message ?? 'Reset failed.');
      }
    });
  }
}
