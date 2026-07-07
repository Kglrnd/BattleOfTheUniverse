import { Component, DestroyRef, inject, signal } from '@angular/core';

import { PlanetView, TechnologyView } from '../../core/models';
import { UniverseApiService } from '../universe/universe-api.service';
import { ResearchApiService } from './research-api.service';

@Component({
  selector: 'app-research',
  imports: [],
  templateUrl: './research.component.html',
  styleUrl: './research.component.css'
})
export class ResearchComponent {
  private readonly api = inject(ResearchApiService);
  private readonly universeApi = inject(UniverseApiService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly technologies = signal<TechnologyView[]>([]);
  protected readonly planets = signal<PlanetView[]>([]);
  protected readonly starting = signal<string | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  /** Bumped every second purely to force the countdown text to re-render. */
  protected readonly clockTick = signal(0);

  constructor() {
    this.universeApi.listPlanets().subscribe((planets) => this.planets.set(planets));
    this.refresh();

    const pollHandle = setInterval(() => this.refresh(), 5000);
    const clockHandle = setInterval(() => this.clockTick.update((v) => v + 1), 1000);
    this.destroyRef.onDestroy(() => {
      clearInterval(pollHandle);
      clearInterval(clockHandle);
    });
  }

  private refresh(): void {
    this.api.list().subscribe((technologies) => this.technologies.set(technologies));
  }

  hasActiveResearch(): boolean {
    return this.technologies().some((t) => t.researchActive);
  }

  start(tech: TechnologyView, planetId: number): void {
    if (this.starting() || !planetId) {
      return;
    }
    this.errorMessage.set(null);
    this.starting.set(tech.key);
    this.api.start(tech.key, planetId).subscribe({
      next: () => {
        this.starting.set(null);
        this.refresh();
      },
      error: (err) => {
        this.starting.set(null);
        this.errorMessage.set(err.error?.message ?? 'Research failed.');
      }
    });
  }

  remainingLabel(tech: TechnologyView): string {
    this.clockTick();
    if (!tech.researchEndsAt) {
      return '';
    }
    const remainingMs = new Date(tech.researchEndsAt).getTime() - Date.now();
    if (remainingMs <= 0) {
      return 'Finishing…';
    }
    const totalSeconds = Math.ceil(remainingMs / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }
}
