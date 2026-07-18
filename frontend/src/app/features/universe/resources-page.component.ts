import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoDirective } from '@jsverse/transloco';

import { CurrentPlanetService } from '../../core/current-planet.service';
import { ResourceProductionListComponent } from './resource-production-list.component';

@Component({
  selector: 'app-resources-page',
  imports: [FormsModule, ResourceProductionListComponent, TranslocoDirective],
  templateUrl: './resources-page.component.html',
  styleUrl: './resources-page.component.css'
})
export class ResourcesPageComponent {
  private readonly currentPlanet = inject(CurrentPlanetService);

  protected readonly planets = this.currentPlanet.planets;
  protected readonly selectedPlanetId = this.currentPlanet.selectedPlanetId;

  onPlanetChange(id: number): void {
    this.currentPlanet.select(id);
  }
}
