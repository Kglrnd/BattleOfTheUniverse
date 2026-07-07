import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';

import { PlanetView } from '../../core/models';
import { UniverseApiService } from './universe-api.service';

@Component({
  selector: 'app-planet-list',
  imports: [RouterLink, MatCardModule],
  templateUrl: './planet-list.component.html',
  styleUrl: './planet-list.component.css'
})
export class PlanetListComponent {
  private readonly api = inject(UniverseApiService);

  protected readonly planets = signal<PlanetView[]>([]);
  protected readonly loading = signal(true);

  constructor() {
    this.api.listPlanets().subscribe((planets) => {
      this.planets.set(planets);
      this.loading.set(false);
    });
  }
}
