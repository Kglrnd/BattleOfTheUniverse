import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, ParamMap, RouterLink } from '@angular/router';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';

import { catalogDescription, catalogName } from '../../core/catalog-i18n';
import { CurrentPlanetService } from '../../core/current-planet.service';
import { formatCountdown } from '../../core/countdown';
import {
  BOMB_SHIP_KEY,
  formatCargo,
  formatShipManifest,
  GALAXY_CLASS_SHIP_KEY,
  INVASION_SHIP_KEY,
  isAttackMission,
  missionLabel
} from '../../core/fleet-mission';
import { GameAssetPipe } from '../../core/game-asset.pipe';
import { ImgFallbackDirective } from '../../core/img-fallback.directive';
import {
  DriveOptionView,
  FleetMissionType,
  FleetMovementView,
  PlanetView,
  ResourceKey,
  ResourceQuantity,
  ShipQuantity,
  ShipyardView
} from '../../core/models';
import { UniverseApiService } from '../universe/universe-api.service';
import { FleetApiService } from './fleet-api.service';

const TRANSPORTABLE_RESOURCES: ResourceKey[] = ['METAL', 'CRYSTAL', 'DEUTERIUM', 'HYDROGEN'];

@Component({
  selector: 'app-fleet',
  imports: [RouterLink, FormsModule, TranslocoDirective, GameAssetPipe, ImgFallbackDirective],
  templateUrl: './fleet.component.html',
  styleUrl: './fleet.component.css'
})
export class FleetComponent {
  private readonly api = inject(UniverseApiService);
  private readonly fleetApi = inject(FleetApiService);
  private readonly currentPlanet = inject(CurrentPlanetService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly transloco = inject(TranslocoService);

  protected readonly planets = this.currentPlanet.planets;
  protected readonly originPlanetId = signal<number | null>(null);

  private readonly shipsResource = rxResource({
    params: () => {
      const originId = this.originPlanetId();
      return originId === null ? undefined : { originId };
    },
    stream: ({ params }) => this.api.getShips(params.originId)
  });
  protected readonly ships = computed(() => this.shipsResource.value() ?? []);

  private readonly resourcesResource = rxResource({
    params: () => {
      const originId = this.originPlanetId();
      return originId === null ? undefined : { originId };
    },
    stream: ({ params }) => this.api.getResources(params.originId)
  });
  protected readonly resources = computed(() => this.resourcesResource.value() ?? []);

  private readonly movementsResource = rxResource({ stream: () => this.fleetApi.movements() });
  protected readonly movements = computed(() => this.movementsResource.value() ?? []);

  protected readonly missionType = signal<FleetMissionType>('COLONIZE');
  /** One-shot prefill for the target coordinate inputs when arriving via a "?mission=..." link. */
  protected readonly prefillGalaxy = signal<number | null>(null);
  protected readonly prefillSystem = signal<number | null>(null);
  protected readonly prefillPosition = signal<number | null>(null);
  /** Ship key -> quantity picked for the fleet currently being assembled. */
  protected readonly manifestQuantities = signal<Record<string, number>>({});
  /** Resource key -> amount picked for a TRANSPORT mission's cargo. */
  protected readonly cargoQuantities = signal<Record<string, number>>({});
  protected readonly dispatching = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly driveOptions = signal<DriveOptionView[]>([]);
  protected readonly selectedDriveKey = signal<string | null>(null);
  protected readonly driveOptionsLoading = signal(false);
  protected readonly driveOptionsError = signal<string | null>(null);
  /** Bumped every second purely to force countdown text to re-render. */
  protected readonly clockTick = signal(0);

  protected readonly missionLabel = missionLabel;
  protected readonly isAttackMission = isAttackMission;
  protected readonly formatShipManifest = formatShipManifest;
  protected readonly formatCargo = formatCargo;
  protected readonly transportableResources = TRANSPORTABLE_RESOURCES;

  constructor() {
    effect(() => {
      if (this.currentPlanet.planets().length > 0) {
        this.applyRequestedOrigin(this.route.snapshot.queryParamMap.get('origin'));
      }
    });

    // Angular reuses this component instance across navigations to the same /fleet
    // route (e.g. clicking "Manage fleet" from a different planet only changes the
    // query param), so the requested origin must be re-applied reactively rather
    // than read once from a snapshot.
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      this.applyRequestedOrigin(params.get('origin'));
      this.applyRequestedTarget(params);
    });

