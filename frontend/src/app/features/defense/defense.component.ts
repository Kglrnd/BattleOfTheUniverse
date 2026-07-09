import { Component, DestroyRef, effect, inject, input, signal } from '@angular/core';

import { formatCountdown } from '../../core/countdown';
import { TowerView } from '../../core/models';
import { DefenseApiService } from './defense-api.service';

@Component({
  selector: 'app-defense',
  imports: [],
  templateUrl: './defense.component.html',
  styleUrl: './defense.component.css'
})
export class DefenseComponent {
  private readonly api = inject(DefenseApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly planetId = input.required<number>();

  protected readonly towers = signal<TowerView[]>([]);
  protected readonly queuingTower = signal<string | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  /** Bumped every second purely to force the countdown text to re-render. */
  protected readonly clockTick = signal(0);

  constructor() {
    effect(() => {
      this.planetId();
      this.queuingTower.set(null);
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
    this.api.list(this.planetId()).subscribe((towers) => this.towers.set(towers));
  }

  build(tower: TowerView, quantity: number): void {
    if (this.queuingTower() || !quantity || quantity < 1) {
      return;
    }
    this.errorMessage.set(null);
    this.queuingTower.set(tower.key);
    this.api.build(this.planetId(), tower.key, quantity).subscribe({
      next: () => {
        this.queuingTower.set(null);
        this.refresh();
      },
      error: (err) => {
        this.queuingTower.set(null);
        this.errorMessage.set(err.error?.message ?? 'Build failed.');
      }
    });
  }

  hasActiveDefenseJob(): boolean {
    return this.towers().some((t) => t.buildActive);
  }

  remainingLabel(tower: TowerView): string {
    this.clockTick();
    return formatCountdown(tower.buildEndsAt);
  }
}
