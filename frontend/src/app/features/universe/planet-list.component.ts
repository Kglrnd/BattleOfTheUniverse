import { Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';

import { isAttackMission } from '../../core/fleet-mission';
import { FleetApiService } from '../fleet/fleet-api.service';
import { UniverseApiService } from './universe-api.service';

@Component({
  selector: 'app-planet-list',
  imports: [RouterLink, TranslocoDirective],
  templateUrl: './planet-list.component.html',
  styleUrl: './planet-list.component.css'
})
export class PlanetListComponent {
  private readonly api = inject(UniverseApiService);
  private readonly fleetApi = inject(FleetApiService);

  private readonly planetsResource = rxResource({ stream: () => this.api.listPlanets() });
  protected readonly planets = computed(() => this.planetsResource.value() ?? []);
  protected readonly loading = this.planetsResource.isLoading;

  private readonly outgoingMovementsResource = rxResource({ stream: () => this.fleetApi.movements() });
  private readonly incomingMovementsResource = rxResource({ stream: () => this.fleetApi.incomingMovements() });
  protected readonly outgoingMovements = computed(() => this.outgoingMovementsResource.value() ?? []);
  protected readonly incomingMovements = computed(() => this.incomingMovementsResource.value() ?? []);

  outgoingCount(planetId: number): number {
    return this.outgoingMovements().filter((m) => m.originPlanetId === planetId).length;
  }

  incomingAttackCount(planetId: number): number {
    return this.incomingMovements().filter((m) => m.targetPlanetId === planetId && isAttackMission(m.missionType)).length;
  }

  incomingFriendlyCount(planetId: number): number {
    return this.incomingMovements().filter((m) => m.targetPlanetId === planetId && !isAttackMission(m.missionType)).length;
  }

  hasIncomingAttack(planetId: number): boolean {
    return this.incomingAttackCount(planetId) > 0;
  }
}
