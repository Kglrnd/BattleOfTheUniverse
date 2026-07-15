import { Component, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';

import { catalogDescription, catalogName } from '../../core/catalog-i18n';
import { formatCountdown } from '../../core/countdown';
import { TowerView } from '../../core/models';
import { DefenseApiService } from './defense-api.service';

@Component({
  selector: 'app-defense',
  imports: [TranslocoDirective],
  templateUrl: './defense.component.html',
  styleUrl: './defense.component.css'
})
export class DefenseComponent {
  private readonly api = inject(DefenseApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly transloco = inject(TranslocoService);

  readonly planetId = input.required<number>();

  protected readonly towersResource = rxResource({
    params: () => ({ planetId: this.planetId() }),
    stream: ({ params }) => this.api.list(params.planetId)
  });
  protected readonly queuingTower = signal<string | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  /** Bumped every second purely to force the countdown text to re-render. */
  protected readonly clockTick = signal(0);

  constructor() {
    effect(() => {
      this.planetId();
      this.queuingTower.set(null);
      this.errorMessage.set(null);
    });

    const pollHandle = setInterval(() => this.towersResource.reload(), 5000);
    const clockHandle = setInterval(() => this.clockTick.update((v) => v + 1), 1000);
    this.destroyRef.onDestroy(() => {
      clearInterval(pollHandle);
      clearInterval(clockHandle);
    });
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
        this.towersResource.reload();
      },
      error: (err) => {
        this.queuingTower.set(null);
        this.errorMessage.set(err.error?.message ?? this.transloco.translate('defense.defense.buildFailed'));
      }
    });
  }

  hasActiveDefenseJob(): boolean {
    return (this.towersResource.value() ?? []).some((t) => t.buildActive);
  }

  protected readonly towerName = (tower: TowerView) => catalogName(this.transloco, 'defenses', tower);
  protected readonly towerDescription = (tower: TowerView) => catalogDescription(this.transloco, 'defenses', tower);

  remainingLabel(tower: TowerView): string {
    this.clockTick();
    return formatCountdown(tower.buildEndsAt);
  }
}
