import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { PlanetView } from '../../core/models';
import { DefenseComponent } from './defense.component';
import { UniverseApiService } from '../universe/universe-api.service';

@Component({
  selector: 'app-defense-page',
  imports: [FormsModule, DefenseComponent],
  templateUrl: './defense-page.component.html',
  styleUrl: './defense-page.component.css'
})
export class DefensePageComponent {
  private readonly api = inject(UniverseApiService);

  protected readonly planets = signal<PlanetView[]>([]);
  protected readonly selectedPlanetId = signal<number | null>(null);

  constructor() {
    this.api.listPlanets().subscribe((planets) => {
      this.planets.set(planets);
      this.selectedPlanetId.set(planets.find((p) => p.homeworld)?.id ?? planets[0]?.id ?? null);
    });
  }

  onPlanetChange(id: number): void {
    this.selectedPlanetId.set(id);
  }
}
