import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';

import { catalogDescription, catalogName } from '../../core/catalog-i18n';
import { formatCountdown } from '../../core/countdown';
import { GameAssetPipe } from '../../core/game-asset.pipe';
import { ImgFallbackDirective } from '../../core/img-fallback.directive';
import { BuildingView } from '../../core/models';
import { UniverseApiService } from './universe-api.service';

@Component({
  selector: 'app-building-list',
  imports: [DecimalPipe, TranslocoDirective, GameAssetPipe, ImgFallbackDirective],
  templateUrl: './building-list.component.html',
  styleUrl: './building-list.component.css'
})
export class BuildingListComponent {
  private readonly api = inject(UniverseApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly transloco = inject(TranslocoService);

  readonly planetId = input.required<number>();

  protected readonly buildingsResource = rxResource({
    params: () => ({ planetId: this.planetId() }),
    stream: ({ params }) => this.api.getBuildings(params.planetId)
  });
  protected readonly upgrading = signal<string | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  /** Bumped every second purely to force the countdown text to re-render. */
  protected readonly clockTick = signal(0);

  constructor() {
    effect(() => {
      this.planetId();
      this.upgrading.set(null);
      this.errorMessage.set(null);
    });

    const pollHandle = setInterval(() => this.buildingsResource.reload(), 5000);
    const clockHandle = setInterval(() => this.clockTick.update((v) => v + 1), 1000);
    this.destroyRef.onDestroy(() => {
      clearInterval(pollHandle);
      clearInterval(clockHandle);
    });
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
        this.buildingsResource.reload();
      },
      error: (err) => {
        this.upgrading.set(null);
        this.errorMessage.set(err.error?.message ?? this.transloco.translate('universe.buildingList.upgradeFailed'));
      }
    });
  }

  hasActiveConstruction(): boolean {
    return (this.buildingsResource.value() ?? []).some((b) => b.constructionActive);
  }

  protected readonly buildingName = (b: BuildingView) => catalogName(this.transloco, 'buildings', b);
  protected readonly buildingDescription = (b: BuildingView) => catalogDescription(this.transloco, 'buildings', b);

  remainingLabel(building: BuildingView): string {
    this.clockTick();
    return formatCountdown(building.constructionEndsAt);
  }
}
