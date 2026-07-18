import { DecimalPipe } from '@angular/common';
import { Component, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';

import { catalogDescription, catalogName } from '../../core/catalog-i18n';
import { GameIconComponent } from '../../core/game-icon/game-icon.component';
import { ResourceProductionView } from '../../core/models';
import { UniverseApiService } from './universe-api.service';

@Component({
  selector: 'app-resource-production-list',
  imports: [DecimalPipe, TranslocoDirective, GameIconComponent],
  templateUrl: './resource-production-list.component.html',
  styleUrl: './resource-production-list.component.css'
})
export class ResourceProductionListComponent {
  private readonly api = inject(UniverseApiService);
  private readonly transloco = inject(TranslocoService);

  readonly planetId = input.required<number>();

  protected readonly productionResource = rxResource({
    params: () => ({ planetId: this.planetId() }),
    stream: ({ params }) => this.api.getProduction(params.planetId)
  });

  protected readonly buildingName = (p: ResourceProductionView) =>
    catalogName(this.transloco, 'buildings', { key: p.buildingKey, name: p.buildingName, description: p.buildingDescription });
  protected readonly buildingDescription = (p: ResourceProductionView) =>
    catalogDescription(this.transloco, 'buildings', { key: p.buildingKey, name: p.buildingName, description: p.buildingDescription });
}
