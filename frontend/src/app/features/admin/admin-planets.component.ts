import { Component, computed, inject, signal } from '@angular/core';

import { AdminPlanetView } from '../../core/models';
import { AdminPlanetsApiService } from './admin-planets-api.service';

@Component({
  selector: 'app-admin-planets',
  imports: [],
  templateUrl: './admin-planets.component.html',
  styleUrl: './admin-planets.component.css'
})
export class AdminPlanetsComponent {
  private readonly api = inject(AdminPlanetsApiService);

  protected readonly planets = signal<AdminPlanetView[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly ownerFilter = signal('');
  protected readonly nameFilter = signal('');
  protected readonly galaxyFilter = signal<number | null>(null);
  protected readonly systemFilter = signal<number | null>(null);
  protected readonly homeworldOnly = signal(false);

  protected readonly filteredPlanets = computed(() => {
    const owner = this.ownerFilter().trim().toLowerCase();
    const name = this.nameFilter().trim().toLowerCase();
    const galaxy = this.galaxyFilter();
    const system = this.systemFilter();
    const homeworldOnly = this.homeworldOnly();

    return this.planets().filter((p) => {
      if (owner && !p.ownerUsername.toLowerCase().includes(owner)) {
        return false;
      }
      if (name && !p.name.toLowerCase().includes(name)) {
        return false;
      }
      if (galaxy !== null && p.galaxy !== galaxy) {
        return false;
      }
      if (system !== null && p.system !== system) {
        return false;
      }
      if (homeworldOnly && !p.homeworld) {
        return false;
      }
      return true;
    });
  });

  constructor() {
    this.api.list().subscribe({
      next: (planets) => {
        this.planets.set(planets);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message ?? 'Failed to load planets.');
      }
    });
  }

  clearFilters(): void {
    this.ownerFilter.set('');
    this.nameFilter.set('');
    this.galaxyFilter.set(null);
    this.systemFilter.set(null);
    this.homeworldOnly.set(false);
  }

  onGalaxyFilterChange(value: string): void {
    this.galaxyFilter.set(value === '' ? null : Number(value));
  }

  onSystemFilterChange(value: string): void {
    this.systemFilter.set(value === '' ? null : Number(value));
  }

  formatCreatedAt(createdAt: string): string {
    return new Date(createdAt).toLocaleString();
  }
}
