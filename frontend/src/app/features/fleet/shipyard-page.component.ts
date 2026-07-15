import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoDirective } from '@jsverse/transloco';

import { CurrentPlanetService } from '../../core/current-planet.service';
import { ShipyardComponent } from './shipyard.component';

@Component({
  selector: 'app-shipyard-page',
  imports: [FormsModule, ShipyardComponent, TranslocoDirective],
  templateUrl: './shipyard-page.component.html',
  styleUrl: './shipyard-page.component.css'
})
export class ShipyardPageComponent {
  private readonly currentPlanet = inject(CurrentPlanetService);

  protected readonly planets = this.currentPlanet.planets;
  protected readonly selectedPlanetId = this.currentPlanet.selectedPlanetId;

  onPlanetChange(id: number): void {
    this.currentPlanet.select(id);
  }
}
