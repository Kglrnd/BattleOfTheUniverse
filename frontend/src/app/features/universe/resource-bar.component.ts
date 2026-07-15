import { Component, DestroyRef, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslocoDirective } from '@jsverse/transloco';

import { CurrentPlanetService } from '../../core/current-planet.service';
import { UniverseApiService } from './universe-api.service';

@Component({
  selector: 'app-resource-bar',
  imports: [TranslocoDirective],
  templateUrl: './resource-bar.component.html',
  styleUrl: './resource-bar.component.css'
})
export class ResourceBarComponent {
  private readonly api = inject(UniverseApiService);
  protected readonly currentPlanet = inject(CurrentPlanetService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly resourcesResource = rxResource({
    params: () => {
      const planetId = this.currentPlanet.selectedPlanetId();
      return planetId === null ? undefined : { planetId };
    },
    stream: ({ params }) => this.api.getResources(params.planetId)
  });

  constructor() {
    const pollHandle = setInterval(() => this.resourcesResource.reload(), 5000);
    this.destroyRef.onDestroy(() => clearInterval(pollHandle));
  }
}
