import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';

import { AdminPlanetsApiService } from './admin-planets-api.service';

@Component({
  selector: 'app-admin-planets',
  imports: [TranslocoDirective],
  templateUrl: './admin-planets.component.html',
  styleUrl: './admin-planets.component.css'
})
export class AdminPlanetsComponent {
  private readonly api = inject(AdminPlanetsApiService);
  private readonly transloco = inject(TranslocoService);

  private readonly planetsResource = rxResource({ stream: () => this.api.list() });
  protected readonly planets = computed(() => this.planetsResource.value() ?? []);
  protected readonly loading = this.planetsResource.isLoading;
  protected readonly errorMessage = computed(() => {
    const error = this.planetsResource.error() as HttpErrorResponse | undefined;
    return error ? (error.error?.message ?? this.transloco.translate('admin.planets.loadError')) : null;
  });

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
