import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';
import { map } from 'rxjs';

import { CurrentPlanetService } from '../../core/current-planet.service';
import { formatCountdown, progressPercent } from '../../core/countdown';
import { formatShipManifest, isAttackMission, missionLabel } from '../../core/fleet-mission';
import { GameIconComponent } from '../../core/game-icon/game-icon.component';
import { FleetMovementView, IncomingMovementView } from '../../core/models';
import { FleetApiService } from '../fleet/fleet-api.service';
import { BuildingListComponent } from './building-list.component';
import { UniverseApiService } from './universe-api.service';

@Component({
  selector: 'app-planet-detail',
  imports: [RouterLink, BuildingListComponent, TranslocoDirective, GameIconComponent],
  templateUrl: './planet-detail.component.html',
  styleUrl: './planet-detail.component.css'
})
export class PlanetDetailComponent {
  private readonly api = inject(UniverseApiService);
  private readonly fleetApi = inject(FleetApiService);
  private readonly currentPlanet = inject(CurrentPlanetService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly transloco = inject(TranslocoService);

  /**
   * Angular reuses this component instance when navigating between /universe/:id
   * routes (e.g. picking a different planet from the sidebar dropdown), so the id
   * must be re-read reactively via paramMap - a one-off snapshot read would go stale.
   */
  protected readonly planetId = toSignal(this.route.paramMap.pipe(map((params) => Number(params.get('id')))), {
    requireSync: true
  });

  protected readonly planetResource = rxResource({
    params: () => ({ planetId: this.planetId() }),
    stream: ({ params }) => this.api.getPlanet(params.planetId)
  });
  protected readonly planet = computed(() => this.planetResource.value() ?? null);

  private readonly outgoingMovementsResource = rxResource({ stream: () => this.fleetApi.movements() });
  private readonly incomingMovementsResource = rxResource({ stream: () => this.fleetApi.incomingMovements() });
  protected readonly outgoingMovements = computed(() =>
    (this.outgoingMovementsResource.value() ?? []).filter((m) => m.originPlanetId === this.planetId())
  );
  protected readonly incomingMovements = computed(() =>
    (this.incomingMovementsResource.value() ?? []).filter((m) => m.targetPlanetId === this.planetId())
  );

  /** Bumped every second purely to force the countdown text to re-render. */
  protected readonly clockTick = signal(0);

  protected readonly renaming = signal(false);
  protected readonly renamePending = signal(false);
  protected readonly renameError = signal<string | null>(null);

  protected readonly missionLabel = missionLabel;
  protected readonly isAttackMission = isAttackMission;
  protected readonly formatShipManifest = formatShipManifest;

  constructor() {
    effect(() => this.currentPlanet.select(this.planetId()));

    const pollHandle = setInterval(() => {
      this.outgoingMovementsResource.reload();
      this.incomingMovementsResource.reload();
    }, 5000);
    const clockHandle = setInterval(() => this.clockTick.update((v) => v + 1), 1000);
    this.destroyRef.onDestroy(() => {
      clearInterval(pollHandle);
      clearInterval(clockHandle);
    });
  }

  remainingMovementLabel(movement: FleetMovementView | IncomingMovementView): string {
    this.clockTick();
    return formatCountdown(movement.arrivesAt);
  }

  movementProgress(movement: FleetMovementView | IncomingMovementView): number {
    this.clockTick();
    return progressPercent(movement.departedAt, movement.arrivesAt);
  }

  startRename(): void {
    this.renameError.set(null);
    this.renaming.set(true);
  }

  cancelRename(): void {
    this.renaming.set(false);
    this.renameError.set(null);
  }

  submitRename(input: HTMLInputElement, planetId: number): void {
    const name = input.value.trim();
    if (!name || this.renamePending()) {
      return;
    }
    this.renamePending.set(true);
    this.renameError.set(null);
    this.api.renamePlanet(planetId, name).subscribe({
      next: () => {
        this.renamePending.set(false);
        this.renaming.set(false);
        this.planetResource.reload();
        this.currentPlanet.reload();
      },
      error: (err) => {
        this.renamePending.set(false);
        this.renameError.set(err.error?.message ?? this.transloco.translate('universe.planetDetail.renameFailed'));
      }
    });
  }
}
