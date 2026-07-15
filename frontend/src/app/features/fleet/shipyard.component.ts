import { Component, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';

import { catalogDescription, catalogName } from '../../core/catalog-i18n';
import { formatCountdown } from '../../core/countdown';
import { GameAssetPipe } from '../../core/game-asset.pipe';
import { ImgFallbackDirective } from '../../core/img-fallback.directive';
import { ShipyardView } from '../../core/models';
import { UniverseApiService } from '../universe/universe-api.service';

@Component({
  selector: 'app-shipyard',
  imports: [TranslocoDirective, GameAssetPipe, ImgFallbackDirective],
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

    const pollHandle = setInterval(() => this.shipsResource.reload(), 5000);
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

  protected readonly shipName = (ship: ShipyardView) => catalogName(this.transloco, 'ships', ship);
  protected readonly shipDescription = (ship: ShipyardView) => catalogDescription(this.transloco, 'ships', ship);

  remainingLabel(ship: ShipyardView): string {
    this.clockTick();
    return formatCountdown(ship.buildEndsAt);
  }
}
