import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { formatCountdown } from '../../core/countdown';
import { isAttackMission, missionLabel } from '../../core/fleet-mission';
import { FleetMissionType, FleetMovementView, PlanetView, ShipyardView } from '../../core/models';
import { UniverseApiService } from '../universe/universe-api.service';
import { FleetApiService } from './fleet-api.service';

@Component({
  selector: 'app-fleet',
  imports: [RouterLink, FormsModule],
  templateUrl: './fleet.component.html',
  styleUrl: './fleet.component.css'
})
export class FleetComponent {
  private readonly api = inject(UniverseApiService);
  private readonly fleetApi = inject(FleetApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly planets = signal<PlanetView[]>([]);
  protected readonly originPlanetId = signal<number | null>(null);
  protected readonly ships = signal<ShipyardView[]>([]);
  protected readonly movements = signal<FleetMovementView[]>([]);
  protected readonly missionType = signal<FleetMissionType>('COLONIZE');
  protected readonly queuingShip = signal<string | null>(null);
  protected readonly dispatching = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly eta = signal<number | null>(null);
  protected readonly etaLoading = signal(false);
  protected readonly etaError = signal<string | null>(null);
  /** Bumped every second purely to force countdown text to re-render. */
  protected readonly clockTick = signal(0);

  protected readonly missionLabel = missionLabel;
  protected readonly isAttackMission = isAttackMission;

  constructor() {
    this.api.listPlanets().subscribe((planets) => {
      this.planets.set(planets);
      this.applyRequestedOrigin(this.route.snapshot.queryParamMap.get('origin'));
    });

    // Angular reuses this component instance across navigations to the same /fleet
    // route (e.g. clicking "Manage fleet" from a different planet only changes the
    // query param), so the requested origin must be re-applied reactively rather
    // than read once from a snapshot.
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      this.applyRequestedOrigin(params.get('origin'));
    });

    this.refreshMovements();

    const pollHandle = setInterval(() => this.refreshMovements(), 5000);
    const clockHandle = setInterval(() => this.clockTick.update((v) => v + 1), 1000);
    this.destroyRef.onDestroy(() => {
      clearInterval(pollHandle);
      clearInterval(clockHandle);
    });
  }

  private applyRequestedOrigin(originParam: string | null): void {
    const planets = this.planets();
    if (planets.length === 0) {
      return;
    }
    const requested = Number(originParam) || null;
    const isValidRequest = requested !== null && planets.some((p) => p.id === requested);
    const next = isValidRequest
      ? requested
      : (this.originPlanetId() ?? planets.find((p) => p.homeworld)?.id ?? planets[0]?.id ?? null);
    if (next !== this.originPlanetId()) {
      this.originPlanetId.set(next);
      this.loadShips();
    }
  }

  protected otherPlanets(): PlanetView[] {
    return this.planets().filter((p) => p.id !== this.originPlanetId());
  }

  onOriginChange(id: number): void {
    this.originPlanetId.set(id);
    this.resetEta();
    this.loadShips();
  }

  setMissionType(type: FleetMissionType): void {
    this.missionType.set(type);
    this.resetEta();
  }

  onTargetPlanetChange(
    event: Event,
    galaxyInput: HTMLInputElement,
    systemInput: HTMLInputElement,
    positionInput: HTMLInputElement,
    shipKey: string
  ): void {
    const targetId = Number((event.target as HTMLSelectElement).value);
    const target = this.planets().find((p) => p.id === targetId);
    if (!target) {
      return;
    }
    galaxyInput.value = String(target.galaxy);
    systemInput.value = String(target.system);
    positionInput.value = String(target.position);
    this.checkEta(shipKey, target.galaxy, target.system, target.position);
  }

  checkEta(shipKey: string, galaxy: number, system: number, position: number): void {
    const originId = this.originPlanetId();
    if (!originId || !shipKey || !galaxy || !system || !position) {
      this.resetEta();
      return;
    }
    this.etaLoading.set(true);
    this.etaError.set(null);
    this.fleetApi.travelTime(originId, shipKey, galaxy, system, position).subscribe({
      next: (result) => {
        this.etaLoading.set(false);
        this.eta.set(result.etaSeconds);
      },
      error: (err) => {
        this.etaLoading.set(false);
        this.eta.set(null);
        this.etaError.set(err.error?.message ?? 'Could not compute travel time.');
      }
    });
  }

  etaLabel(): string {
    const seconds = this.eta();
    if (seconds === null) {
      return '';
    }
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes}:${secs.toString().padStart(2, '0')}`;
  }

  buildShip(ship: ShipyardView, quantity: number): void {
    const originId = this.originPlanetId();
    if (!originId || this.queuingShip() || !quantity || quantity < 1) {
      return;
    }
    this.errorMessage.set(null);
    this.queuingShip.set(ship.key);
    this.api.buildShips(originId, ship.key, quantity).subscribe({
      next: () => {
        this.queuingShip.set(null);
        this.loadShips();
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

  dispatch(shipKey: string, quantity: number, galaxy: number, system: number, position: number): void {
    const originId = this.originPlanetId();
    if (this.dispatching() || !originId || !shipKey || !quantity || quantity < 1 || !galaxy || !system || !position) {
      return;
    }
    this.errorMessage.set(null);
    this.dispatching.set(true);
    this.fleetApi
      .dispatch({
        originPlanetId: originId,
        shipKey,
        quantity,
        missionType: this.missionType(),
        targetGalaxy: galaxy,
        targetSystem: system,
        targetPosition: position
      })
      .subscribe({
        next: () => {
          this.dispatching.set(false);
          this.resetEta();
          this.loadShips();
          this.refreshMovements();
        },
        error: (err) => {
          this.dispatching.set(false);
          this.errorMessage.set(err.error?.message ?? 'Dispatch failed.');
        }
      });
  }

  originLabel(planetId: number): string {
    const p = this.planets().find((pl) => pl.id === planetId);
    return p ? `${p.name} ${p.coordinates}` : `#${planetId}`;
  }

  remainingMovementLabel(movement: FleetMovementView): string {
    return this.countdown(movement.arrivesAt);
  }

  private resetEta(): void {
    this.eta.set(null);
    this.etaError.set(null);
  }

  private loadShips(): void {
    const originId = this.originPlanetId();
    if (!originId) {
      this.ships.set([]);
      return;
    }
    this.api.getShips(originId).subscribe((ships) => this.ships.set(ships));
  }

  private refreshMovements(): void {
    this.fleetApi.movements().subscribe((movements) => this.movements.set(movements));
  }

  private countdown(endsAt: string | null): string {
    this.clockTick();
    return formatCountdown(endsAt);
  }
}
