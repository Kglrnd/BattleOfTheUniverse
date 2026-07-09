import { Injectable, computed, effect, inject, signal } from '@angular/core';

import { AuthService } from './auth.service';
import { PlanetView } from './models';
import { UniverseApiService } from '../features/universe/universe-api.service';

/**
 * The single source of truth for "which planet is the player currently looking at" -
 * every planet-scoped page reads/writes this instead of independently defaulting to
 * the homeworld, so the global resource banner always matches whatever planet the rest
 * of the UI is showing.
 */
@Injectable({ providedIn: 'root' })
export class CurrentPlanetService {
  private readonly auth = inject(AuthService);
  private readonly api = inject(UniverseApiService);

  private readonly planetsSignal = signal<PlanetView[]>([]);
  private readonly selectedPlanetIdSignal = signal<number | null>(null);

  readonly planets = this.planetsSignal.asReadonly();
  readonly selectedPlanetId = this.selectedPlanetIdSignal.asReadonly();
  readonly selectedPlanet = computed(
    () => this.planetsSignal().find((p) => p.id === this.selectedPlanetIdSignal()) ?? null
  );

  constructor() {
    effect(() => {
      if (this.auth.isAuthenticated()) {
        this.refresh();
      } else {
        this.planetsSignal.set([]);
        this.selectedPlanetIdSignal.set(null);
      }
    });
  }

  refresh(): void {
    this.api.listPlanets().subscribe((planets) => {
      this.planetsSignal.set(planets);
      const current = this.selectedPlanetIdSignal();
      if (current === null || !planets.some((p) => p.id === current)) {
        this.selectedPlanetIdSignal.set(planets.find((p) => p.homeworld)?.id ?? planets[0]?.id ?? null);
      }
    });
  }

  select(planetId: number): void {
    // Native <select> (ngModelChange) emits string values, so normalize here rather
    // than trust every call site - the strict-equality lookup in `selectedPlanet`
    // above would otherwise silently fail to match a numeric PlanetView.id.
    this.selectedPlanetIdSignal.set(Number(planetId));
  }
}
