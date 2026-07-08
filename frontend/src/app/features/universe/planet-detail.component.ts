import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { formatCountdown } from '../../core/countdown';
import { BuildingView, PlanetView, ResourceView } from '../../core/models';
import { UniverseApiService } from './universe-api.service';

@Component({
  selector: 'app-planet-detail',
  imports: [RouterLink],
  templateUrl: './planet-detail.component.html',
  styleUrl: './planet-detail.component.css'
})
export class PlanetDetailComponent {
  private readonly api = inject(UniverseApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  /**
   * Angular reuses this component instance when navigating between /universe/:id
   * routes (e.g. picking a different planet from the sidebar dropdown), so the id
   * must be re-read reactively via paramMap - a one-off snapshot read would go stale.
   */
  private planetId = 0;

  protected readonly planet = signal<PlanetView | null>(null);
  protected readonly resources = signal<ResourceView[]>([]);
  protected readonly buildings = signal<BuildingView[]>([]);
  protected readonly upgrading = signal<string | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  /** Bumped every second purely to force the countdown text to re-render. */
  protected readonly clockTick = signal(0);

  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      this.planetId = Number(params.get('id'));
      this.upgrading.set(null);
      this.errorMessage.set(null);
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
    this.api.getResources(this.planetId).subscribe((resources) => this.resources.set(resources));
    this.api.getBuildings(this.planetId).subscribe((buildings) => this.buildings.set(buildings));
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

  private countdown(endsAt: string | null): string {
    this.clockTick();
    return formatCountdown(endsAt);
  }
}
