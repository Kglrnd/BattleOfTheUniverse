import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoDirective } from '@jsverse/transloco';

import { CurrentPlanetService } from '../../core/current-planet.service';
import { EspionageComponent } from './espionage.component';

@Component({
  selector: 'app-espionage-page',
  imports: [FormsModule, EspionageComponent, TranslocoDirective],
  templateUrl: './espionage-page.component.html',
  styleUrl: './espionage-page.component.css'
})
export class EspionagePageComponent {
  private readonly currentPlanet = inject(CurrentPlanetService);

  protected readonly planets = this.currentPlanet.planets;
  protected readonly selectedPlanetId = this.currentPlanet.selectedPlanetId;

  onPlanetChange(id: number): void {
    this.currentPlanet.select(id);
  }
}
