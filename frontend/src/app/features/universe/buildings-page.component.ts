import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoDirective } from '@jsverse/transloco';

import { CurrentPlanetService } from '../../core/current-planet.service';
import { BuildingListComponent } from './building-list.component';

@Component({
  selector: 'app-buildings-page',
  imports: [FormsModule, BuildingListComponent, TranslocoDirective],
  templateUrl: './buildings-page.component.html',
  styleUrl: './buildings-page.component.css'
})
export class BuildingsPageComponent {
  private readonly currentPlanet = inject(CurrentPlanetService);

  protected readonly planets = this.currentPlanet.planets;
  protected readonly selectedPlanetId = this.currentPlanet.selectedPlanetId;

  onPlanetChange(id: number): void {
    this.currentPlanet.select(id);
  }
}
