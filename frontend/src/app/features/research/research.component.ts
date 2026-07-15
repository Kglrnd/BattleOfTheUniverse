import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';

import { catalogDescription, catalogName } from '../../core/catalog-i18n';
import { ResearchPlanetOption, TechnologyView } from '../../core/models';
import { ResearchApiService } from './research-api.service';

@Component({
  selector: 'app-research',
  imports: [DecimalPipe, TranslocoDirective],
  templateUrl: './research.component.html',
  styleUrl: './research.component.css'
})
export class ResearchComponent {
  private readonly api = inject(ResearchApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly transloco = inject(TranslocoService);

  protected readonly technologiesResource = rxResource({ stream: () => this.api.list() });
  protected readonly researchPlanetsResource = rxResource({ stream: () => this.api.listPlanetOptions() });
  protected readonly starting = signal<string | null>(null);
  protected readonly activating = signal<number | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  /** Bumped every second purely to force the countdown text to re-render. */
  protected readonly clockTick = signal(0);

  constructor() {
    const pollHandle = setInterval(() => {
      this.technologiesResource.reload();
      this.researchPlanetsResource.reload();
    }, 5000);
    const clockHandle = setInterval(() => this.clockTick.update((v) => v + 1), 1000);
    this.destroyRef.onDestroy(() => {
      clearInterval(pollHandle);
      clearInterval(clockHandle);
    });
  }

  hasActiveResearch(): boolean {
    return (this.technologiesResource.value() ?? []).some((t) => t.researchActive);
  }

  hasActiveResearchPlanet(): boolean {
    return (this.researchPlanetsResource.value() ?? []).some((p) => p.active);
  }

  protected readonly technologyName = (tech: TechnologyView) => catalogName(this.transloco, 'technologies', tech);
  protected readonly technologyDescription = (tech: TechnologyView) =>
    catalogDescription(this.transloco, 'technologies', tech);

  activate(planet: ResearchPlanetOption): void {
    if (this.activating() || planet.active || planet.researchLabLevel === 0) {
      return;
    }
    this.errorMessage.set(null);
    this.activating.set(planet.planetId);
    this.api.activate(planet.planetId).subscribe({
      next: () => {
        this.activating.set(null);
        this.technologiesResource.reload();
        this.researchPlanetsResource.reload();
      },
      error: (err) => {
        this.activating.set(null);
        this.errorMessage.set(err.error?.message ?? this.transloco.translate('research.activatePlanetError'));
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
        this.technologiesResource.reload();
        this.researchPlanetsResource.reload();
      },
      error: (err) => {
        this.starting.set(null);
        this.errorMessage.set(err.error?.message ?? this.transloco.translate('research.startError'));
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
