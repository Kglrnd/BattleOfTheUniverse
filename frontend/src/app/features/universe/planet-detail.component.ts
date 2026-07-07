import { Component, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { BuildingView, FleetMovementView, PlanetView, ResourceView, ShipyardView } from '../../core/models';
import { FleetApiService } from '../fleet/fleet-api.service';
import { UniverseApiService } from './universe-api.service';

@Component({
  selector: 'app-planet-detail',
  imports: [],
  templateUrl: './planet-detail.component.html',
  styleUrl: './planet-detail.component.css'
})
export class PlanetDetailComponent {
  private readonly api = inject(UniverseApiService);
  private readonly fleetApi = inject(FleetApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  private readonly planetId = Number(this.route.snapshot.paramMap.get('id'));

  protected readonly planet = signal<PlanetView | null>(null);
  protected readonly resources = signal<ResourceView[]>([]);
  protected readonly buildings = signal<BuildingView[]>([]);
  protected readonly ships = signal<ShipyardView[]>([]);
  protected readonly movements = signal<FleetMovementView[]>([]);
  protected readonly upgrading = signal<string | null>(null);
  protected readonly queuingShip = signal<string | null>(null);
  protected readonly dispatching = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  /** Bumped every second purely to force the countdown text to re-render. */
  protected readonly clockTick = signal(0);

  constructor() {
    this.api.getPlanet(this.planetId).subscribe((planet) => this.planet.set(planet));
    this.refresh();

    const pollHandle = setInterval(() => this.refresh(), 5000);
    const clockHandle = setInterval(() => this.clockTick.update((v) => v + 1), 1000);
    this.destroyRef.onDestroy(() => {
      clearInterval(pollHandle);
      clearInterval(clockHandle);
    });
  }

  private refresh(): void {
    this.api.getResources(this.planetId).subscribe((resources) => this.resources.set(resources));
    this.api.getBuildings(this.planetId).subscribe((buildings) => this.buildings.set(buildings));
    this.api.getShips(this.planetId).subscribe((ships) => this.ships.set(ships));
    this.fleetApi.movements().subscribe((movements) => this.movements.set(movements));
  }

  upgrade(building: BuildingView): void {
    if (this.upgrading()) {
      return;
    }
    this.errorMessage.set(null);
    this.upgrading.set(building.key);
    this.api.upgrade(this.planetId, building.key).subscribe({
      next: () => {
        this.upgrading.set(null);
        this.refresh();
      },
      error: (err) => {
        this.upgrading.set(null);
        this.errorMessage.set(err.error?.message ?? 'Upgrade failed.');
      }
    });
  }

  hasActiveConstruction(): boolean {
    return this.buildings().some((b) => b.constructionActive);
  }

  remainingLabel(building: BuildingView): string {
    return this.countdown(building.constructionEndsAt);
  }

  buildShip(ship: ShipyardView, quantity: number): void {
    if (this.queuingShip() || !quantity || quantity < 1) {
      return;
    }
    this.errorMessage.set(null);
    this.queuingShip.set(ship.key);
    this.api.buildShips(this.planetId, ship.key, quantity).subscribe({
      next: () => {
        this.queuingShip.set(null);
        this.refresh();
      },
      error: (err) => {
        this.queuingShip.set(null);
        this.errorMessage.set(err.error?.message ?? 'Shipyard order failed.');
      }
    });
  }

  hasActiveShipyardJob(): boolean {
    return this.ships().some((s) => s.buildActive);
  }

  remainingShipLabel(ship: ShipyardView): string {
    return this.countdown(ship.buildEndsAt);
  }

  shipsAvailable(shipKey: string): number {
    return this.ships().find((s) => s.key === shipKey)?.owned ?? 0;
  }

  dispatch(shipKey: string, quantity: number, targetGalaxy: number, targetSystem: number, targetPosition: number): void {
    if (this.dispatching() || !shipKey || !quantity || quantity < 1) {
      return;
    }
    this.errorMessage.set(null);
    this.dispatching.set(true);
    this.fleetApi
      .dispatch({
        originPlanetId: this.planetId,
        shipKey,
        quantity,
        missionType: 'COLONIZE',
        targetGalaxy,
        targetSystem,
        targetPosition
      })
      .subscribe({
        next: () => {
          this.dispatching.set(false);
          this.refresh();
        },
        error: (err) => {
          this.dispatching.set(false);
          this.errorMessage.set(err.error?.message ?? 'Dispatch failed.');
        }
      });
  }

  remainingMovementLabel(movement: FleetMovementView): string {
    return this.countdown(movement.arrivesAt);
  }

  private countdown(endsAt: string | null): string {
    this.clockTick();
    if (!endsAt) {
      return '';
    }
    const remainingMs = new Date(endsAt).getTime() - Date.now();
    if (remainingMs <= 0) {
      return 'Finishing…';
    }
    const totalSeconds = Math.ceil(remainingMs / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }
}
