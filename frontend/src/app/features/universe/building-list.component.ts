import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, effect, inject, input, signal } from '@angular/core';

import { formatCountdown } from '../../core/countdown';
import { BuildingView } from '../../core/models';
import { UniverseApiService } from './universe-api.service';

@Component({
  selector: 'app-building-list',
  imports: [DecimalPipe],
  templateUrl: './building-list.component.html',
  styleUrl: './building-list.component.css'
})
export class BuildingListComponent {
  private readonly api = inject(UniverseApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly planetId = input.required<number>();

  protected readonly buildings = signal<BuildingView[]>([]);
  protected readonly upgrading = signal<string | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  /** Bumped every second purely to force the countdown text to re-render. */
  protected readonly clockTick = signal(0);

  constructor() {
    effect(() => {
      this.planetId();
      this.upgrading.set(null);
      this.errorMessage.set(null);
      this.refresh();
    });

    const pollHandle = setInterval(() => this.refresh(), 5000);
    const clockHandle = setInterval(() => this.clockTick.update((v) => v + 1), 1000);
    this.destroyRef.onDestroy(() => {
      clearInterval(pollHandle);
      clearInterval(clockHandle);
    });
  }

  private refresh(): void {
    this.api.getBuildings(this.planetId()).subscribe((buildings) => this.buildings.set(buildings));
  }

  upgrade(building: BuildingView): void {
    if (this.upgrading()) {
      return;
    }
    this.errorMessage.set(null);
    this.upgrading.set(building.key);
    this.api.upgrade(this.planetId(), building.key).subscribe({
      next: () => {
        this.upgrading.set(null);
        this.refresh();
      },
      error: (err) => {
        this.upgrading.set(null);
        this.errorMessage.set(err.error?.message ?? 'Upgrade failed.');
      }
    });
  }

  hasActiveConstruction(): boolean {
    return this.buildings().some((b) => b.constructionActive);
  }

  remainingLabel(building: BuildingView): string {
    this.clockTick();
    return formatCountdown(building.constructionEndsAt);
  }
}