    const pollHandle = setInterval(() => this.movementsResource.reload(), 5000);
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
    const next = isValidRequest ? requested : (this.originPlanetId() ?? this.currentPlanet.selectedPlanetId());
    if (next !== this.originPlanetId()) {
      this.originPlanetId.set(next);
      if (next !== null) {
        this.currentPlanet.select(next);
      }
      this.manifestQuantities.set({});
      this.cargoQuantities.set({});
      this.resetDriveOptions();
    }
  }

  private static readonly MISSION_TYPES: FleetMissionType[] = [
    'COLONIZE',
    'STATION',
    'ATTACK',
    'ESPIONAGE',
    'TRANSPORT',
    'BOMBARD',
    'INVADE'
  ];

  /** Prefills mission type + target coordinates when arriving via a system-view "Attack"/"Colonize" link. */
  private applyRequestedTarget(params: ParamMap): void {
    const mission = params.get('mission');
    const galaxy = Number(params.get('targetGalaxy')) || null;
    const system = Number(params.get('targetSystem')) || null;
    const position = Number(params.get('targetPosition')) || null;
    if (!mission || !galaxy || !system || !position || !FleetComponent.MISSION_TYPES.includes(mission as FleetMissionType)) {
      return;
    }
    this.missionType.set(mission as FleetMissionType);
    this.prefillGalaxy.set(galaxy);
    this.prefillSystem.set(system);
    this.prefillPosition.set(position);
    this.refreshDriveOptions(galaxy, system, position);
  }

  protected otherPlanets(): PlanetView[] {
    return this.planets().filter((p) => p.id !== this.originPlanetId());
  }

  onOriginChange(id: number): void {
    this.originPlanetId.set(id);
    this.currentPlanet.select(id);
    this.manifestQuantities.set({});
    this.cargoQuantities.set({});
    this.resetDriveOptions();
  }

  setMissionType(type: FleetMissionType): void {
    this.missionType.set(type);
    this.cargoQuantities.set({});
    this.resetDriveOptions();
  }

  onTargetPlanetChange(
    event: Event,
    galaxyInput: HTMLInputElement,
    systemInput: HTMLInputElement,
    positionInput: HTMLInputElement
  ): void {
    const targetId = Number((event.target as HTMLSelectElement).value);
    const target = this.planets().find((p) => p.id === targetId);
    if (!target) {
      return;
    }
    galaxyInput.value = String(target.galaxy);
    systemInput.value = String(target.system);
    positionInput.value = String(target.position);
    this.refreshDriveOptions(target.galaxy, target.system, target.position);
  }

  updateManifestQuantity(shipKey: string, event: Event, galaxy: number, system: number, position: number): void {
    const requested = (event.target as HTMLInputElement).valueAsNumber || 0;
    const quantity = Math.max(0, Math.min(requested, this.shipsAvailable(shipKey)));
    const next = { ...this.manifestQuantities() };
    if (quantity > 0) {
      next[shipKey] = quantity;
    } else {
      delete next[shipKey];
    }
    this.manifestQuantities.set(next);
    this.refreshDriveOptions(galaxy, system, position);
  }

  protected currentManifest(): ShipQuantity[] {
    return Object.entries(this.manifestQuantities())
      .filter(([, quantity]) => quantity > 0)
      .map(([shipKey, quantity]) => ({ shipKey, quantity }));
  }

  /**
   * Client-side mirror of the server's manifest-composition rules for Orbital Bombs/Invasion
   * Units (mutual exclusion, no flying alone, mandatory Galaxy Class escort) - purely a UX
   * hint so the dispatch button can disable early; the server remains authoritative.
   */
  protected specialShipCompositionError(): string | null {
    const manifest = this.currentManifest();
    const hasBomb = manifest.some((s) => s.shipKey === BOMB_SHIP_KEY);
    const hasInvasionUnit = manifest.some((s) => s.shipKey === INVASION_SHIP_KEY);
    if (!hasBomb && !hasInvasionUnit) {
      return null;
    }
    if (hasBomb && hasInvasionUnit) {
      return this.transloco.translate('fleet.specialShipMutualExclusion');
    }
    if (manifest.length === 1) {
      return this.transloco.translate('fleet.specialShipNeedsEscort');
    }
    const hasGalaxyClassEscort = manifest.some((s) => s.shipKey === GALAXY_CLASS_SHIP_KEY);
    if (!hasGalaxyClassEscort) {
      return this.transloco.translate('fleet.specialShipNeedsGalaxyClass');
    }
    return null;
  }

  protected currentCargo(): ResourceQuantity[] {
    return Object.entries(this.cargoQuantities())
      .filter(([, amount]) => amount > 0)
      .map(([resourceKey, amount]) => ({ resourceKey: resourceKey as ResourceKey, amount }));
  }

  protected resourceOnHand(resourceKey: ResourceKey): number {
    return this.resources().find((r) => r.resourceKey === resourceKey)?.amount ?? 0;
  }

  protected fleetCargoCapacity(): number {
    return this.currentManifest().reduce((total, entry) => {
      const ship = this.ships().find((s) => s.key === entry.shipKey);
      return total + (ship?.cargoCapacity ?? 0) * entry.quantity;
    }, 0);
  }

  protected cargoUsed(): number {
    return this.currentCargo().reduce((total, entry) => total + entry.amount, 0);
  }

  updateCargoQuantity(resourceKey: ResourceKey, event: Event): void {
    const requested = (event.target as HTMLInputElement).valueAsNumber || 0;
    const remainingCapacity = this.fleetCargoCapacity() - this.cargoUsed() + (this.cargoQuantities()[resourceKey] ?? 0);
    const cap = Math.min(this.resourceOnHand(resourceKey), Math.max(0, remainingCapacity));
    const amount = Math.max(0, Math.min(requested, cap));
    const next = { ...this.cargoQuantities() };
    if (amount > 0) {
      next[resourceKey] = amount;
    } else {
      delete next[resourceKey];
    }
    this.cargoQuantities.set(next);
  }

  refreshDriveOptions(galaxy: number, system: number, position: number): void {
    const originId = this.originPlanetId();
    const ships = this.currentManifest();
    if (!originId || ships.length === 0 || !galaxy || !system || !position) {
      this.resetDriveOptions();
      return;
    }
    this.driveOptionsLoading.set(true);
    this.driveOptionsError.set(null);
    this.fleetApi
      .driveOptions({ originPlanetId: originId, ships, targetGalaxy: galaxy, targetSystem: system, targetPosition: position })
      .subscribe({
        next: (options) => {
          this.driveOptionsLoading.set(false);
          this.driveOptions.set(options);
          if (options.length === 0) {
            this.selectedDriveKey.set(null);
            this.driveOptionsError.set(this.transloco.translate('fleet.noDriveCapable'));
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
          this.driveOptionsError.set(err.error?.message ?? this.transloco.translate('fleet.driveOptionsFailed'));
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

  protected readonly shipName = (ship: ShipyardView) => catalogName(this.transloco, 'ships', ship);
  protected readonly shipDescription = (ship: ShipyardView) => catalogDescription(this.transloco, 'ships', ship);

  shipsAvailable(shipKey: string): number {
    return this.ships().find((s) => s.key === shipKey)?.owned ?? 0;
  }

  dispatch(galaxy: number, system: number, position: number): void {
    const originId = this.originPlanetId();
    const driveKey = this.selectedDriveKey();
    const ships = this.currentManifest();
    if (this.dispatching() || !originId || ships.length === 0 || !galaxy || !system || !position || !driveKey) {
      return;
    }
    this.errorMessage.set(null);
    this.dispatching.set(true);
    this.fleetApi
      .dispatch({
        originPlanetId: originId,
        ships,
        missionType: this.missionType(),
        targetGalaxy: galaxy,
        targetSystem: system,
        targetPosition: position,
        driveKey,
        cargo: this.missionType() === 'TRANSPORT' ? this.currentCargo() : []
      })
      .subscribe({
        next: () => {
          this.dispatching.set(false);
          this.manifestQuantities.set({});
          this.cargoQuantities.set({});
          this.resetDriveOptions();
          this.shipsResource.reload();
          this.resourcesResource.reload();
          this.movementsResource.reload();
        },
        error: (err) => {
          this.dispatching.set(false);
          this.errorMessage.set(err.error?.message ?? this.transloco.translate('fleet.dispatchFailed'));
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

  private resetDriveOptions(): void {
    this.driveOptions.set([]);
    this.selectedDriveKey.set(null);
    this.driveOptionsError.set(null);
  }

  private countdown(endsAt: string | null): string {
    this.clockTick();
    return formatCountdown(endsAt);
  }
}
