import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoDirective } from '@jsverse/transloco';

import { CurrentPlanetService } from '../../core/current-planet.service';
import { DefenseComponent } from './defense.component';

@Component({
  selector: 'app-defense-page',
  imports: [FormsModule, DefenseComponent, TranslocoDirective],
  templateUrl: './defense-page.component.html',
  styleUrl: './defense-page.component.css'
})
export class DefensePageComponent {
  private readonly currentPlanet = inject(CurrentPlanetService);

  protected readonly planets = this.currentPlanet.planets;
  protected readonly selectedPlanetId = this.currentPlanet.selectedPlanetId;

  onPlanetChange(id: number): void {
    this.currentPlanet.select(id);
  }
}
