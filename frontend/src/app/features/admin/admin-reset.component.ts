import { Component, inject, signal } from '@angular/core';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';

import { AdminGameApiService } from './admin-game-api.service';

@Component({
  selector: 'app-admin-reset',
  imports: [TranslocoDirective],
  templateUrl: './admin-reset.component.html',
  styleUrl: './admin-reset.component.css'
})
export class AdminResetComponent {
  private readonly api = inject(AdminGameApiService);
  private readonly transloco = inject(TranslocoService);

  protected readonly sending = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly doneMessage = signal<string | null>(null);

  reset(): void {
    if (this.sending()) {
      return;
    }
    const confirmed = confirm(this.transloco.translate('admin.reset.confirmMessage'));
    if (!confirmed) {
      return;
    }
    this.errorMessage.set(null);
    this.doneMessage.set(null);
    this.sending.set(true);
    this.api.reset().subscribe({
      next: () => {
        this.sending.set(false);
        this.doneMessage.set(this.transloco.translate('admin.reset.doneMessage'));
      },
      error: (err) => {
        this.sending.set(false);
        this.errorMessage.set(err.error?.message ?? this.transloco.translate('admin.reset.resetError'));
      }
    });
  }
}
