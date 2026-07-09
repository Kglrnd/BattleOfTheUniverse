import { Component, DestroyRef, effect, inject, signal } from '@angular/core';

import { CurrentPlanetService } from '../../core/current-planet.service';
import { ResourceView } from '../../core/models';
import { UniverseApiService } from './universe-api.service';

@Component({
  selector: 'app-resource-bar',
  imports: [],
  templateUrl: './resource-bar.component.html',
  styleUrl: './resource-bar.component.css'
})
export class ResourceBarComponent {
  private readonly api = inject(UniverseApiService);
  protected readonly currentPlanet = inject(CurrentPlanetService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly resources = signal<ResourceView[]>([]);

  constructor() {
    effect(() => {
      const planetId = this.currentPlanet.selectedPlanetId();
      if (planetId === null) {
        this.resources.set([]);
        return;
      }
      this.refresh(planetId);
    });

    const pollHandle = setInterval(() => {
      const planetId = this.currentPlanet.selectedPlanetId();
      if (planetId !== null) {
        this.refresh(planetId);
      }
    }, 5000);
    this.destroyRef.onDestroy(() => clearInterval(pollHandle));
  }

  private refresh(planetId: number): void {
    this.api.getResources(planetId).subscribe((resources) => this.resources.set(resources));
  }
}
