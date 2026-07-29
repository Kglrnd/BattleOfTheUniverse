import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';

import { AdminSettingsApiService } from './admin-settings-api.service';

@Component({
  selector: 'app-admin-settings',
  imports: [TranslocoDirective],
  templateUrl: './admin-settings.component.html',
  styleUrl: './admin-settings.component.css'
})
export class AdminSettingsComponent {
  private readonly api = inject(AdminSettingsApiService);
  private readonly transloco = inject(TranslocoService);

  private readonly settingsResource = rxResource({ stream: () => this.api.get() });

  protected readonly loading = this.settingsResource.isLoading;
  protected readonly registrationEnabled = computed(
    () => (this.settingsResource.error() ? true : (this.settingsResource.value()?.registrationEnabled ?? true))
  );
  protected readonly saving = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    effect(() => {
      const error = this.settingsResource.error() as HttpErrorResponse | undefined;
      if (error) {
        this.errorMessage.set(error.error?.message ?? this.transloco.translate('admin.settings.loadError'));
      }
    });
  }

  toggleRegistration(): void {
    if (this.saving() || this.loading()) {
      return;
    }
    const next = !this.registrationEnabled();
    this.errorMessage.set(null);
    this.saving.set(true);
    this.api.updateRegistrationEnabled(next).subscribe({
      next: (settings) => {
        this.saving.set(false);
        this.settingsResource.update(() => settings);
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(err.error?.message ?? this.transloco.translate('admin.settings.updateError'));
      }
    });
  }
}
