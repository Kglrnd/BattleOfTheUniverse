import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CurrentPlanetService } from '../../core/current-planet.service';
import { DefenseComponent } from './defense.component';

@Component({
  selector: 'app-defense-page',
  imports: [FormsModule, DefenseComponent],
  templateUrl: './defense-page.component.html',
  styleUrl: './defense-page.component.css'
})
export class DefensePageComponent {
  private readonly currentPlanet = inject(CurrentPlanetService);

  protected readonly planets = this.currentPlanet.planets;
  protected readonly selectedPlanetId = this.currentPlanet.selectedPlanetId;

  constructor() {
    this.currentPlanet.refresh();
  }

  onPlanetChange(id: number): void {
    this.currentPlanet.select(id);
  }
}
