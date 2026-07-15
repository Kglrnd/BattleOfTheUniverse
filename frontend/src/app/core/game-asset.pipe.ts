import { Pipe, PipeTransform } from '@angular/core';

export type GameAssetCategory = 'planets' | 'buildings' | 'ships';

/**
 * Resolves a planet's imageVariant (a number) or a catalog item's key (a string)
 * to the static image served from public/images/<category>/<idOrKey>.webp.
 */
@Pipe({ name: 'gameAsset' })
export class GameAssetPipe implements PipeTransform {
  transform(idOrKey: number | string, category: GameAssetCategory): string {
    return `/images/${category}/${idOrKey}.webp`;
  }
}
