import { Injectable, computed, inject, linkedSignal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

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

  private readonly planetsResource = rxResource({
    params: () => (this.auth.isAuthenticated() ? {} : undefined),
    stream: () => this.api.listPlanets()
  });

  readonly planets = computed(() => this.planetsResource.value() ?? []);

  // linkedSignal because the default selection must be derived from `planets`
  // (fall back to the homeworld once the list (re)loads) while still being
  // independently settable via `select()` - a plain `computed` couldn't do both.
  readonly selectedPlanetId = linkedSignal<PlanetView[], number | null>({
    source: this.planets,
    computation: (planets, previous) => {
      if (previous && planets.some((p) => p.id === previous.value)) {
        return previous.value;
      }
      return planets.find((p) => p.homeworld)?.id ?? planets[0]?.id ?? null;
    }
  });

  readonly selectedPlanet = computed(() => this.planets().find((p) => p.id === this.selectedPlanetId()) ?? null);

  reload(): void {
    this.planetsResource.reload();
  }

  select(planetId: number): void {
    // Native <select> (ngModelChange) emits string values, so normalize here rather
    // than trust every call site - the strict-equality lookup in `selectedPlanet`
    // above would otherwise silently fail to match a numeric PlanetView.id.
    this.selectedPlanetId.set(Number(planetId));
  }
}
