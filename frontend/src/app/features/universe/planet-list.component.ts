import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { isAttackMission } from '../../core/fleet-mission';
import { FleetMovementView, IncomingMovementView, PlanetView } from '../../core/models';
import { FleetApiService } from '../fleet/fleet-api.service';
import { UniverseApiService } from './universe-api.service';

@Component({
  selector: 'app-planet-list',
  imports: [RouterLink],
  templateUrl: './planet-list.component.html',
  styleUrl: './planet-list.component.css'
})
export class PlanetListComponent {
  private readonly api = inject(UniverseApiService);
  private readonly fleetApi = inject(FleetApiService);

  protected readonly planets = signal<PlanetView[]>([]);
  protected readonly loading = signal(true);
  protected readonly outgoingMovements = signal<FleetMovementView[]>([]);
  protected readonly incomingMovements = signal<IncomingMovementView[]>([]);

  constructor() {
    this.api.listPlanets().subscribe((planets) => {
      this.planets.set(planets);
      this.loading.set(false);
    });
    this.refreshFleetActivity();
  }

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

  private refreshFleetActivity(): void {
    this.fleetApi.movements().subscribe((movements) => this.outgoingMovements.set(movements));
    this.fleetApi.incomingMovements().subscribe((movements) => this.incomingMovements.set(movements));
  }
}
