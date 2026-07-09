import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { PlanetView } from '../../core/models';
import { BuildingListComponent } from './building-list.component';
import { UniverseApiService } from './universe-api.service';

@Component({
  selector: 'app-buildings-page',
  imports: [FormsModule, BuildingListComponent],
  templateUrl: './buildings-page.component.html',
  styleUrl: './buildings-page.component.css'
})
export class BuildingsPageComponent {
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
