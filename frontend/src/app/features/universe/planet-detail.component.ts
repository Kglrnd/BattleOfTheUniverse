import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { CurrentPlanetService } from '../../core/current-planet.service';
import { formatCountdown } from '../../core/countdown';
import { formatShipManifest, isAttackMission, missionLabel } from '../../core/fleet-mission';
import { FleetMovementView, IncomingMovementView, PlanetView } from '../../core/models';
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
  protected planetId = 0;

  protected readonly planet = signal<PlanetView | null>(null);
  protected readonly outgoingMovements = signal<FleetMovementView[]>([]);
  protected readonly incomingMovements = signal<IncomingMovementView[]>([]);
  /** Bumped every second purely to force the countdown text to re-render. */
  protected readonly clockTick = signal(0);

  protected readonly missionLabel = missionLabel;
  protected readonly isAttackMission = isAttackMission;
  protected readonly formatShipManifest = formatShipManifest;

  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      this.planetId = Number(params.get('id'));
      this.currentPlanet.select(this.planetId);
      this.api.getPlanet(this.planetId).subscribe((planet) => this.planet.set(planet));
      this.refresh();
    });

    const pollHandle = setInterval(() => this.refresh(), 5000);
    const clockHandle = setInterval(() => this.clockTick.update((v) => v + 1), 1000);
    this.destroyRef.onDestroy(() => {
      clearInterval(pollHandle);
      clearInterval(clockHandle);
    });
  }

  private refresh(): void {
    this.fleetApi.movements().subscribe((movements) => {
      this.outgoingMovements.set(movements.filter((m) => m.originPlanetId === this.planetId));
    });
    this.fleetApi.incomingMovements().subscribe((movements) => {
      this.incomingMovements.set(movements.filter((m) => m.targetPlanetId === this.planetId));
    });
  }

  remainingMovementLabel(movement: FleetMovementView | IncomingMovementView): string {
    this.clockTick();
    return formatCountdown(movement.arrivesAt);
  }
}
