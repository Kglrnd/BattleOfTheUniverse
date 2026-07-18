import { Component, DestroyRef, computed, effect, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';

import { catalogDescription, catalogName } from '../../core/catalog-i18n';
import { formatCountdown, progressPercentFromDuration } from '../../core/countdown';
import { shipCategory } from '../../core/fleet-mission';
import { GameIconComponent } from '../../core/game-icon/game-icon.component';
import { ShipyardQueueEntryView, ShipyardView } from '../../core/models';
import { UniverseApiService } from '../universe/universe-api.service';

interface ShipSection {
  readonly labelKey: string;
  readonly ships: ShipyardView[];
}

@Component({
  selector: 'app-shipyard',
  imports: [TranslocoDirective, GameIconComponent],
  templateUrl: './shipyard.component.html',
  styleUrl: './shipyard.component.css'
})
export class ShipyardComponent {
  private readonly api = inject(UniverseApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly transloco = inject(TranslocoService);

  readonly planetId = input.required<number>();

  protected readonly shipsResource = rxResource({
    params: () => ({ planetId: this.planetId() }),
    stream: ({ params }) => this.api.getShips(params.planetId)
  });
  protected readonly queueResource = rxResource({
    params: () => ({ planetId: this.planetId() }),
    stream: ({ params }) => this.api.getShipyardQueue(params.planetId)
  });
  protected readonly shipSections = computed<ShipSection[]>(() => {
    const ships = this.shipsResource.value() ?? [];
    return [
      { labelKey: 'combatShips', ships: ships.filter((s) => shipCategory(s.key) === 'COMBAT') },
      { labelKey: 'utilityShips', ships: ships.filter((s) => shipCategory(s.key) === 'UTILITY') },
      { labelKey: 'specialShips', ships: ships.filter((s) => shipCategory(s.key) === 'SPECIAL') }
    ];
  });
  protected readonly queuingShip = signal<string | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  /** Bumped every second purely to force the countdown text to re-render. */
  protected readonly clockTick = signal(0);

  constructor() {
    effect(() => {
      this.planetId();
      this.queuingShip.set(null);
      this.errorMessage.set(null);
    });

    const pollHandle = setInterval(() => {
      this.shipsResource.reload();
      this.queueResource.reload();
    }, 5000);
    const clockHandle = setInterval(() => this.clockTick.update((v) => v + 1), 1000);
    this.destroyRef.onDestroy(() => {
      clearInterval(pollHandle);
      clearInterval(clockHandle);
    });
  }

  build(ship: ShipyardView, quantity: number): void {
    if (this.queuingShip() || !quantity || quantity < 1) {
      return;
    }
    this.errorMessage.set(null);
    this.queuingShip.set(ship.key);
    this.api.buildShips(this.planetId(), ship.key, quantity).subscribe({
      next: () => {
        this.queuingShip.set(null);
        this.shipsResource.reload();
        this.queueResource.reload();
      },
      error: (err) => {
        this.queuingShip.set(null);
        this.errorMessage.set(err.error?.message ?? this.transloco.translate('shipyard.shipyard.shipyardOrderFailed'));
      }
    });
  }

  hasActiveShipyardJob(): boolean {
    return (this.shipsResource.value() ?? []).some((s) => s.buildActive);
  }

  /**
   * Below the pipeline's unlock level there's still at most one order at a time, so this falls
   * back to the pre-pipeline "anything building?" check - the queue endpoint always reports
   * maxSize 0 there, by design, so it can't be used to detect that single in-progress order.
   */
  isQueueFull(): boolean {
    const queue = this.queueResource.value();
    if (queue && queue.maxSize > 0) {
      return queue.entries.length >= queue.maxSize;
    }
    return this.hasActiveShipyardJob();
  }

  protected readonly shipName = (ship: ShipyardView) => catalogName(this.transloco, 'ships', ship);
  protected readonly shipDescription = (ship: ShipyardView) => catalogDescription(this.transloco, 'ships', ship);

  remainingLabel(ship: ShipyardView): string {
    this.clockTick();
    return formatCountdown(ship.buildEndsAt);
  }

  progress(ship: ShipyardView): number {
    this.clockTick();
    return progressPercentFromDuration(ship.buildEndsAt, ship.unitBuildTimeSeconds * (ship.buildingQuantity ?? 1));
  }

  remainingLabelForQueueEntry(entry: ShipyardQueueEntryView): string {
    this.clockTick();
    return formatCountdown(entry.endsAt);
  }
}
