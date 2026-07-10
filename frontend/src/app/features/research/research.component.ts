import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';

import { ResearchPlanetOption, TechnologyView } from '../../core/models';
import { ResearchApiService } from './research-api.service';

@Component({
  selector: 'app-research',
  imports: [DecimalPipe],
  templateUrl: './research.component.html',
  styleUrl: './research.component.css'
})
export class ResearchComponent {
  private readonly api = inject(ResearchApiService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly technologies = signal<TechnologyView[]>([]);
  protected readonly researchPlanets = signal<ResearchPlanetOption[]>([]);
  protected readonly starting = signal<string | null>(null);
  protected readonly activating = signal<number | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  /** Bumped every second purely to force the countdown text to re-render. */
  protected readonly clockTick = signal(0);

  constructor() {
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
    this.api.listPlanetOptions().subscribe((planets) => this.researchPlanets.set(planets));
  }

  hasActiveResearch(): boolean {
    return this.technologies().some((t) => t.researchActive);
  }

  hasActiveResearchPlanet(): boolean {
    return this.researchPlanets().some((p) => p.active);
  }

  activate(planet: ResearchPlanetOption): void {
    if (this.activating() || planet.active || planet.researchLabLevel === 0) {
      return;
    }
    this.errorMessage.set(null);
    this.activating.set(planet.planetId);
    this.api.activate(planet.planetId).subscribe({
      next: () => {
        this.activating.set(null);
        this.refresh();
      },
      error: (err) => {
        this.activating.set(null);
        this.errorMessage.set(err.error?.message ?? 'Could not activate this research planet.');
      }
    });
  }

  start(tech: TechnologyView): void {
    if (this.starting() || !this.hasActiveResearchPlanet()) {
      return;
    }
    this.errorMessage.set(null);
    this.starting.set(tech.key);
    this.api.start(tech.key).subscribe({
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
