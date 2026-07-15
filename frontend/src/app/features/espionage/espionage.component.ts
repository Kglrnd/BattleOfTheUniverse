import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';

import { DriveOptionView } from '../../core/models';
import { UniverseApiService } from '../universe/universe-api.service';
import { FleetApiService } from '../fleet/fleet-api.service';

const PROBE_SHIP_KEY = 'espionage_probe';

@Component({
  selector: 'app-espionage',
  imports: [FormsModule, RouterLink, TranslocoDirective],
  templateUrl: './espionage.component.html',
  styleUrl: './espionage.component.css'
})
export class EspionageComponent {
  private readonly api = inject(UniverseApiService);
  private readonly fleetApi = inject(FleetApiService);
  private readonly transloco = inject(TranslocoService);

  readonly planetId = input.required<number>();

  private readonly shipsResource = rxResource({
    params: () => ({ planetId: this.planetId() }),
    stream: ({ params }) => this.api.getShips(params.planetId)
  });
  protected readonly probesOwned = computed(
    () => this.shipsResource.value()?.find((s) => s.key === PROBE_SHIP_KEY)?.owned ?? 0
  );
  protected readonly quantity = signal(1);
  protected readonly driveOptions = signal<DriveOptionView[]>([]);
  protected readonly selectedDriveKey = signal<string | null>(null);
  protected readonly driveOptionsLoading = signal(false);
  protected readonly driveOptionsError = signal<string | null>(null);
  protected readonly sending = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly sentConfirmation = signal(false);

  constructor() {
    effect(() => {
      this.planetId();
      this.resetDriveOptions();
      this.sentConfirmation.set(false);
      this.errorMessage.set(null);
    });
  }

  updateQuantity(event: Event, galaxy: number, system: number, position: number): void {
    const requested = (event.target as HTMLInputElement).valueAsNumber || 0;
    this.quantity.set(Math.max(0, Math.min(requested, this.probesOwned())));
    this.refreshDriveOptions(galaxy, system, position);
  }

  refreshDriveOptions(galaxy: number, system: number, position: number): void {
    if (this.quantity() <= 0 || !galaxy || !system || !position) {
      this.resetDriveOptions();
      return;
    }
    this.driveOptionsLoading.set(true);
    this.driveOptionsError.set(null);
    this.fleetApi
      .driveOptions({
        originPlanetId: this.planetId(),
        ships: [{ shipKey: PROBE_SHIP_KEY, quantity: this.quantity() }],
        targetGalaxy: galaxy,
        targetSystem: system,
        targetPosition: position
      })
      .subscribe({
        next: (options) => {
          this.driveOptionsLoading.set(false);
          this.driveOptions.set(options);
          if (options.length === 0) {
            this.selectedDriveKey.set(null);
            this.driveOptionsError.set(this.transloco.translate('espionage.espionage.noDriveCapable'));
            return;
          }
          const stillOffered = options.some((o) => o.key === this.selectedDriveKey());
          if (!stillOffered) {
            const fastest = options.reduce((best, o) => (o.etaSeconds < best.etaSeconds ? o : best));
            this.selectedDriveKey.set(fastest.key);
          }
        },
        error: (err) => {
          this.driveOptionsLoading.set(false);
          this.driveOptions.set([]);
          this.selectedDriveKey.set(null);
          this.driveOptionsError.set(err.error?.message ?? this.transloco.translate('espionage.espionage.driveOptionsFailed'));
        }
      });
  }

  selectDrive(key: string): void {
    this.selectedDriveKey.set(key);
  }

  formatEta(seconds: number): string {
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes}:${secs.toString().padStart(2, '0')}`;
  }

  send(targetGalaxy: number, targetSystem: number, targetPosition: number): void {
    const driveKey = this.selectedDriveKey();
    if (this.sending() || this.quantity() <= 0 || !driveKey) {
      return;
    }
    this.errorMessage.set(null);
    this.sentConfirmation.set(false);
    this.sending.set(true);
    this.fleetApi
      .dispatch({
        originPlanetId: this.planetId(),
        ships: [{ shipKey: PROBE_SHIP_KEY, quantity: this.quantity() }],
        missionType: 'ESPIONAGE',
        targetGalaxy,
        targetSystem,
        targetPosition,
        driveKey
      })
      .subscribe({
        next: () => {
          this.sending.set(false);
          this.sentConfirmation.set(true);
          this.resetDriveOptions();
          this.shipsResource.reload();
        },
        error: (err) => {
          this.sending.set(false);
          this.errorMessage.set(err.error?.message ?? this.transloco.translate('espionage.espionage.dispatchFailed'));
        }
      });
  }

  private resetDriveOptions(): void {
    this.driveOptions.set([]);
    this.selectedDriveKey.set(null);
    this.driveOptionsError.set(null);
  }
}
