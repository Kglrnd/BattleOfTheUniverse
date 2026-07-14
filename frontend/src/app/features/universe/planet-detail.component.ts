import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';

import { CurrentPlanetService } from '../../core/current-planet.service';
import { formatCountdown } from '../../core/countdown';
import { formatShipManifest, isAttackMission, missionLabel } from '../../core/fleet-mission';
import { FleetMovementView, IncomingMovementView } from '../../core/models';
import { FleetApiService } from '../fleet/fleet-api.service';
import { BuildingListComponent } from './building-list.component';
import { UniverseApiService } from './universe-api.service';

@Component({
  selector: 'app-planet-detail',
  imports: [RouterLink, BuildingListComponent],
  templateUrl: './planet-detail.component.html',
  styleUrl: './planet-detail.component.css'
})
export class PlanetDetailComponent {
  private readonly api = inject(UniverseApiService);
  private readonly fleetApi = inject(FleetApiService);
  private readonly currentPlanet = inject(CurrentPlanetService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

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
}
