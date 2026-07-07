import { Component, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { BuildingView, PlanetView, ResourceView } from '../../core/models';
import { UniverseApiService } from './universe-api.service';

@Component({
  selector: 'app-planet-detail',
  imports: [],
  templateUrl: './planet-detail.component.html',
  styleUrl: './planet-detail.component.css'
})
export class PlanetDetailComponent {
  private readonly api = inject(UniverseApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  private readonly planetId = Number(this.route.snapshot.paramMap.get('id'));

  protected readonly planet = signal<PlanetView | null>(null);
  protected readonly resources = signal<ResourceView[]>([]);
  protected readonly buildings = signal<BuildingView[]>([]);
  protected readonly upgrading = signal<string | null>(null);
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
    this.clockTick();
    if (!building.constructionEndsAt) {
      return '';
    }
    const remainingMs = new Date(building.constructionEndsAt).getTime() - Date.now();
    if (remainingMs <= 0) {
      return 'Finishing…';
    }
    const totalSeconds = Math.ceil(remainingMs / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }
}
